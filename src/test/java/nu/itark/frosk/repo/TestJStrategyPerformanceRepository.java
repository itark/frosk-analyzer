package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.StrategyPerformance;
import nu.itark.frosk.repo.StrategyPerformanceRepository;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@Slf4j
@SpringBootTest(classes = {FroskApplication.class})
public class TestJStrategyPerformanceRepository extends BaseIntegrationTest {

    @Autowired
    StrategyPerformanceRepository strategyPerformanceRepository;

    @Test
    public final void testX() {
        List<StrategyPerformance> top3List = strategyPerformanceRepository.findTop3ByOrderByTotalProfitLossDesc();
        for (StrategyPerformance sp : top3List) {
            log.info("trade:" + ReflectionToStringBuilder.toString(sp));
        }
    }

}
