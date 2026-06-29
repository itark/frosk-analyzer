package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.CryptoPortfolioItemDTO;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Product;
import nu.itark.frosk.model.IntradayBar;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradayBarRepository;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.util.FroskUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Returns live open crypto positions by scanning INTRADAY_SIGNAL for BUY signals
 * without a subsequent SELL (per ticker + strategy). No snapshot persistence —
 * always computed fresh against the current signal log.
 */
@Service
@Profile("crypto")
@Slf4j
@RequiredArgsConstructor
public class CryptoPortfolioService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private final IntradaySignalRepository intradaySignalRepository;
    private final IntradayBarRepository intradayBarRepository;
    private final SecurityRepository securityRepository;
    private final ProductProxy productProxy;

    public List<CryptoPortfolioItemDTO> getOpenPositions() {
        List<CryptoPortfolioItemDTO> result = new ArrayList<>();

        List<String> tickers = intradaySignalRepository.findDistinctTickers();
        List<String> strategies = intradaySignalRepository.findDistinctStrategyNames();

        for (String ticker : tickers) {
            for (String strategy : strategies) {
                Optional<IntradaySignal> latestBuy = intradaySignalRepository
                        .findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(
                                strategy, ticker, "BUY");

                if (latestBuy.isEmpty()) continue;

                IntradaySignal buy = latestBuy.get();

                boolean hasSellAfter = intradaySignalRepository
                        .existsByStrategyNameAndTickerAndSignalTypeAndSignalTimestampGreaterThan(
                                strategy, ticker, "SELL", buy.getSignalTimestamp());

                if (hasSellAfter) continue;

                BigDecimal entryPrice = buy.getClosePrice();
                BigDecimal currentPrice = getCurrentPrice(ticker);

                BigDecimal pnl = null;
                if (entryPrice != null && currentPrice != null
                        && entryPrice.compareTo(BigDecimal.ZERO) != 0) {
                    pnl = FroskUtil.getPercentage(entryPrice, currentPrice)
                            .setScale(4, RoundingMode.DOWN);
                }

                String entryDateTime = DateFormatUtils.format(
                        Date.from(Instant.ofEpochSecond(buy.getSignalTimestamp())), DATE_FORMAT);

                result.add(CryptoPortfolioItemDTO.builder()
                        .ticker(ticker)
                        .strategyName(strategy.replace("Strategy", ""))
                        .entryPrice(entryPrice)
                        .currentPrice(currentPrice)
                        .unrealizedPnlPercent(pnl)
                        .entryDateTime(entryDateTime)
                        .build());
            }
        }

        log.info("CryptoPortfolioService: {} open positions across {} tickers", result.size(), tickers.size());
        return result;
    }

    private BigDecimal getCurrentPrice(String ticker) {
        try {
            Security security = securityRepository.findByName(ticker);
            if (security != null) {
                IntradayBar bar = intradayBarRepository.findTopBySecurityIdOrderByBarTimestampDesc(security.getId());
                if (bar != null) {
                    return bar.getClose();
                }
            }
        } catch (Exception e) {
            log.warn("Could not get latest intraday bar for {}: {}", ticker, e.getMessage());
        }

        try {
            Product product = productProxy.getProduct(ticker);
            if (product != null && product.getPrice() != null && !product.getPrice().isBlank()) {
                return new BigDecimal(product.getPrice());
            }
        } catch (Exception e) {
            log.warn("Could not get Coinbase price for {}: {}", ticker, e.getMessage());
        }

        return null;
    }
}
