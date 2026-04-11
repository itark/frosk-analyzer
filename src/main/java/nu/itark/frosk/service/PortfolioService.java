package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.PortfolioDTO;
import nu.itark.frosk.analysis.PortfolioPositionDTO;
import nu.itark.frosk.model.*;
import nu.itark.frosk.repo.*;
import nu.itark.frosk.util.FroskUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final List<String> PORTFOLIO_STRATEGIES = List.of(
            "ShortTermMomentumLongTermStrengthStrategy",
            "HighLanderStrategy"
    );

    final FeaturedStrategyRepository featuredStrategyRepository;
    final StrategyTradeRepository strategyTradeRepository;
    final SecurityRepository securityRepository;
    final SecurityPriceRepository securityPriceRepository;
    final PortfolioRepository portfolioRepository;
    final PortfolioPositionRepository portfolioPositionRepository;

    /**
     * Builds a new Portfolio snapshot from all currently open FeaturedStrategy positions.
     * Each call creates a new persisted snapshot (history is preserved).
     */
    @Transactional
    public Portfolio build() {
        log.info("Building portfolio snapshot...");

        List<FeaturedStrategy> openStrategies = featuredStrategyRepository.findByOpen(true).stream()
                .filter(fs -> PORTFOLIO_STRATEGIES.contains(fs.getName()))
                .collect(Collectors.toList());
        log.info("Found {} open positions for strategies {}", openStrategies.size(), PORTFOLIO_STRATEGIES);

        Portfolio portfolio = new Portfolio();
        portfolio.setSnapshotDate(new Date());

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
        log.info("Portfolio snapshot saved: id={}, openPositions={}, avgPnl={}",
                saved.getId(), saved.getOpenPositionCount(), saved.getTotalPnlPercent());
        return saved;
    }

    /**
     * Returns the most recently built portfolio snapshot as a DTO.
     * Returns an empty DTO if no portfolio has been built yet.
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getCurrent() {
        Optional<Portfolio> latest = portfolioRepository.findTopByOrderBySnapshotDateDesc();
        if (latest.isEmpty()) {
            log.warn("No portfolio snapshot found. Run build() first.");
            return PortfolioDTO.builder()
                    .snapshotDate("none")
                    .openPositionCount(0)
                    .positions(Collections.emptyList())
                    .build();
        }
        return toDTO(latest.get());
    }

    /**
     * Returns all historical portfolio snapshots ordered newest first.
     * Positions are NOT loaded (header data only) to keep the list lightweight.
     */
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getHistory() {
        return portfolioRepository.findAll().stream()
                .sorted(Comparator.comparing(Portfolio::getSnapshotDate).reversed())
                .map(p -> PortfolioDTO.builder()
                        .id(p.getId())
                        .snapshotDate(DateFormatUtils.format(p.getSnapshotDate(), DATE_FORMAT))
                        .openPositionCount(p.getOpenPositionCount())
                        .totalPnlPercent(p.getTotalPnlPercent())
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
                .positions(positionDTOs)
                .build();
    }
}
