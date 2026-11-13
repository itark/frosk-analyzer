package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.BundleScreeningDTO;
import nu.itark.frosk.analysis.BundleVoteDTO;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S7 — Bundle-screening / ensemble stock-picking (a "jury system" over the existing frosk
 * strategies). See {@code docs/research/omx-equity-strategies-research.md}.
 *
 * <p>Each security gets one vote per independent strategy that currently holds an open position
 * in it. A security that several strategies with different logic (momentum, trend, factor) agree
 * on has a lower chance of being a false positive. Buy only candidates with {@code minVotes}+
 * agreement; the HedgeIndex acts as a veto (risk-off → no buys regardless of votes).
 *
 * <p>Read-only aggregation of existing {@link FeaturedStrategy} state — it places no trades and
 * is deliberately NOT registered as a live portfolio strategy.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BundleScreeningService {

    /** The three independent alpha sources that make up the jury. */
    public static final List<String> BUNDLE_STRATEGIES = List.of(
            "ShortTermMomentumLongTermStrengthStrategy", // momentum
            "HighLanderStrategy",                        // trend
            "SwedishLongTermMomentumStrategy");          // factor / quality (Månadsportföljen)

    private final FeaturedStrategyRepository featuredStrategyRepository;
    private final HedgeIndexService hedgeIndexService;

    /** Minimum agreeing strategies (votes) for a buy candidate. 2-of-3 by default. */
    @Value("${frosk.bundle.min.votes:2}")
    private int minVotes;

    public BundleScreeningDTO screen() {
        ZonedDateTime now = ZonedDateTime.now();
        int score = hedgeIndexService.getScore(now);
        boolean vetoed = hedgeIndexService.risk(now);

        List<FeaturedStrategy> open =
                featuredStrategyRepository.findByNameInAndOpenOrderBySqnDesc(BUNDLE_STRATEGIES, Boolean.TRUE);

        // Group open positions by security; distinct strategy names per security = vote count.
        Map<String, List<FeaturedStrategy>> bySecurity = open.stream()
                .collect(Collectors.groupingBy(FeaturedStrategy::getSecurityName));

        List<BundleVoteDTO> candidates = new ArrayList<>();
        for (Map.Entry<String, List<FeaturedStrategy>> e : bySecurity.entrySet()) {
            List<String> strategies = e.getValue().stream()
                    .map(FeaturedStrategy::getName)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            if (strategies.size() < minVotes) {
                continue;
            }
            candidates.add(BundleVoteDTO.builder()
                    .securityName(e.getKey())
                    .securityDesc(e.getValue().get(0).getSecurityDesc())
                    .votes(strategies.size())
                    .strategies(strategies)
                    .build());
        }
        candidates.sort(Comparator.comparingInt(BundleVoteDTO::getVotes).reversed()
                .thenComparing(BundleVoteDTO::getSecurityName));

        log.info("Bundle-screening: {} candidates with >= {} votes (hedge score {}, vetoed={})",
                candidates.size(), minVotes, score, vetoed);

        return BundleScreeningDTO.builder()
                .date(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .hedgeIndexScore(score)
                .regime(HedgeIndexService.regimeLabel(score))
                .vetoed(vetoed)
                .minVotes(minVotes)
                .bundleStrategies(BUNDLE_STRATEGIES)
                .candidates(candidates)
                .build();
    }
}
