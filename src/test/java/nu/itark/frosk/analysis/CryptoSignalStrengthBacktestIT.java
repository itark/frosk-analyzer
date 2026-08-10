package nu.itark.frosk.analysis;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.dataset.COINBASEDataManager;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.CryptoIntradayDataService;
import nu.itark.frosk.service.CryptoRegimeService;
import nu.itark.frosk.service.TransactionFeeService;
import nu.itark.frosk.strategies.CryptoEMACrossLongIntradayStrategy;
import nu.itark.frosk.strategies.CryptoEMACrossShortIntradayStrategy;
import nu.itark.frosk.strategies.CryptoIntradayStrategy;
import nu.itark.frosk.strategies.CryptoRangeBreakoutIntradayStrategy;
import nu.itark.frosk.strategies.CryptoVWAPReversionIntradayStrategy;
import nu.itark.frosk.strategies.ISignalStrength;
import nu.itark.frosk.strategies.SignalStrength;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.cost.CostModel;
import org.ta4j.core.analysis.cost.LinearBorrowingCostModel;
import org.ta4j.core.analysis.cost.LinearTransactionCostModel;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.backtest.TradeOnNextOpenModel;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Diagnostic-only, not a permanent fixture: does the rule-based signal
 * strength score (ISignalStrength — see CryptoEMACrossLongIntradayStrategy
 * javadoc for the design rationale) actually correlate with better trade
 * outcomes on real data, before trusting it to scale real position size?
 *
 * <p>Syncs live 15m Coinbase candles (same network read as
 * StrategyComparisonReportIT#cryptoIntradayReport), runs each strategy's own
 * backtest, and for every closed position looks up
 * {@code strategy.getSignalStrength(entryIndex)} — the exact same call the
 * live Tier-0 pipeline makes — to bucket win rate and avg PnL by tier.
 * Delete this file once the finding has been acted on; it has no reason to
 * outlive the investigation.
 *
 * <p>Overrides {@code crypto.intraday.products} to the full production list — the
 * "test" profile's default is just BTC/ETH/SOL, far too small a sample (5 closed
 * trades total) to say anything about whether STRONG actually outperforms BASE.
 */
@TestPropertySource(properties = {
        "crypto.intraday.products=" + CryptoSignalStrengthBacktestIT.PRODUCTS
})
public class CryptoSignalStrengthBacktestIT extends BaseIntegrationTest {

    static final String PRODUCTS =
            "AAVE-EUR,ADA-EUR,AERO-USD,ALLO-USD,ATOM-EUR,BAT-EUR,BCH-EUR,BILL-USD,BTC-EUR,CRV-EUR,DEGEN-USD,"
                    + "ETH-EUR,EURC-USDC,HBAR-USD,HYPE-USD,ICP-EUR,LIGHTER-USD,LSETH-USD,LTC-EUR,MON-USD,NEAR-USD,"
                    + "ONDO-USD,PENGU-USD,RE-USD,SOL-EUR,SUI-USD,TAO-USD,TOSHI-USD,TRAC-USD,USDC-EUR,USDT-EUR,"
                    + "VVV-USD,WLD-USD,XLM-EUR,XRP-EUR,XTZ-EUR,ZEC-USD";

    /** Coinbase taker round-trip (2 × 0.6%) in percent — same constant as StrategyComparisonReportIT. */
    private static final double CRYPTO_ROUND_TRIP_FEE_PCT = 1.2;

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    CryptoIntradayDataService cryptoIntradayDataService;

    @Autowired
    CryptoVWAPReversionIntradayStrategy vwapReversionStrategy;

    @Autowired
    CryptoRangeBreakoutIntradayStrategy rangeBreakoutStrategy;

    @Autowired
    CryptoEMACrossLongIntradayStrategy emaCrossLongStrategy;

    @Autowired
    CryptoEMACrossShortIntradayStrategy emaCrossShortStrategy;

    @Autowired
    TransactionFeeService transactionFeeService;

    @Autowired
    CryptoRegimeService cryptoRegimeService;

    @Autowired
    COINBASEDataManager coinbaseDataManager;

    @Autowired
    nu.itark.frosk.repo.SecurityRepository securityRepository;

    /** Same pattern as StrategyComparisonReportIT#seedCryptoSecuritiesIfMissing, for the full universe. */
    private void seedCryptoSecuritiesIfMissing() {
        for (String product : Arrays.asList(PRODUCTS.split(","))) {
            if (securityRepository.findByName(product) == null) {
                Security seed = new Security(product, product + " (signal strength backtest seed)", "COINBASE", "EUR");
                seed.setActive(true);
                securityRepository.save(seed);
            }
        }
    }

