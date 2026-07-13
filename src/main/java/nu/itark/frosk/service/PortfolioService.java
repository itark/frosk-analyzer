package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.PortfolioDTO;
import nu.itark.frosk.analysis.PortfolioPositionDTO;
import nu.itark.frosk.model.*;
import nu.itark.frosk.repo.*;
import nu.itark.frosk.util.FroskUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds and reads Portfolio snapshots based on open FeaturedStrategy positions.
 *
 * <p>Usage:
 * <ol>
 *   <li>Call {@link #build()} after each strategy run to persist a fresh snapshot.</li>
 *   <li>Call {@link #getCurrent()} to get the latest snapshot as a DTO.</li>
 *   <li>Call {@link #getHistory()} to see all historical snapshots.</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private static final double SLMS_MAX_SECTOR_RATIO = 0.30;

    @Value("${frosk.swedish.longterm.topN:20}")
    private int slmsTopN;

    /** Minimum SQN a position must have to appear in the portfolio. */
    @Value("${frosk.portfolio.min.sqn:1.0}")
    private double portfolioMinSqn;

    /**
     * Minimum profitable-trades ratio (win rate) a position must have,
     * in percent (e.g. 40.0 = 40%) — matching how
     * {@code FeaturedStrategy.profitableTradesRatio} is stored.
     */
    @Value("${frosk.portfolio.min.win.rate:40.0}")
    private double portfolioMinWinRate;

    /** Maximum number of non-SLMS positions combined, ranked by SQN. */
    @Value("${frosk.portfolio.other.topN:12}")
    private int otherTopN;

    /**
     * When true, bypasses the same-day idempotency check and always rebuilds.
     * Default false. Set to true in application-test.properties so tests are never skipped.
     */
    @Value("${frosk.portfolio.force.rebuild:false}")
    private boolean forceRebuild;

    /** Per-trade fee fraction for intraday round-trip PnL (0.0003 = 0.03%). */
    @Value("${exchange.transaction.intradayFeePerTradePercent:0.0003}")
    private double intradayFeePerTradePercent;

    private static final String TYPE_DAILY = "DAILY";
    private static final String TYPE_INTRADAY = "INTRADAY";

    private static final List<String> DAILY_STRATEGIES = List.of(
            "ShortTermMomentumLongTermStrengthStrategy",
            "HighLanderStrategy",
            "SwedishLongTermMomentumStrategy",
            "DailyOversoldBounceStrategy",
            "CANSLIMStrategy"
    );

    /**
     * Equity and crypto names both listed — the two processes have separate
     * databases, so each only ever finds its own strategies' positions.
     */
    private static final List<String> INTRADAY_STRATEGIES = List.of(
            "OpeningRangeBreakoutIntradayStrategy",
            "VWAPMeanReversionIntradayStrategy",
            "GapReversalIntradayStrategy",
            "CryptoRangeBreakoutIntradayStrategy",
            "CryptoVWAPReversionIntradayStrategy",
            "CryptoEMACrossLongIntradayStrategy"
    );

    final FeaturedStrategyRepository featuredStrategyRepository;
    final StrategyTradeRepository strategyTradeRepository;
    final SecurityRepository securityRepository;
    final SecurityPriceRepository securityPriceRepository;
    final PortfolioRepository portfolioRepository;
    final PortfolioPositionRepository portfolioPositionRepository;
    final HedgeIndexService hedgeIndexService;
    final IntradayBarRepository intradayBarRepository;
    final IntradaySignalRepository intradaySignalRepository;

    /**
     * Builds a new Portfolio snapshot from all currently open FeaturedStrategy positions.
     * Each call creates a new persisted snapshot (history is preserved).
     */
    @Transactional
    public Portfolio build() {
        if (!forceRebuild && builtToday(TYPE_DAILY)) {
            log.info("[PortfolioService] Skipping build — daily portfolio already built today ({}). " +
                    "Set frosk.portfolio.force.rebuild=true to override.", LocalDate.now());
            return portfolioRepository.findTopByPortfolioTypeOrderBySnapshotDateDesc(TYPE_DAILY).orElse(null);
        }
        log.info("Building daily portfolio snapshot...");

        List<FeaturedStrategy> allOpen = featuredStrategyRepository.findByOpen(true).stream()
                .filter(fs -> DAILY_STRATEGIES.contains(fs.getName()))
                .filter(this::passesQualityGate)
                .collect(Collectors.toList());

        // Apply 30%-per-sector cap to SwedishLongTermMomentumStrategy positions
        List<FeaturedStrategy> slmsPositions = allOpen.stream()
                .filter(fs -> "SwedishLongTermMomentumStrategy".equals(fs.getName()))
                .collect(Collectors.toList());

        // Cap HighLander + ShortTermMomentum positions by SQN (top-N only)
        List<FeaturedStrategy> otherPositions = allOpen.stream()
                .filter(fs -> !"SwedishLongTermMomentumStrategy".equals(fs.getName()))
                .sorted(Comparator.comparing(
                        (FeaturedStrategy fs) -> fs.getSqn() != null ? fs.getSqn() : BigDecimal.ZERO,
                        Comparator.reverseOrder()))
                .limit(otherTopN)
                .collect(Collectors.toList());
        List<FeaturedStrategy> cappedSlms = applySectorCap(slmsPositions, SLMS_MAX_SECTOR_RATIO);

        // Tiered HedgeIndex sizing for SLMS: score 0-3 → topN, score 4-7 → topN/2, score 8+ → 0
        int effectiveTopN = computeTieredTopN();

        // Limit SwedishLongTermMomentumStrategy positions to effectiveTopN, ranked by SQN descending
        List<FeaturedStrategy> topNSlms = cappedSlms.stream()
                .sorted(Comparator.comparing(
                        (FeaturedStrategy fs) -> fs.getSqn() != null ? fs.getSqn() : BigDecimal.ZERO,
                        Comparator.reverseOrder()))
                .limit(effectiveTopN)
                .collect(Collectors.toList());

        List<FeaturedStrategy> openStrategies = Stream.concat(otherPositions.stream(), topNSlms.stream())
                .collect(Collectors.toList());
        log.info("Portfolio: {} positions total (quality gate: sqn>={}, winRate>={}; other cap: top{}; " +
                        "SLMS after sector cap: {}, after tieredTopN={} [base={}])",
                openStrategies.size(), portfolioMinSqn, portfolioMinWinRate, otherTopN,
                cappedSlms.size(), topNSlms.size(), effectiveTopN, slmsTopN);

        Portfolio portfolio = new Portfolio();
        portfolio.setSnapshotDate(new Date());
        portfolio.setPortfolioType(TYPE_DAILY);

        List<PortfolioPosition> positions = new ArrayList<>();
        List<BigDecimal> pnlValues = new ArrayList<>();

        for (FeaturedStrategy fs : openStrategies) {
            PortfolioPosition pos = buildPosition(fs, portfolio);
            if (pos != null) {
                positions.add(pos);
                if (pos.getUnrealizedPnlPercent() != null) {
                    pnlValues.add(pos.getUnrealizedPnlPercent());
                }
            }
        }

        portfolio.setOpenPositionCount(positions.size());
        portfolio.setTotalPnlPercent(averagePnl(pnlValues));
        portfolio.getPositions().addAll(positions);

        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Daily portfolio snapshot saved: id={}, openPositions={}, avgPnl={}",
                saved.getId(), saved.getOpenPositionCount(), saved.getTotalPnlPercent());
        return saved;
    }

    /**
     * Builds an intraday portfolio snapshot from open intraday-strategy positions
     * plus today's realized round trips.
     * Rebuilds every Tier-0 cycle (no same-day idempotency — intraday positions change frequently).
     *
     * <p>Unlike the daily portfolio (average unrealized PnL of open positions),
     * the intraday {@code totalPnlPercent} is additive day-PnL: the sum of all
     * round trips closed today (net of 2× fee) plus the unrealized PnL of any
     * positions still open. Intraday round trips complete within hours, so an
     * open-positions-only snapshot read 0.0000 essentially always.
     */
    @Transactional
    public Portfolio buildIntraday() {
        log.info("Building intraday portfolio snapshot...");

        List<FeaturedStrategy> intradayOpen = featuredStrategyRepository.findByOpen(true).stream()
                .filter(fs -> INTRADAY_STRATEGIES.contains(fs.getName()))
                .collect(Collectors.toList());

        Portfolio portfolio = new Portfolio();
        portfolio.setSnapshotDate(new Date());
        portfolio.setPortfolioType(TYPE_INTRADAY);

        List<PortfolioPosition> positions = new ArrayList<>();
        BigDecimal unrealizedSum = BigDecimal.ZERO;

        for (FeaturedStrategy fs : intradayOpen) {
            PortfolioPosition pos = buildIntradayPosition(fs, portfolio);
            if (pos != null) {
                positions.add(pos);
                if (pos.getUnrealizedPnlPercent() != null) {
                    unrealizedSum = unrealizedSum.add(pos.getUnrealizedPnlPercent());
                }
            }
        }

        RealizedIntradayPnl realized = computeTodaysRealizedIntradayPnl();

        portfolio.setOpenPositionCount(positions.size());
        portfolio.setRealizedPnlPercent(realized.pnlPercent().setScale(4, RoundingMode.DOWN));
        portfolio.setClosedTradeCount(realized.roundTrips());
        portfolio.setTotalPnlPercent(realized.pnlPercent().add(unrealizedSum).setScale(4, RoundingMode.DOWN));
        portfolio.getPositions().addAll(positions);

        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Intraday portfolio snapshot saved: id={}, openPositions={}, realized={} ({} round trips), totalPnl={}",
                saved.getId(), saved.getOpenPositionCount(), saved.getRealizedPnlPercent(),
                saved.getClosedTradeCount(), saved.getTotalPnlPercent());
        return saved;
    }

    record RealizedIntradayPnl(BigDecimal pnlPercent, int roundTrips) {}

    /**
     * Pairs today's BUY→SELL signals per (ticker, strategy) and sums the
     * round-trip PnL, deducting the entry and exit fee.
     */
    private RealizedIntradayPnl computeTodaysRealizedIntradayPnl() {
        long startOfDayEpoch = LocalDate.now().atStartOfDay(ZoneId.of("Europe/Stockholm")).toEpochSecond();
        BigDecimal roundTripFeePct = BigDecimal.valueOf(2 * intradayFeePerTradePercent * 100);

        BigDecimal sum = BigDecimal.ZERO;
        int roundTrips = 0;
        for (String ticker : intradaySignalRepository.findDistinctTickers()) {
            for (String strategyName : INTRADAY_STRATEGIES) {
                List<IntradaySignal> signals = intradaySignalRepository
                        .findByTickerAndStrategyNameAndSignalTimestampGreaterThanEqualOrderBySignalTimestampAsc(
                                ticker, strategyName, startOfDayEpoch);
                IntradaySignal pendingBuy = null;
                for (IntradaySignal s : signals) {
                    if ("BUY".equals(s.getSignalType())) {
                        pendingBuy = s;
                    } else if ("SELL".equals(s.getSignalType()) && pendingBuy != null
                            && pendingBuy.getClosePrice() != null && s.getClosePrice() != null
                            && pendingBuy.getClosePrice().compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal gross = s.getClosePrice().subtract(pendingBuy.getClosePrice())
                                .divide(pendingBuy.getClosePrice(), 6, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                        sum = sum.add(gross.subtract(roundTripFeePct));
                        roundTrips++;
                        pendingBuy = null;
                    }
                }
            }
        }
        return new RealizedIntradayPnl(sum, roundTrips);
    }

    /**
     * Returns the most recently built daily portfolio snapshot as a DTO.
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getCurrent() {
        return getLatestByType(TYPE_DAILY);
    }

    /**
     * Returns the most recently built intraday portfolio snapshot as a DTO.
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getCurrentIntraday() {
        return getLatestByType(TYPE_INTRADAY);
    }

    private PortfolioDTO getLatestByType(String type) {
        Optional<Portfolio> latest = portfolioRepository.findTopByPortfolioTypeOrderBySnapshotDateDesc(type);
        if (latest.isEmpty()) {
            log.warn("No {} portfolio snapshot found.", type);
            return PortfolioDTO.builder()
                    .snapshotDate("none")
                    .openPositionCount(0)
                    .totalPnlPercent(BigDecimal.ZERO)
                    .realizedPnlPercent(BigDecimal.ZERO)
                    .closedTradeCount(0)
                    .positions(Collections.emptyList())
                    .build();
        }
        return toDTO(latest.get());
    }

    /**
     * Returns all historical daily portfolio snapshots ordered newest first.
     * Positions are NOT loaded (header data only) to keep the list lightweight.
     */
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getHistory() {
        return getHistoryByType(TYPE_DAILY);
    }

    /**
     * Returns all historical intraday portfolio snapshots ordered newest first.
     */
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getHistoryIntraday() {
        return getHistoryByType(TYPE_INTRADAY);
    }

    private List<PortfolioDTO> getHistoryByType(String type) {
        return portfolioRepository.findByPortfolioTypeOrderBySnapshotDateDesc(type).stream()
                .map(p -> PortfolioDTO.builder()
                        .id(p.getId())
                        .snapshotDate(DateFormatUtils.format(p.getSnapshotDate(), DATE_FORMAT))
                        .openPositionCount(p.getOpenPositionCount())
                        .totalPnlPercent(p.getTotalPnlPercent())
                        .realizedPnlPercent(p.getRealizedPnlPercent())
                        .closedTradeCount(p.getClosedTradeCount())
                        .positions(Collections.emptyList())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Returns the full positions for a specific historical snapshot by id.
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getById(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean builtToday(String portfolioType) {
        LocalDate today = LocalDate.now();
        Date startOfDay = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay   = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return portfolioRepository.existsByPortfolioTypeAndSnapshotDateBetween(portfolioType, startOfDay, endOfDay);
    }

    /**
     * Tiered HedgeIndex sizing for Månadsportföljen:
     * Score 0-3 (Strong Risk-On) → full topN
     * Score 4-7 (Cautious)       → topN / 2
     * Score 8+  (Defensive)      → 0 (no SLMS positions)
     */
    private int computeTieredTopN() {
        int score = hedgeIndexService.getScoreForDay(ZonedDateTime.now());
        if (score <= 3) {
            return slmsTopN;
        } else if (score <= 7) {
            return Math.max(1, slmsTopN / 2);
        } else {
            return 0;
        }
    }

    /**
     * Minimum quality gate applied to every position before it enters the portfolio.
     * Requires SQN >= frosk.portfolio.min.sqn (default 1.0)
     * and profitable trades ratio >= frosk.portfolio.min.win.rate in percent
     * (default 40.0 = 40%). Positions with no track record (null SQN or win
     * rate) are excluded.
     */
    private boolean passesQualityGate(FeaturedStrategy fs) {
        if (fs.getSqn() == null || fs.getSqn().doubleValue() < portfolioMinSqn) {
            log.debug("Quality gate excluded {} ({}) — SQN={}", fs.getSecurityName(), fs.getName(), fs.getSqn());
            return false;
        }
        if (fs.getProfitableTradesRatio() == null || fs.getProfitableTradesRatio().doubleValue() < portfolioMinWinRate) {
            log.debug("Quality gate excluded {} ({}) — winRate={}", fs.getSecurityName(), fs.getName(), fs.getProfitableTradesRatio());
            return false;
        }
        return true;
    }

    private List<FeaturedStrategy> applySectorCap(List<FeaturedStrategy> positions, double maxSectorRatio) {
        if (positions.isEmpty()) return positions;
        int maxPerSector = Math.max(1, (int) Math.ceil(positions.size() * maxSectorRatio));

        Map<String, List<FeaturedStrategy>> bySector = positions.stream()
                .collect(Collectors.groupingBy(fs -> {
                    nu.itark.frosk.model.Security sec = securityRepository.findByName(fs.getSecurityName());
                    return (sec != null && sec.getSector() != null && !sec.getSector().isBlank())
                            ? sec.getSector() : "Unknown";
                }));

        List<FeaturedStrategy> result = new ArrayList<>();
        bySector.forEach((sector, sectorPositions) -> {
            sectorPositions.stream()
                    .sorted(Comparator.comparing(
                            fs -> fs.getSqn() != null ? fs.getSqn() : BigDecimal.ZERO,
                            Comparator.reverseOrder()))
                    .limit(maxPerSector)
                    .forEach(result::add);
        });
        log.info("Sector cap applied: {} sectors, max {} per sector, {} positions kept from {}",
                bySector.size(), maxPerSector, result.size(), positions.size());
        return result;
    }

    private PortfolioPosition buildPosition(FeaturedStrategy fs, Portfolio portfolio) {
        List<StrategyTrade> trades = strategyTradeRepository.findByFeaturedStrategyId(fs.getId());
        if (trades.isEmpty()) {
            log.warn("No trades found for FeaturedStrategy id={}, security={}", fs.getId(), fs.getSecurityName());
            return null;
        }

        // The open BUY trade is the most recent one (last entry, not yet closed)
        StrategyTrade entryTrade = trades.stream()
                .max(Comparator.comparing(StrategyTrade::getDate))
                .orElse(null);
        if (entryTrade == null || !"BUY".equals(entryTrade.getType())) {
            log.warn("Latest trade for {} is not a BUY — skipping", fs.getSecurityName());
            return null;
        }

        BigDecimal latestClose = getLatestClose(fs.getSecurityName());
        BigDecimal entryPrice = entryTrade.getPrice();
        BigDecimal pnl = null;
        if (latestClose != null && entryPrice != null && entryPrice.compareTo(BigDecimal.ZERO) != 0) {
            pnl = FroskUtil.getPercentage(entryPrice, latestClose);
        }

        PortfolioPosition pos = new PortfolioPosition();
        pos.setPortfolio(portfolio);
        pos.setFeaturedStrategyId(fs.getId());
        pos.setSecurityName(fs.getSecurityName());
        pos.setSecurityDesc(fs.getSecurityDesc());
        pos.setStrategyName(fs.getName());
        pos.setEntryDate(entryTrade.getDate());
        pos.setEntryPrice(entryPrice);
        pos.setLatestPrice(latestClose);
        pos.setUnrealizedPnlPercent(pnl != null ? pnl.setScale(4, RoundingMode.DOWN) : null);
        pos.setOpen(fs.isOpen());
        pos.setSqn(fs.getSqn());
        pos.setExpectency(fs.getExpectency());
        pos.setProfitableTradesRatio(fs.getProfitableTradesRatio());
        return pos;
    }

    private PortfolioPosition buildIntradayPosition(FeaturedStrategy fs, Portfolio portfolio) {
        List<StrategyTrade> trades = strategyTradeRepository.findByFeaturedStrategyId(fs.getId());
        if (trades.isEmpty()) {
            log.warn("No trades found for intraday FeaturedStrategy id={}, security={}", fs.getId(), fs.getSecurityName());
            return null;
        }

        StrategyTrade entryTrade = trades.stream()
                .max(Comparator.comparing(StrategyTrade::getDate))
                .orElse(null);
        if (entryTrade == null || !"BUY".equals(entryTrade.getType())) {
            return null;
        }

        BigDecimal latestClose = getLatestIntradayClose(fs.getSecurityName());
        BigDecimal entryPrice = entryTrade.getPrice();
        BigDecimal pnl = null;
        if (latestClose != null && entryPrice != null && entryPrice.compareTo(BigDecimal.ZERO) != 0) {
            pnl = FroskUtil.getPercentage(entryPrice, latestClose);
        }

        PortfolioPosition pos = new PortfolioPosition();
        pos.setPortfolio(portfolio);
        pos.setFeaturedStrategyId(fs.getId());
        pos.setSecurityName(fs.getSecurityName());
        pos.setSecurityDesc(fs.getSecurityDesc());
        pos.setStrategyName(fs.getName());
        pos.setEntryDate(entryTrade.getDate());
        pos.setEntryPrice(entryPrice);
        pos.setLatestPrice(latestClose);
        pos.setUnrealizedPnlPercent(pnl != null ? pnl.setScale(4, RoundingMode.DOWN) : null);
        pos.setOpen(fs.isOpen());
        pos.setSqn(fs.getSqn());
        pos.setExpectency(fs.getExpectency());
        pos.setProfitableTradesRatio(fs.getProfitableTradesRatio());
        return pos;
    }

    private BigDecimal getLatestIntradayClose(String securityName) {
        try {
            Security security = securityRepository.findByName(securityName);
            if (security == null) return null;
            IntradayBar bar = intradayBarRepository.findTopBySecurityIdOrderByBarTimestampDesc(security.getId());
            return bar != null ? bar.getClose() : null;
        } catch (Exception e) {
            log.error("Could not get latest intraday close for {}: {}", securityName, e.getMessage());
            return null;
        }
    }

    private BigDecimal getLatestClose(String securityName) {
        try {
            Security security = securityRepository.findByName(securityName);
            if (security == null) return null;
            SecurityPrice sp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
            return sp != null ? sp.getClose() : null;
        } catch (Exception e) {
            log.error("Could not get latest close for {}: {}", securityName, e.getMessage());
            return null;
        }
    }

    private BigDecimal averagePnl(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.DOWN);
    }

    private PortfolioDTO toDTO(Portfolio p) {
        List<PortfolioPositionDTO> positionDTOs = p.getPositions().stream()
                .map(pos -> PortfolioPositionDTO.builder()
                        .securityName(pos.getSecurityName())
                        .securityDesc(pos.getSecurityDesc())
                        .strategyName(pos.getStrategyName() != null
                                ? pos.getStrategyName().replace("Strategy", "") : null)
                        .entryDate(pos.getEntryDate() != null
                                ? DateFormatUtils.format(pos.getEntryDate(), "yyyy-MM-dd") : null)
                        .entryPrice(pos.getEntryPrice())
                        .latestPrice(pos.getLatestPrice())
                        .unrealizedPnlPercent(pos.getUnrealizedPnlPercent())
                        .open(pos.isOpen())
                        .sqn(pos.getSqn())
                        .expectency(pos.getExpectency())
                        .profitableTradesRatio(pos.getProfitableTradesRatio())
                        .build())
                .collect(Collectors.toList());

        return PortfolioDTO.builder()
                .id(p.getId())
                .snapshotDate(DateFormatUtils.format(p.getSnapshotDate(), DATE_FORMAT))
                .openPositionCount(p.getOpenPositionCount())
                .totalPnlPercent(p.getTotalPnlPercent())
                .realizedPnlPercent(p.getRealizedPnlPercent())
                .closedTradeCount(p.getClosedTradeCount())
                .positions(positionDTOs)
                .build();
    }
}
