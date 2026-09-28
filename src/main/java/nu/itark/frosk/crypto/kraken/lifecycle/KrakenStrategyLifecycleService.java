package nu.itark.frosk.crypto.kraken.lifecycle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.controller.DataController;
import nu.itark.frosk.crypto.livetrading.LiveTradingGate;
import nu.itark.frosk.model.KrakenFuturesPaperOrder;
import nu.itark.frosk.model.KrakenStrategyConfig;
import nu.itark.frosk.model.KrakenStrategyModeChange;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.KrakenFuturesPaperOrderRepository;
import nu.itark.frosk.repo.KrakenStrategyConfigRepository;
import nu.itark.frosk.repo.KrakenStrategyModeChangeRepository;
import nu.itark.frosk.service.CryptoIntradayStrategyRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Promotion lifecycle for Kraken Futures strategies: readiness metrics, the hard
 * promotion criteria, and every mode change.
 *
 * <h3>Transitions</h3>
 * <ul>
 *   <li>Promote (criteria enforced): PAPER → SHADOW, PAPER → LIVE, SHADOW → LIVE</li>
 *   <li>Demote (never gated): any mode → PAPER or DISABLED</li>
 * </ul>
 * Rollback is deliberately never gated: getting a strategy OUT of real-money
 * trading must always be one click, whatever its metrics say.
 *
 * <h3>Hard criteria</h3>
 * Minimum closed trades, minimum days running, maximum drawdown (all
 * configurable, defaults 30 / 30 / 15%), plus two that make promotion
 * meaningless or wrong: a strategy switched off by its property never signals,
 * and a pre-registered experiment must not trade real money before its data gate.
 */
