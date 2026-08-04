package nu.itark.frosk.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.CryptoPaperAccountDTO;
import nu.itark.frosk.analysis.CryptoPaperPositionDTO;
import nu.itark.frosk.model.CryptoPaperAccount;
import nu.itark.frosk.model.CryptoPaperOrder;
import nu.itark.frosk.repo.CryptoPaperAccountRepository;
import nu.itark.frosk.repo.CryptoPaperOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Simulates fills for every BUY/SELL signal emitted by {@link CryptoIntradayStrategyRunner},
 * independent of whether live trading is enabled — a continuously running paper track record.
 *
 * <p>Sizes positions the same way {@link nu.itark.frosk.crypto.livetrading.LiveTradingGate}
 * sizes real orders (equity * pct.of.equity, clamped to [min, max], capped by a total-exposure
 * fraction of equity), but against this account's own simulated equity so the simulation stays
 * a faithful preview of how live trading would behave.
 */
@Service
@Profile("crypto")
@Slf4j
public class CryptoPaperTradingService {

    @Value("${crypto.paper.trading.init.capital.eur:2000}")
    private BigDecimal initCapitalEur;

    @Value("${crypto.live.trading.position.pct.of.equity:0.05}")
    private BigDecimal positionPctOfEquity;

    @Value("${crypto.live.trading.position.min.eur:25}")
    private BigDecimal minPositionEur;

    @Value("${crypto.live.trading.max.position.eur:500}")
    private BigDecimal maxPositionEur;

    @Value("${crypto.live.trading.max.total.exposure.pct:0.5}")
    private BigDecimal maxTotalExposurePct;

    /**
     * Coinbase taker fee per leg — taker because {@code CoinbaseOrderClient} only
     * sends {@code market_market_ioc}, so every simulated fill mirrors a taker fill.
     * Charged on both entry and exit, exactly as a real order would be; without it
     * this account reported gross PnL as if it were net, and at ~60x turnover on the
     * account balance that gap is larger than the strategies' entire edge.
     */
    @Value("${exchange.transaction.cryptoTakerFeePerTradePercent:0.006}")
    private BigDecimal takerFeeFraction;

    @Autowired
    private CryptoPaperAccountRepository accountRepository;

    @Autowired
    private CryptoPaperOrderRepository orderRepository;

    @PostConstruct
    private void initAccount() {
        if (accountRepository.count() == 0) {
            CryptoPaperAccount account = new CryptoPaperAccount();
            account.setInitCapitalEur(initCapitalEur);
            account.setCashEur(initCapitalEur);
            accountRepository.save(account);
            log.info("CryptoPaperTradingService: initialized paper account with {} EUR", initCapitalEur);
        }
    }

    private CryptoPaperAccount getAccount() {
        return accountRepository.findAll().get(0);
    }

    public BigDecimal computeEquity() {
        return getAccount().getCashEur().add(orderRepository.sumOpenExposureEur());
    }

    public BigDecimal computePositionSizeEur() {
        return sizePosition(computeEquity());
    }

    private BigDecimal sizePosition(BigDecimal equity) {
        return equity.multiply(positionPctOfEquity).max(minPositionEur).min(maxPositionEur);
    }

    public void dispatchBuy(String strategyName, String ticker, BigDecimal closePrice) {
        CryptoPaperAccount account = getAccount();
        BigDecimal openExposure = orderRepository.sumOpenExposureEur();
        BigDecimal equity = account.getCashEur().add(openExposure);
        BigDecimal eurAmount = sizePosition(equity);

        BigDecimal maxTotalExposure = equity.multiply(maxTotalExposurePct);
        if (openExposure.add(eurAmount).compareTo(maxTotalExposure) > 0) {
            log.debug("CryptoPaperTradingService: skip BUY {} — open exposure {} + {} would exceed {} ({}% of equity {})",
                    ticker, openExposure, eurAmount, maxTotalExposure, maxTotalExposurePct, equity);
            return;
        }
        // The taker fee is paid on top of the notional, so cash must cover both legs' worth of it
        BigDecimal entryFee = eurAmount.multiply(takerFeeFraction).setScale(8, RoundingMode.HALF_UP);
        BigDecimal cashNeeded = eurAmount.add(entryFee);
        if (account.getCashEur().compareTo(cashNeeded) < 0) {
            log.debug("CryptoPaperTradingService: skip BUY {} — cash {} < {} (incl. {} fee)",
                    ticker, account.getCashEur(), cashNeeded, entryFee);
            return;
        }

        BigDecimal quantity = eurAmount.divide(closePrice, 8, RoundingMode.HALF_UP);

        CryptoPaperOrder order = new CryptoPaperOrder();
        order.setTicker(ticker);
        order.setSide("BUY");
        order.setStrategyName(strategyName);
        order.setEurAmount(eurAmount);
        order.setFilledPrice(closePrice);
        order.setFilledQuantity(quantity);
        orderRepository.save(order);

        account.setCashEur(account.getCashEur().subtract(cashNeeded));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("PAPER ORDER: BUY {} {} @ {}EUR (qty={}, fee={}, strategy={})",
                ticker, eurAmount, closePrice, quantity, entryFee, strategyName);
    }

