package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.RecommendationTrend;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyPerformance;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@Slf4j
public class TestJRecommendationTrendRepository extends BaseIntegrationTest {

    @Autowired
    SecurityRepository securityRepository;


    @Autowired
    RecommendationTrendRepository recommendationTrendRepository;

    @Test
    public final void testFindBySecurityOrderByPeriod() {
        String securityName = "ABB.ST"; //ABB.ST, ESSITY-B.ST, MER.ST, BICO-B.ST, AIK-B.ST, AGTIRA-B.ST, KAV.ST, BRIX, OODA.ST
        final Security security = securityRepository.findByName(securityName);
       // log.info("security:{}",security);

        List<RecommendationTrend> bySecurityOrderByPeriod = recommendationTrendRepository.findBySecurityOrderByPeriod(security);
        log.info("bySecurityOrderByPeriod:{}",bySecurityOrderByPeriod);
    }

    @Test
    public final void testRecommendations() {
        String securityName = "ABB.ST"; //ABB.ST, ESSITY-B.ST, MER.ST, BICO-B.ST, AIK-B.ST, AGTIRA-B.ST, KAV.ST, BRIX, OODA.ST
        final Optional<RecommendationTrend> latestCurrentTrendBySecurityName = recommendationTrendRepository.findLatestCurrentTrendBySecurityName(securityName);
        log.info("latestCurrentTrendBySecurityName:{}",latestCurrentTrendBySecurityName);

        latestCurrentTrendBySecurityName.get().getBearishPercentage();
        latestCurrentTrendBySecurityName.get().getBullishPercentage();
        latestCurrentTrendBySecurityName.get().getNeutralPercentage();
        latestCurrentTrendBySecurityName.get().getRecommendationScore();
        latestCurrentTrendBySecurityName.get().getTotalRecommendations();
        log.info("RecommendationScore:{}",latestCurrentTrendBySecurityName.get().getRecommendationScore());

    }


}