@Service
@Profile("kraken-futures")
@RequiredArgsConstructor
@Slf4j
public class KrakenStrategyLifecycleService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<StrategyMode> PROMOTION_TARGETS = Set.of(StrategyMode.SHADOW, StrategyMode.LIVE);
    private static final Set<StrategyMode> DEMOTION_TARGETS = Set.of(StrategyMode.PAPER, StrategyMode.DISABLED);

    private final CryptoIntradayStrategyRunner strategyRunner;
    private final KrakenStrategyModeService modeService;
    private final KrakenStrategyConfigRepository configRepository;
    private final KrakenStrategyModeChangeRepository modeChangeRepository;
    private final KrakenFuturesPaperOrderRepository paperOrderRepository;
    private final IntradaySignalRepository signalRepository;
    private final LiveTradingGate liveTradingGate;

    @Value("${kraken.strategy.promotion.min.closed.trades:30}")
    private int minClosedTrades;

    @Value("${kraken.strategy.promotion.min.days.running:30}")
    private int minDaysRunning;

    @Value("${kraken.strategy.promotion.max.drawdown.pct:15}")
    private BigDecimal maxDrawdownPct;

    // ── queries ──────────────────────────────────────────────────────────

    public List<StrategyReadinessDTO> readinessAll() {
        return strategyRunner.getStrategyNames().stream().sorted().map(this::buildReadiness).toList();
    }

    public StrategyReadinessDTO readiness(String strategyName) {
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
    public StrategyReadinessDTO promote(String strategyName, StrategyMode target) {
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

        StrategyReadinessDTO readiness = buildReadiness(strategyName);
        if (!readiness.isReadyForPromotion()) {
            log.warn("KrakenStrategyLifecycle: promotion {} {}→{} REFUSED — {}",
                    strategyName, current, target, readiness.getBlockers());
            throw new PromotionRefusedException(readiness);
        }

        changeMode(strategyName, current, target, readiness);
        return buildReadiness(strategyName);
    }

    /** Rollback to PAPER or DISABLED — never gated by criteria. */
    @Transactional
    public StrategyReadinessDTO demote(String strategyName, StrategyMode target) {
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

    private void changeMode(String strategyName, StrategyMode from, StrategyMode to, StrategyReadinessDTO snapshot) {
        modeService.setMode(strategyName, to);

        KrakenStrategyModeChange audit = new KrakenStrategyModeChange();
        audit.setStrategyName(strategyName);
        audit.setFromMode(from);
        audit.setToMode(to);
        audit.setClosedTrades(snapshot.getClosedTrades());
        audit.setDaysRunning(snapshot.getDaysRunning());
        audit.setMaxDrawdownPct(snapshot.getMaxDrawdownPct());
        modeChangeRepository.save(audit);

        log.warn("KrakenStrategyLifecycle: {} mode {} → {} (trades={}, days={}, maxDD={}%, liveTradingEnabled={})",
                strategyName, from, to, snapshot.getClosedTrades(), snapshot.getDaysRunning(),
                snapshot.getMaxDrawdownPct(), liveTradingGate.isEnabled());
    }

    StrategyReadinessDTO buildReadiness(String strategyName) {
        List<KrakenFuturesPaperOrder> closed =
                paperOrderRepository.findByStrategyNameAndStatusOrderByClosedAtAsc(strategyName, "CLOSED");
        StrategyMetrics m = StrategyMetrics.of(closed.stream()
                .map(o -> new StrategyMetrics.Trade(o.getRealizedPnlUsd(), o.getUsdAmount()))
                .toList());

        int days = daysRunning(strategyName);
        boolean enabledByConfig = strategyRunner.isEnabledByConfig(strategyName);
        boolean preReg = DataController.PRE_REGISTRATION_PENDING_STRATEGIES.contains(strategyName);

        List<String> blockers = new ArrayList<>();
        if (!enabledByConfig) {
            blockers.add("Strategin är avstängd i application-kraken-futures.properties och genererar inga signaler");
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

        Optional<KrakenStrategyConfig> config = configRepository.findById(strategyName);
        return StrategyReadinessDTO.builder()
                .strategyName(strategyName)
                .mode(modeService.getMode(strategyName))
                .modeChangedAt(config.map(KrakenStrategyConfig::getModeChangedAt).map(t -> t.format(TS)).orElse(null))
                .closedTrades(m.closedTrades())
                .daysRunning(days)
                .maxDrawdownPct(m.maxDrawdownPct())
                .sharpeRatio(m.sharpeRatio())
                .winRate(m.winRate())
                .avgRR(m.avgRR())
                .totalPnlUsd(m.totalPnlUsd())
                .byRegime(regimeBreakdown(closed))
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
     * Closed trades grouped by the regime they were opened in. Instrumentation:
     * nothing gates on regime, this is the evidence for deciding whether anything
     * should. Trades from before the regime logging group under UNKNOWN.
     */
    private List<StrategyReadinessDTO.RegimeStats> regimeBreakdown(List<KrakenFuturesPaperOrder> closed) {
        Map<String, List<KrakenFuturesPaperOrder>> byRegime = closed.stream()
                .collect(Collectors.groupingBy(o -> o.getMarketRegime() != null ? o.getMarketRegime() : "UNKNOWN",
                        LinkedHashMap::new, Collectors.toList()));
        List<StrategyReadinessDTO.RegimeStats> out = new ArrayList<>();
        byRegime.forEach((regime, orders) -> {
            BigDecimal pnl = orders.stream()
                    .map(KrakenFuturesPaperOrder::getRealizedPnlUsd)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long wins = orders.stream()
                    .filter(o -> o.getRealizedPnlUsd() != null && o.getRealizedPnlUsd().signum() > 0)
                    .count();
            out.add(StrategyReadinessDTO.RegimeStats.builder()
                    .regime(regime)
                    .closedTrades(orders.size())
                    .totalPnlUsd(pnl.setScale(2, RoundingMode.HALF_UP))
                    .winRate(BigDecimal.valueOf((double) wins / orders.size()).setScale(4, RoundingMode.HALF_UP))
                    .build());
        });
        out.sort(Comparator.comparing(StrategyReadinessDTO.RegimeStats::getRegime));
        return out;
    }

    /**
     * Whole days since the strategy's first signal on this process (its first
     * paper position if it has no signal rows). 0 when it has never signalled.
     */
    private int daysRunning(String strategyName) {
        Optional<LocalDateTime> start = signalRepository.findTopByStrategyNameOrderBySignalTimestampAsc(strategyName)
                .map(s -> LocalDateTime.ofInstant(Instant.ofEpochSecond(s.getSignalTimestamp()), ZoneId.systemDefault()))
                .or(() -> paperOrderRepository.findTopByStrategyNameOrderByCreatedAtAsc(strategyName)
                        .map(KrakenFuturesPaperOrder::getCreatedAt));
        return start.map(t -> (int) Math.max(0, Duration.between(t, LocalDateTime.now()).toDays())).orElse(0);
    }

    private void requireKnown(String strategyName) {
        if (!strategyRunner.getStrategyNames().contains(strategyName)) {
            throw new StrategyNotFoundException("Okänd strategi: " + strategyName);
        }
    }

    // ── exceptions (mapped to HTTP status in KrakenStrategyLifecycleController) ──

    public static class StrategyNotFoundException extends RuntimeException {
        public StrategyNotFoundException(String message) { super(message); }
    }

    public static class InvalidTransitionException extends RuntimeException {
        public InvalidTransitionException(String message) { super(message); }
    }

    @Getter
    public static class PromotionRefusedException extends RuntimeException {
        private final StrategyReadinessDTO readiness;

        public PromotionRefusedException(StrategyReadinessDTO readiness) {
            super(String.join("; ", readiness.getBlockers()));
            this.readiness = readiness;
        }
    }
}
