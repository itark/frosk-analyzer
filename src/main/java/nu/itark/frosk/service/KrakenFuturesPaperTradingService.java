package nu.itark.frosk.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.CryptoPaperAccountDTO;
import nu.itark.frosk.analysis.CryptoPaperPositionDTO;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.IntradayBar;
import nu.itark.frosk.model.KrakenFuturesPaperAccount;
import nu.itark.frosk.model.KrakenFuturesPaperOrder;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.IntradayBarRepository;
import nu.itark.frosk.repo.KrakenFuturesPaperAccountRepository;
import nu.itark.frosk.repo.KrakenFuturesPaperOrderRepository;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.strategies.SignalStrength;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Simulates fills for SHRT/COVR and BUY/SELL signals from
 * {@link CryptoIntradayStrategyRunner} against a Kraken Futures paper account.
 *
 * <h3>Position sizing</h3>
 * Positions are sized as {@code collateral * pctOfCollateral * signalMultiplier},
 * clamped to {@code [minPositionUsd, maxPositionUsd]}, and further capped so
 * total open exposure does not exceed {@code maxTotalExposurePct} of collateral.
 *
 * <h3>Fees</h3>
 * Kraken Futures charges taker fees on both entry and exit legs (market orders).
 * The default 0.05% taker rate is applied to both sides; override with
 * {@code kraken.futures.paper.taker.fee.pct}.
 *
 * <h3>Funding simulation</h3>
 * Perpetual futures (PF_*) charge/pay funding every 8 hours. A {@link #applyFunding()}
 * method runs at 00:00, 08:00, and 16:00 UTC and debits
 * {@code usdAmount * fundingRatePer8h} from the account's collateral and records it
 * on the open position. Configure via {@code kraken.futures.paper.funding.rate.per.8h}.
 *
 * <p>Longs pay funding when the perpetual trades at a premium (positive rate).
 * Shorts receive it. The configured rate is signed from the long's perspective:
 * positive = longs pay, negative = longs receive.
 *
 * <h3>Risk management</h3>
 * Every entry ({@link #openPosition}) is additionally gated by {@link
 * RiskManagementService#checkEntry} — daily-loss circuit breaker, per-position
 * size cap, and max open positions — layered on top of (not replacing) the
 * {@code maxTotalExposurePct} check below. See that class's javadoc.
 */
@Service
@Profile("kraken-futures")
@Slf4j
public class KrakenFuturesPaperTradingService {

    @Value("${kraken.futures.paper.init.collateral.usd:5000}")
    private BigDecimal initCollateralUsd;

    @Value("${kraken.futures.paper.position.pct.of.collateral:0.05}")
    private BigDecimal positionPctOfCollateral;

    @Value("${kraken.futures.paper.position.min.usd:25}")
    private BigDecimal minPositionUsd;

    @Value("${kraken.futures.paper.position.max.usd:1000}")
    private BigDecimal maxPositionUsd;

    @Value("${kraken.futures.paper.max.total.exposure.pct:0.5}")
    private BigDecimal maxTotalExposurePct;

    /**
     * Taker fee fraction per leg (entry + exit). Kraken Futures default taker
     * rate for retail is ~0.05%; override for your fee tier.
     */
    @Value("${kraken.futures.paper.taker.fee.pct:0.0005}")
    private BigDecimal takerFeeFraction;

    /**
     * Funding rate per 8-hour period, signed from the long position's perspective.
     * Positive → longs pay shorts. Negative → shorts pay longs.
     *
     * <p>Historical average for BTC perpetual is roughly +0.01% per 8h in bull
     * markets; configure to match actual market conditions or leave at default.
     */
    @Value("${kraken.futures.paper.funding.rate.per.8h:0.0001}")
    private BigDecimal fundingRatePer8h;

    @Value("${crypto.signal.strength.elevated.multiplier:1.5}")
    private BigDecimal elevatedMultiplier;

    @Value("${crypto.signal.strength.strong.multiplier:2.0}")
    private BigDecimal strongMultiplier;

    @Autowired
    private KrakenFuturesPaperAccountRepository accountRepository;

    @Autowired
    private KrakenFuturesPaperOrderRepository orderRepository;

    @Autowired
    private FeaturedStrategyRepository featuredStrategyRepository;

    @Autowired
    private RiskManagementService riskManagementService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private SecurityPriceRepository securityPriceRepository;

    /** Fallback price source — see {@link #latestPrice}. */
    @Autowired
    private IntradayBarRepository intradayBarRepository;

    // ── lifecycle ────────────────────────────────────────────────────────

    @PostConstruct
    private void initAccount() {
        if (accountRepository.count() == 0) {
            KrakenFuturesPaperAccount account = new KrakenFuturesPaperAccount();
            account.setInitCollateralUsd(initCollateralUsd);
            account.setCollateralUsd(initCollateralUsd);
            accountRepository.save(account);
            log.info("KrakenFuturesPaperTradingService: initialized paper account with {}USD collateral",
                    initCollateralUsd);
        }
    }

    // ── public dispatch API ──────────────────────────────────────────────

    /** Open a long position (BUY signal). */
    public void dispatchLong(String strategyName, String symbol, BigDecimal markPrice) {
        dispatchLong(strategyName, symbol, markPrice, null);
    }

    /** Open a long position (BUY signal) with signal strength scaling. */
    public void dispatchLong(String strategyName, String symbol, BigDecimal markPrice,
                             SignalStrength strength) {
        openPosition("LONG", strategyName, symbol, markPrice, strength);
    }

    /** Close a long position (SELL signal). */
    public void dispatchCloseLong(String strategyName, String symbol, BigDecimal markPrice) {
        closePosition("LONG", strategyName, symbol, markPrice);
    }

    /** Open a short position (SHRT signal). */
    public void dispatchShort(String strategyName, String symbol, BigDecimal markPrice) {
        dispatchShort(strategyName, symbol, markPrice, null);
    }

    /** Open a short position (SHRT signal) with signal strength scaling. */
    public void dispatchShort(String strategyName, String symbol, BigDecimal markPrice,
                              SignalStrength strength) {
        openPosition("SHORT", strategyName, symbol, markPrice, strength);
    }

    /** Close a short position (COVR signal). */
    public void dispatchCloseShort(String strategyName, String symbol, BigDecimal markPrice) {
        closePosition("SHORT", strategyName, symbol, markPrice);
    }

    // ── funding scheduler ────────────────────────────────────────────────

    /**
     * Applies funding to all open positions every 8 hours (00:00, 08:00, 16:00 UTC).
     *
     * <p>Longs pay {@code usdAmount * fundingRatePer8h}; shorts receive it
     * (negative deduction). The net effect is debited from collateral and
     * recorded on each open position's {@code totalFundingPaidUsd}.
     */
    @Scheduled(cron = "0 0 0,8,16 * * *")
    public void applyFunding() {
        List<KrakenFuturesPaperOrder> openPositions = orderRepository.findByStatusOrderByCreatedAtDesc("OPEN");
        if (openPositions.isEmpty()) return;

        KrakenFuturesPaperAccount account = getAccount();
        BigDecimal totalFundingDebit = BigDecimal.ZERO;

        for (KrakenFuturesPaperOrder pos : openPositions) {
            // Positive fundingRatePer8h → longs pay, shorts receive
            BigDecimal fundingSign = "LONG".equals(pos.getDirection()) ? BigDecimal.ONE : BigDecimal.ONE.negate();
            BigDecimal fundingAmount = pos.getUsdAmount()
                    .multiply(fundingRatePer8h)
                    .multiply(fundingSign)
                    .setScale(8, RoundingMode.HALF_UP);

            pos.setTotalFundingPaidUsd(pos.getTotalFundingPaidUsd().add(fundingAmount));
            orderRepository.save(pos);

            totalFundingDebit = totalFundingDebit.add(fundingAmount);
        }

        account.setCollateralUsd(account.getCollateralUsd().subtract(totalFundingDebit));
        account.setTotalFundingPaidUsd(account.getTotalFundingPaidUsd().add(totalFundingDebit));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("KrakenFuturesPaperTradingService: funding applied to {} positions — net debit {}USD (rate {})",
                openPositions.size(), totalFundingDebit, fundingRatePer8h);
    }

    // ── dashboard summary ────────────────────────────────────────────────

    /**
     * Snapshot of the paper account for the frosk-dashboard "Paper Account" card.
     *
     * <p>Reuses {@link CryptoPaperAccountDTO} (the spot-crypto shape) so the shared
     * frontend card renders unchanged. Kraken Futures is USD-denominated, so the
     * DTO's {@code *Eur} fields carry USD values here. Equity mirrors the spot
     * definition: available collateral + cost basis of open positions (not
     * mark-to-market).
     */
    public CryptoPaperAccountDTO getAccountSummary() {
        KrakenFuturesPaperAccount account = getAccount();
        List<KrakenFuturesPaperOrder> openOrders =
                orderRepository.findByStatusOrderByCreatedAtDesc("OPEN");

        BigDecimal openExposure = openOrders.stream()
                .map(KrakenFuturesPaperOrder::getUsdAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal equity = account.getCollateralUsd().add(openExposure);
        BigDecimal pnlPercent = account.getInitCollateralUsd() == null
                || account.getInitCollateralUsd().signum() == 0
                ? BigDecimal.ZERO
                : equity.subtract(account.getInitCollateralUsd())
                        .divide(account.getInitCollateralUsd(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        BigDecimal unrealizedPnlUsd = computeUnrealizedPnlUsd(openOrders);
        BigDecimal unrealizedPnlPct = pctOfInitCollateral(unrealizedPnlUsd, account.getInitCollateralUsd());
        BigDecimal totalPnlUsd = account.getRealizedPnlUsd().add(unrealizedPnlUsd);
        BigDecimal totalPnlPct = pctOfInitCollateral(totalPnlUsd, account.getInitCollateralUsd());

        return CryptoPaperAccountDTO.builder()
                .initCapitalEur(account.getInitCollateralUsd())
                .cashEur(account.getCollateralUsd())
                .equityEur(equity)
                .realizedPnlEur(account.getRealizedPnlUsd())
                .realizedPnlPercent(pnlPercent)
                .unrealizedPnlEur(unrealizedPnlUsd)
                .unrealizedPnlPct(unrealizedPnlPct)
                .totalPnlEur(totalPnlUsd)
                .totalPnlPct(totalPnlPct)
                .openPositionsCount(openOrders.size())
                .updatedAt(account.getUpdatedAt() != null ? account.getUpdatedAt().toString() : null)
                .openPositions(openOrders.stream()
                        .map(o -> {
                            FeaturedStrategy fs = featuredStrategyRepository
                                    .findByNameAndSecurityName(o.getStrategyName(), o.getSymbol());
                            return CryptoPaperPositionDTO.builder()
                                    .ticker(o.getSymbol())
                                    .strategyName(o.getStrategyName())
                                    .eurAmount(o.getUsdAmount())
                                    .filledPrice(o.getEntryPrice())
                                    .filledQuantity(o.getContracts())
                                    .createdAt(o.getCreatedAt() != null ? o.getCreatedAt().toString() : null)
                                    .signalStrength(o.getSignalStrength())
                                    .historicalWinRate(fs != null ? fs.getProfitableTradesRatio() : null)
                                    .historicalSqn(fs != null ? fs.getSqn() : null)
                                    .historicalTrades(fs != null ? fs.getNumberofTrades() : null)
                                    .build();
                        })
                        .toList())
                .build();
    }

    // ── private ──────────────────────────────────────────────────────────

    private void openPosition(String direction, String strategyName, String symbol,
                              BigDecimal markPrice, SignalStrength strength) {
        KrakenFuturesPaperAccount account = getAccount();
        BigDecimal openExposure = orderRepository.sumOpenExposureUsd();
        BigDecimal collateral = account.getCollateralUsd().add(openExposure);
        BigDecimal positionUsd = sizePosition(collateral, strength);

        RiskCheckResult riskCheck = riskManagementService.checkEntry(positionUsd, collateral);
        if (!riskCheck.allowed()) {
            log.warn("KrakenFuturesPaperTradingService: skip {} {} — blocked by risk management: {}",
                    direction, symbol, riskCheck.reason());
            return;
        }

        BigDecimal maxTotalExposure = collateral.multiply(maxTotalExposurePct);
        if (openExposure.add(positionUsd).compareTo(maxTotalExposure) > 0) {
            log.debug("KrakenFuturesPaperTradingService: skip {} {} — exposure cap reached "
                    + "(open={}, new={}, max={})", direction, symbol, openExposure, positionUsd, maxTotalExposure);
            return;
        }

        BigDecimal entryFee = positionUsd.multiply(takerFeeFraction).setScale(8, RoundingMode.HALF_UP);
        BigDecimal collateralNeeded = positionUsd.add(entryFee);
        if (account.getCollateralUsd().compareTo(collateralNeeded) < 0) {
            log.debug("KrakenFuturesPaperTradingService: skip {} {} — insufficient collateral "
                    + "(have={}, need={})", direction, symbol, account.getCollateralUsd(), collateralNeeded);
            return;
        }

        // Contracts = USD amount / mark price (Kraken PF_* contract = 1 USD notional)
        BigDecimal contracts = positionUsd.divide(markPrice, 0, RoundingMode.HALF_UP);

        KrakenFuturesPaperOrder order = new KrakenFuturesPaperOrder();
        order.setSymbol(symbol);
        order.setDirection(direction);
        order.setStrategyName(strategyName);
        order.setUsdAmount(positionUsd);
        order.setContracts(contracts);
        order.setEntryPrice(markPrice);
        order.setStatus("OPEN");
        if (strength != null) {
            order.setSignalStrength(strength.name());
        }
        orderRepository.save(order);

        account.setCollateralUsd(account.getCollateralUsd().subtract(collateralNeeded));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("PAPER FUTURES: {} {} — {}USD @ {} ({} contracts, fee={}, strategy={}, strength={})",
                direction, symbol, positionUsd, markPrice, contracts, entryFee,
                strategyName, strength != null ? strength : "n/a");
    }

    private void closePosition(String direction, String strategyName, String symbol,
                               BigDecimal markPrice) {
        Optional<KrakenFuturesPaperOrder> openOpt = orderRepository
                .findTopBySymbolAndStrategyNameAndDirectionAndStatusOrderByCreatedAtDesc(
                        symbol, strategyName, direction, "OPEN");

        if (openOpt.isEmpty()) {
            log.debug("KrakenFuturesPaperTradingService: close {} {}/{} — no open position, skip",
                    direction, strategyName, symbol);
            return;
        }

        KrakenFuturesPaperOrder pos = openOpt.get();
        BigDecimal exitUsd = pos.getContracts().multiply(markPrice);
        BigDecimal entryFee = pos.getUsdAmount().multiply(takerFeeFraction).setScale(8, RoundingMode.HALF_UP);
        BigDecimal exitFee  = exitUsd.multiply(takerFeeFraction).setScale(8, RoundingMode.HALF_UP);

        // Raw PnL before fees and funding
        BigDecimal rawPnl;
        if ("LONG".equals(direction)) {
            rawPnl = pos.getContracts().multiply(markPrice.subtract(pos.getEntryPrice()));
        } else {
            rawPnl = pos.getContracts().multiply(pos.getEntryPrice().subtract(markPrice));
        }

        BigDecimal netPnl = rawPnl
                .subtract(entryFee)
                .subtract(exitFee)
                .subtract(pos.getTotalFundingPaidUsd());

        pos.setExitPrice(markPrice);
        pos.setRealizedPnlUsd(netPnl);
        pos.setStatus("CLOSED");
        pos.setClosedAt(LocalDateTime.now());
        orderRepository.save(pos);

        KrakenFuturesPaperAccount account = getAccount();
        // Collateral accounting:
        //   At open we deducted: positionUsd + entryFee
        //   Funding was deducted each 8h by the scheduler (already gone from collateral)
        //   At close we return: positionUsd + rawPnl (price gain/loss) - exitFee
        //   netPnl (for P&L reporting) = rawPnl - entryFee - exitFee - totalFunding
        account.setCollateralUsd(account.getCollateralUsd().add(pos.getUsdAmount()).add(rawPnl).subtract(exitFee));
        account.setRealizedPnlUsd(account.getRealizedPnlUsd().add(netPnl));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("PAPER FUTURES CLOSE: {} {} @ {} — PnL={}USD (rawPnl={}, fees={}, funding={}, strategy={})",
                direction, symbol, markPrice, netPnl, rawPnl,
                entryFee.add(exitFee), pos.getTotalFundingPaidUsd(), strategyName);
    }

    private BigDecimal sizePosition(BigDecimal collateral, SignalStrength strength) {
        BigDecimal base = collateral.multiply(positionPctOfCollateral).multiply(multiplierFor(strength));
        return base.max(minPositionUsd).min(maxPositionUsd);
    }

    private BigDecimal multiplierFor(SignalStrength strength) {
        if (strength == null) return BigDecimal.ONE;
        return switch (strength) {
            case STRONG   -> strongMultiplier;
            case ELEVATED -> elevatedMultiplier;
            case BASE     -> BigDecimal.ONE;
        };
    }

    private KrakenFuturesPaperAccount getAccount() {
        return accountRepository.findAll().get(0);
    }

    // ── unrealized PnL ───────────────────────────────────────────────────

    /**
     * Sum of mark-to-market PnL across every open position — (currentPrice -
     * entryPrice) × contracts for LONG, inverted for SHORT, mirroring {@link
     * #closePosition}'s {@code rawPnl} (before fees/funding, which only apply
     * on realization). Skips a position silently (contributes 0) when no
     * current price can be found, rather than failing the whole summary.
     */
    private BigDecimal computeUnrealizedPnlUsd(List<KrakenFuturesPaperOrder> openOrders) {
        BigDecimal total = BigDecimal.ZERO;
        for (KrakenFuturesPaperOrder o : openOrders) {
            BigDecimal currentPrice = latestPrice(o.getSymbol());
            if (currentPrice == null || o.getContracts() == null || o.getEntryPrice() == null) {
                continue;
            }
            BigDecimal diff = "LONG".equals(o.getDirection())
                    ? currentPrice.subtract(o.getEntryPrice())
                    : o.getEntryPrice().subtract(currentPrice);
            total = total.add(o.getContracts().multiply(diff));
        }
        return total.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Latest close for {@code symbol}: {@code security_price} (daily) first,
     * falling back to the latest 15m {@code intraday_bar} close. On Kraken
     * Futures only the regime product ({@code PF_XBTUSD}) gets a daily sync
     * ({@link KrakenFuturesIntradayDataService#syncDailyCloses}) — every other
     * PF_* symbol has 15m bars only, so this almost always falls through to
     * the intraday_bar branch here. Null when neither source has data.
     */
    private BigDecimal latestPrice(String symbol) {
        Security security = securityRepository.findByName(symbol);
        if (security == null) {
            return null;
        }
        SecurityPrice daily = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
        if (daily != null && daily.getClose() != null) {
            return daily.getClose();
        }
        IntradayBar bar = intradayBarRepository.findTopBySecurityIdOrderByBarTimestampDesc(security.getId());
        return bar != null ? bar.getClose() : null;
    }

    private static BigDecimal pctOfInitCollateral(BigDecimal amountUsd, BigDecimal initCollateralUsd) {
        if (initCollateralUsd == null || initCollateralUsd.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amountUsd.divide(initCollateralUsd, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
