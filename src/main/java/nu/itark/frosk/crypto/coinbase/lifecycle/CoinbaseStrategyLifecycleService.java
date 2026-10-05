package nu.itark.frosk.crypto.coinbase.lifecycle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.controller.DataController;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMetrics;
import nu.itark.frosk.crypto.kraken.lifecycle.StrategyMode;
import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import nu.itark.frosk.model.CoinbaseStrategyConfig;
import nu.itark.frosk.model.CoinbaseStrategyModeChange;
import nu.itark.frosk.model.CryptoPaperOrder;
import nu.itark.frosk.repo.CoinbaseStrategyConfigRepository;
import nu.itark.frosk.repo.CoinbaseStrategyModeChangeRepository;
import nu.itark.frosk.repo.CryptoPaperOrderRepository;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.service.CryptoIntradayStrategyRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Promotion lifecycle for Coinbase strategies: readiness metrics, the hard
 * promotion criteria, and every mode change. Mirrors
 * {@code KrakenStrategyLifecycleService}; see that class for the full
 * rationale. Differences, all consequences of Coinbase being spot/long-only
 * rather than leveraged futures:
 * <ul>
 *   <li>Closed trades come from {@code crypto_paper_order} SELL rows (see
 *       {@link CryptoPaperOrderRepository#findByStrategyNameAndSideOrderByCreatedAtAsc}),
 *       not a single row that flips OPEN→CLOSED.</li>
 *   <li>No per-trade market-regime breakdown — {@code CryptoPaperOrder} does
 *       not carry a regime column, unlike {@code KrakenFuturesPaperOrder}, so
 *       {@code byRegime} is always empty here.</li>
 *   <li>PnL is denominated in EUR, not USD.</li>
 * </ul>
 *
 * <h3>Transitions</h3>
 * <ul>
 *   <li>Promote (criteria enforced): PAPER → SHADOW, PAPER → LIVE, SHADOW → LIVE</li>
 *   <li>Demote (never gated): any mode → PAPER or DISABLED</li>
 * </ul>
 * Rollback is deliberately never gated: getting a strategy OUT of real-money
 * trading must always be one click, whatever its metrics say.
 */
@Service
@Profile("crypto")
@RequiredArgsConstructor
@Slf4j
public class CoinbaseStrategyLifecycleService {

    private static final java.time.format.DateTimeFormatter TS = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<StrategyMode> PROMOTION_TARGETS = Set.of(StrategyMode.SHADOW, StrategyMode.LIVE);
    private static final Set<StrategyMode> DEMOTION_TARGETS = Set.of(StrategyMode.PAPER, StrategyMode.DISABLED);

    private final CryptoIntradayStrategyRunner strategyRunner;
    private final CoinbaseStrategyModeService modeService;
    private final CoinbaseStrategyConfigRepository configRepository;
    private final CoinbaseStrategyModeChangeRepository modeChangeRepository;
    private final CryptoPaperOrderRepository paperOrderRepository;
    private final IntradaySignalRepository signalRepository;
    private final LiveTradingGate liveTradingGate;

    @Value("${coinbase.strategy.promotion.min.closed.trades:30}")
    private int minClosedTrades;

    @Value("${coinbase.strategy.promotion.min.days.running:30}")
    private int minDaysRunning;

    @Value("${coinbase.strategy.promotion.max.drawdown.pct:15}")
    private BigDecimal maxDrawdownPct;

    // ── backfill ─────────────────────────────────────────────────────────

    /**
     * One-off, idempotent repair of SELL rows written before
     * {@code entry_eur_amount} existed. Without it every historical trade has a
     * null notional, {@link StrategyMetrics} skips it, and the lifecycle panel
     * shows 0 closed trades for strategies that have traded for months.
     *
     * <p>Pairs each unfilled SELL with the latest earlier CLOSED BUY of the same
     * strategy, ticker and exact quantity (a SELL always closes the full BUY
     * quantity), each BUY used at most once, and copies its notional and mode.
     * Rows with no match are left null and stay excluded from the metrics.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillEntryNotional() {
        int fixed = 0, unmatched = 0;
        for (String strategy : strategyRunner.getStrategyNames()) {
            List<CryptoPaperOrder> sells = paperOrderRepository
                    .findByStrategyNameAndSideOrderByCreatedAtAsc(strategy, "SELL").stream()
                    .filter(o -> o.getEntryEurAmount() == null).toList();
            if (sells.isEmpty()) continue;
            List<CryptoPaperOrder> buys = new ArrayList<>(paperOrderRepository
                    .findByStrategyNameAndSideOrderByCreatedAtAsc(strategy, "BUY").stream()
                    .filter(o -> "CLOSED".equals(o.getStatus())).toList());
            for (CryptoPaperOrder sell : sells) {
                CryptoPaperOrder match = null;
                for (CryptoPaperOrder buy : buys) {
                    if (buy.getTicker().equals(sell.getTicker())
                            && buy.getFilledQuantity() != null && sell.getFilledQuantity() != null
                            && buy.getFilledQuantity().compareTo(sell.getFilledQuantity()) == 0
                            && !buy.getCreatedAt().isAfter(sell.getCreatedAt())) {
                        match = buy; // keep scanning: latest qualifying BUY wins
                    }
                }
                if (match == null) { unmatched++; continue; }
                buys.remove(match);
                sell.setEntryEurAmount(match.getEurAmount());
                sell.setExecutionMode(match.getExecutionMode());
                paperOrderRepository.save(sell);
                fixed++;
            }
        }
        if (fixed > 0 || unmatched > 0) {
            log.warn("CoinbaseStrategyLifecycle: backfilled entry_eur_amount on {} historical SELL rows ({} unmatched)",
                    fixed, unmatched);
        }
    }

    // ── queries ──────────────────────────────────────────────────────────

    public List<CoinbaseStrategyReadinessDTO> readinessAll() {
        return strategyRunner.getStrategyNames().stream().sorted().map(this::buildReadiness).toList();
    }

    public CoinbaseStrategyReadinessDTO readiness(String strategyName) {
        requireKnown(strategyName);
        return buildReadiness(strategyName);
    }

    // ── commands ─────────────────────────────────────────────────────────

    /**
     * @throws StrategyNotFoundException  unknown strategy (404)
     * @throws InvalidTransitionException not a promotion path from the current mode (409)
     * @throws PromotionRefusedException  a hard criterion fails (422) — carries the readiness
     */
    @Transactional
    public CoinbaseStrategyReadinessDTO promote(String strategyName, StrategyMode target) {
        requireKnown(strategyName);
        if (target == null || !PROMOTION_TARGETS.contains(target)) {
            throw new InvalidTransitionException("targetMode måste vara SHADOW eller LIVE (fick " + target + ")");
        }
        StrategyMode current = modeService.getMode(strategyName);
        boolean allowed = (current == StrategyMode.PAPER)
                || (current == StrategyMode.SHADOW && target == StrategyMode.LIVE);
        if (!allowed) {
            throw new InvalidTransitionException(
                    "Kan inte gå från " + current + " till " + target + " — tillåtet: PAPER→SHADOW, PAPER→LIVE, SHADOW→LIVE");
        }

        CoinbaseStrategyReadinessDTO readiness = buildReadiness(strategyName);
        if (!readiness.isReadyForPromotion()) {
            log.warn("CoinbaseStrategyLifecycle: promotion {} {}→{} REFUSED — {}",
                    strategyName, current, target, readiness.getBlockers());
            throw new PromotionRefusedException(readiness);
        }

        changeMode(strategyName, current, target, readiness);
        return buildReadiness(strategyName);
    }

    /** Rollback to PAPER or DISABLED — never gated by criteria. */
    @Transactional
    public CoinbaseStrategyReadinessDTO demote(String strategyName, StrategyMode target) {
        requireKnown(strategyName);
        if (target == null || !DEMOTION_TARGETS.contains(target)) {
            throw new InvalidTransitionException("targetMode måste vara PAPER eller DISABLED (fick " + target + ")");
        }
        StrategyMode current = modeService.getMode(strategyName);
        if (current == target) {
            throw new InvalidTransitionException(strategyName + " är redan " + target);
        }
        changeMode(strategyName, current, target, buildReadiness(strategyName));
        return buildReadiness(strategyName);
    }

    // ── internals ────────────────────────────────────────────────────────

    private void changeMode(String strategyName, StrategyMode from, StrategyMode to, CoinbaseStrategyReadinessDTO snapshot) {
        modeService.setMode(strategyName, to);

        CoinbaseStrategyModeChange audit = new CoinbaseStrategyModeChange();
        audit.setStrategyName(strategyName);
        audit.setFromMode(from);
        audit.setToMode(to);
        audit.setClosedTrades(snapshot.getClosedTrades());
        audit.setDaysRunning(snapshot.getDaysRunning());
        audit.setMaxDrawdownPct(snapshot.getMaxDrawdownPct());
        modeChangeRepository.save(audit);

        log.warn("CoinbaseStrategyLifecycle: {} mode {} → {} (trades={}, days={}, maxDD={}%, liveTradingEnabled={})",
                strategyName, from, to, snapshot.getClosedTrades(), snapshot.getDaysRunning(),
                snapshot.getMaxDrawdownPct(), liveTradingGate.isEnabled());
    }

    CoinbaseStrategyReadinessDTO buildReadiness(String strategyName) {
        List<CryptoPaperOrder> closedSells =
                paperOrderRepository.findByStrategyNameAndSideOrderByCreatedAtAsc(strategyName, "SELL");
        StrategyMetrics m = StrategyMetrics.of(closedSells.stream()
                .map(o -> new StrategyMetrics.Trade(o.getRealizedPnlEur(), o.getEntryEurAmount()))
                .toList());

        int days = daysRunning(strategyName);
        boolean enabledByConfig = strategyRunner.isEnabledByConfig(strategyName);
        boolean preReg = DataController.PRE_REGISTRATION_PENDING_STRATEGIES.contains(strategyName);

        List<String> blockers = new ArrayList<>();
        if (!enabledByConfig) {
            blockers.add("Strategin är avstängd i application-crypto.properties och genererar inga signaler");
        }
        if (preReg) {
            blockers.add("Pre-registrerat test pågår (~/itark/PREREG_*.md) — får inte handla riktiga pengar före datagaten");
        }
        if (m.closedTrades() < minClosedTrades) {
            blockers.add("Minst " + minClosedTrades + " avslutade trades krävs (" + m.closedTrades() + "/" + minClosedTrades + ")");
        }
        if (days < minDaysRunning) {
            blockers.add("Minst " + minDaysRunning + " dagar krävs (" + days + "/" + minDaysRunning + ")");
        }
        if (m.maxDrawdownPct() != null && m.maxDrawdownPct().compareTo(maxDrawdownPct) >= 0) {
            blockers.add("Max drawdown måste vara under " + maxDrawdownPct.stripTrailingZeros().toPlainString()
                    + " % (är " + m.maxDrawdownPct() + " %)");
        }

        Optional<CoinbaseStrategyConfig> config = configRepository.findById(strategyName);
        return CoinbaseStrategyReadinessDTO.builder()
                .strategyName(strategyName)
                .mode(modeService.getMode(strategyName))
                .modeChangedAt(config.map(CoinbaseStrategyConfig::getModeChangedAt).map(t -> t.format(TS)).orElse(null))
                .closedTrades(m.closedTrades())
                .daysRunning(days)
                .maxDrawdownPct(m.maxDrawdownPct())
                .sharpeRatio(m.sharpeRatio())
                .winRate(m.winRate())
                .avgRR(m.avgRR())
                .totalPnlEur(m.totalPnlUsd())
                .byRegime(List.of())
                .readyForPromotion(blockers.isEmpty())
                .blockers(blockers)
                .minClosedTrades(minClosedTrades)
                .minDaysRunning(minDaysRunning)
                .maxAllowedDrawdownPct(maxDrawdownPct)
                .enabledByConfig(enabledByConfig)
                .preRegistrationPending(preReg)
                .liveTradingEnabled(liveTradingGate.isEnabled())
                .build();
    }

    /**
     * Whole days since the strategy's first signal on this process (its first
     * paper order if it has no signal rows). 0 when it has never signalled.
     */
    private int daysRunning(String strategyName) {
        Optional<LocalDateTime> start = signalRepository.findTopByStrategyNameOrderBySignalTimestampAsc(strategyName)
                .map(s -> LocalDateTime.ofInstant(Instant.ofEpochSecond(s.getSignalTimestamp()), ZoneId.systemDefault()))
                .or(() -> paperOrderRepository.findTopByStrategyNameOrderByCreatedAtAsc(strategyName)
                        .map(CryptoPaperOrder::getCreatedAt));
        return start.map(t -> (int) Math.max(0, Duration.between(t, LocalDateTime.now()).toDays())).orElse(0);
    }

    private void requireKnown(String strategyName) {
        if (!strategyRunner.getStrategyNames().contains(strategyName)) {
            throw new StrategyNotFoundException("Okänd strategi: " + strategyName);
        }
    }

    // ── exceptions (mapped to HTTP status in CoinbaseStrategyLifecycleController) ──

    public static class StrategyNotFoundException extends RuntimeException {
        public StrategyNotFoundException(String message) { super(message); }
    }

    public static class InvalidTransitionException extends RuntimeException {
        public InvalidTransitionException(String message) { super(message); }
    }

    @Getter
    public static class PromotionRefusedException extends RuntimeException {
        private final CoinbaseStrategyReadinessDTO readiness;

        public PromotionRefusedException(CoinbaseStrategyReadinessDTO readiness) {
            super(String.join("; ", readiness.getBlockers()));
            this.readiness = readiness;
        }
    }
}
