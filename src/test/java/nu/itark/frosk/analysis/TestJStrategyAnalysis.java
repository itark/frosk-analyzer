package nu.itark.frosk.analysis;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyPerformance;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyPerformanceRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.RSI2Strategy;
import nu.itark.frosk.strategies.SimpleMovingMomentumStrategy;
import nu.itark.frosk.util.DateTimeManager;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.BarSeries;

import java.util.List;
import java.util.logging.Logger;

@SpringBootTest(classes = {FroskApplication.class})
public class TestJStrategyAnalysis extends BaseIntegrationTest {
	Logger logger = Logger.getLogger(TestJStrategyAnalysis.class.getName());
	
	@Autowired
	StrategyAnalysis strategyAnalysis;

	@Autowired
	StrategyPerformanceRepository strategyPerformanceRepository;

	@Autowired
	private BarSeriesService barSeriesService;

	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;
	

	@Test
	public final void runSMM() {
		Long sec_id = barSeriesService.getSecurityId("MASK-EUR"); //"BTRST-EUR","BTC-EUR"
		strategyAnalysis.run(SimpleMovingMomentumStrategy.class.getSimpleName(), sec_id);
		//Verify
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(SimpleMovingMomentumStrategy.class.getSimpleName(), "BTC-EUR");

		fs.getTrades().forEach(t-> {
			System.out.println(ReflectionToStringBuilder.toString(t));
		});

	}	
	
	@Test
	public void runRSI2() {
		Long sec_id = barSeriesService.getSecurityId("BTC-EUR");
		strategyAnalysis.run(RSI2Strategy.class.getSimpleName(), sec_id);
	}

	@Test
	public void runAll() {
		logger.info("All");
		strategyAnalysis.run(null, null);
	}


	@Test
	public final void runSetBestStrategy() {
		BarSeries barSeries = barSeriesService.getDataSet("BTC-EUR", false);

		strategyPerformanceRepository.findAll().forEach(sp-> {
			logger.info("test:sp="+ReflectionToStringBuilder.toString(sp));
		});

		strategyPerformanceRepository.findBySecurityNameAndDate(barSeries.getName(), DateTimeManager.get(barSeries.getLastBar().getEndTime())).forEach(sp-> {
			logger.info("exist:sp="+ReflectionToStringBuilder.toString(sp));
		});


		//strategyAnalysis.setBestStrategy(barSeries);
	}


}