    @Test
    public void breakDownByStrengthTier() {
        seedCryptoSecuritiesIfMissing();
        // RangeBreakout/EMACrossLong/EMACrossShort all gate entries on CryptoRegimeRule,
        // which reads daily BTC-EUR closes from security_price. The test DB's snapshot of
        // that table is frozen at 2026-06-12 — two months stale relative to the 15m bar
        // window under test — so every bar floors to the same one stale regime value,
        // silently suppressing every entry for those three strategies (confirmed: they
        // reported exactly 0 closed trades before this fix, while VWAPReversion — the one
        // strategy with no regime gate — reported hundreds). Backfill real daily BTC-EUR
        // history via the same free public Coinbase endpoint production uses, then drop
        // the already-built (stale) cache so it rebuilds from the fresh data.
        coinbaseDataManager.syncronize("BTC-EUR");
        cryptoRegimeService.clearCache();
        Map<Security, BarSeries> seriesMap = cryptoIntradayDataService.syncAndBuildAllSeries();
        assertFalse(seriesMap.isEmpty(), "No crypto intraday series — Coinbase sync failed?");
        List<BarSeries> seriesList = seriesMap.values().stream().toList();

        System.out.println("\n=== SIGNAL STRENGTH BACKTEST (" + seriesList.size()
                + " products, 15m bars, taker fee 0.6%/trade) ===");

        report("CryptoVWAPReversionIntradayStrategy", vwapReversionStrategy, seriesList);
        report("CryptoRangeBreakoutIntradayStrategy", rangeBreakoutStrategy, seriesList);
        report("CryptoEMACrossLongIntradayStrategy", emaCrossLongStrategy, seriesList);
        report("CryptoEMACrossShortIntradayStrategy", emaCrossShortStrategy, seriesList);
    }

    private void report(String label, Object strategyInstance, List<BarSeries> seriesList) {
        if (!(strategyInstance instanceof ISignalStrength strengthSource)) {
            System.out.println(label + " does not implement ISignalStrength — skipping");
            return;
        }
        boolean isShort = strategyInstance instanceof CryptoIntradayStrategy cis && cis.isShort();

        Map<SignalStrength, Bucket> byTier = new EnumMap<>(SignalStrength.class);
        for (SignalStrength tier : SignalStrength.values()) {
            byTier.put(tier, new Bucket());
        }

        for (BarSeries series : seriesList) {
            if (series.getBarData().isEmpty()) continue;

            Strategy ta4jStrategy;
            TradingRecord record;
            try {
                ta4jStrategy = buildStrategy(strategyInstance, series);
                record = isShort ? runShort(ta4jStrategy, series) : barSeriesService.runConfiguredStrategy(series, ta4jStrategy);
            } catch (Exception e) {
                continue;
            }

            for (Position position : record.getPositions()) {
                if (!position.isClosed()) continue;
                int entryIdx = position.getEntry().getIndex();
                double pnlPct = (position.getGrossReturn().doubleValue() - 1) * 100;
                if (Double.isNaN(pnlPct)) continue;

                SignalStrength tier = strengthSource.getSignalStrength(entryIdx);
                byTier.get(tier).add(pnlPct);
            }
        }

        System.out.println("\n-- " + label + " --");
        System.out.printf("%-10s %8s %10s %12s %12s%n", "TIER", "n", "winRate%", "avgPnL/tr", "netPnL/tr");
        int total = byTier.values().stream().mapToInt(b -> b.count).sum();
        for (SignalStrength tier : SignalStrength.values()) {
            Bucket b = byTier.get(tier);
            double avgPnl = b.avg();
            System.out.printf("%-10s %8d %10.1f %12.3f %12.3f%n",
                    tier, b.count,
                    b.count > 0 ? 100.0 * b.wins / b.count : 0,
                    avgPnl, avgPnl - CRYPTO_ROUND_TRIP_FEE_PCT);
        }
        System.out.println("total closed trades: " + total);
    }

    private Strategy buildStrategy(Object strategyInstance, BarSeries series) {
        if (strategyInstance instanceof CryptoVWAPReversionIntradayStrategy s) return s.buildStrategy(series);
        if (strategyInstance instanceof CryptoRangeBreakoutIntradayStrategy s) return s.buildStrategy(series);
        if (strategyInstance instanceof CryptoEMACrossLongIntradayStrategy s) return s.buildStrategy(series);
        if (strategyInstance instanceof CryptoEMACrossShortIntradayStrategy s) return s.buildStrategy(series);
        throw new IllegalArgumentException("Unhandled strategy type: " + strategyInstance.getClass());
    }

    /**
     * {@link BarSeriesService#runConfiguredStrategy} always opens {@code TradeType.BUY}
     * positions — correct for every long strategy, but it would silently invert PnL for
     * a short strategy's entry/exit rules. Same cost model and trade-execution model as
     * production (see BarSeriesService), just with the trade type a short strategy needs.
     */
    private TradingRecord runShort(Strategy strategy, BarSeries series) {
        double fee = transactionFeeService.resolveFeeFraction(strategy.getName());
        CostModel transactionCostModel = new LinearTransactionCostModel(fee);
        CostModel borrowingCostModel = new LinearBorrowingCostModel(0.00001);
        BarSeriesManager manager = new BarSeriesManager(series, transactionCostModel, borrowingCostModel,
                new TradeOnNextOpenModel());
        return manager.run(strategy, Trade.TradeType.SELL);
    }

    private static class Bucket {
        int count;
        int wins;
        double sumPnl;

        void add(double pnlPct) {
            count++;
            sumPnl += pnlPct;
            if (pnlPct > 0) wins++;
        }

        double avg() {
            return count > 0 ? sumPnl / count : 0;
        }
    }
}