    public void dispatchSell(String strategyName, String ticker, BigDecimal closePrice) {
        Optional<CryptoPaperOrder> openBuy = orderRepository
                .findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(ticker, strategyName, "BUY", "FILLED");
        if (openBuy.isEmpty()) {
            log.debug("CryptoPaperTradingService: SELL signal for {}/{} but no open paper BUY — skip", ticker, strategyName);
            return;
        }
        CryptoPaperOrder buyOrder = openBuy.get();
        BigDecimal proceeds = buyOrder.getFilledQuantity().multiply(closePrice);
        // Both legs pay the taker fee: the entry fee was charged at BUY (recomputed
        // here from the same constant), the exit fee comes off the proceeds.
        BigDecimal entryFee = buyOrder.getEurAmount().multiply(takerFeeFraction).setScale(8, RoundingMode.HALF_UP);
        BigDecimal exitFee = proceeds.multiply(takerFeeFraction).setScale(8, RoundingMode.HALF_UP);
        BigDecimal netProceeds = proceeds.subtract(exitFee);
        BigDecimal pnl = netProceeds.subtract(buyOrder.getEurAmount().add(entryFee));

        CryptoPaperOrder sellOrder = new CryptoPaperOrder();
        sellOrder.setTicker(ticker);
        sellOrder.setSide("SELL");
        sellOrder.setStrategyName(strategyName);
        sellOrder.setEurAmount(proceeds);
        sellOrder.setFilledPrice(closePrice);
        sellOrder.setFilledQuantity(buyOrder.getFilledQuantity());
        sellOrder.setRealizedPnlEur(pnl);
        orderRepository.save(sellOrder);

        buyOrder.setStatus("CLOSED");
        orderRepository.save(buyOrder);

        CryptoPaperAccount account = getAccount();
        account.setCashEur(account.getCashEur().add(netProceeds));
        account.setRealizedPnlEur(account.getRealizedPnlEur().add(pnl));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("PAPER ORDER: SELL {} {} @ {}EUR (pnl={}EUR net of {} fees, strategy={})",
                ticker, buyOrder.getFilledQuantity(), closePrice, pnl, entryFee.add(exitFee), strategyName);
    }

    public CryptoPaperAccountDTO getAccountSummary() {
        CryptoPaperAccount account = getAccount();
        List<CryptoPaperOrder> openOrders = orderRepository.findBySideAndStatusOrderByCreatedAtDesc("BUY", "FILLED");
        BigDecimal openExposure = openOrders.stream()
                .map(CryptoPaperOrder::getEurAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal equity = account.getCashEur().add(openExposure);
        BigDecimal pnlPercent = account.getInitCapitalEur().signum() == 0
                ? BigDecimal.ZERO
                : equity.subtract(account.getInitCapitalEur())
                        .divide(account.getInitCapitalEur(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return CryptoPaperAccountDTO.builder()
                .initCapitalEur(account.getInitCapitalEur())
                .cashEur(account.getCashEur())
                .equityEur(equity)
                .realizedPnlEur(account.getRealizedPnlEur())
                .realizedPnlPercent(pnlPercent)
                .openPositionsCount(openOrders.size())
                .updatedAt(account.getUpdatedAt() != null ? account.getUpdatedAt().toString() : null)
                .openPositions(openOrders.stream()
                        .map(o -> CryptoPaperPositionDTO.builder()
                                .ticker(o.getTicker())
                                .strategyName(o.getStrategyName())
                                .eurAmount(o.getEurAmount())
                                .filledPrice(o.getFilledPrice())
                                .filledQuantity(o.getFilledQuantity())
                                .createdAt(o.getCreatedAt().toString())
                                .build())
                        .toList())
                .build();
    }
}
