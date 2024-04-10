package nu.itark.frosk.analysis;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyPerformanceRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.strategies.*;
import nu.itark.frosk.util.DateTimeManager;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.BarSeries;

import java.util.Comparator;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
	public void run1() {
		Long sec_id = barSeriesService.getSecurityId("SOL-EUR");
		strategyAnalysis.run(ADXStrategy.class.getSimpleName(), sec_id);
		//Verify
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(ADXStrategy.class.getSimpleName(), "BTC-EUR");
		fs.getStrategyTrades().stream()
				.sorted(Comparator.comparing(StrategyTrade::getDate))
				//.peek(t-> System.out.println(ReflectionToStringBuilder.toString(t, ToStringStyle.MULTI_LINE_STYLE)))
				.peek(t-> {
					if (t.getType().equals("BUY")) {
						System.out.println("Entered on " + t.getPrice());
					} else {
						System.out.println("Exit on " + t.getPrice() + ", pnl="+t.getPnl());
					}
				})
				.collect(Collectors.toSet());

		strategyAnalysis.runBot(ADXStrategy.class.getSimpleName(), sec_id);
	}

	@Test
	public final void run2() {
		Long sec_id = barSeriesService.getSecurityId("BTC-EUR"); //"BTRST-EUR","BTC-EUR"
		strategyAnalysis.run(SimpleMovingMomentumStrategy.class.getSimpleName(), sec_id);

		//Verify
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(SimpleMovingMomentumStrategy.class.getSimpleName(), "BTC-EUR");

		fs.getStrategyTrades().forEach(t-> {
			System.out.println(ReflectionToStringBuilder.toString(t));
		});

	}

	@Test
	public final void run3() {
		String strategyName = "EMATenTwentyStrategy";
		String securityName = "SOL-EUR";
		Long sec_id = barSeriesService.getSecurityId(securityName); //"BTRST-EUR","BTC-EUR","SOL-EUR"
		strategyAnalysis.run(strategyName, sec_id);

		//Verify
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, securityName);
		fs.getStrategyTrades().stream()
				.sorted(Comparator.comparing(StrategyTrade::getDate))
				.peek(t-> System.out.println(ReflectionToStringBuilder.toString(t, ToStringStyle.MULTI_LINE_STYLE)))
/*
				.peek(t-> {
					if (t.getType().equals("BUY")) {
						System.out.println("Entered on " + t.getPrice());
					} else {
						System.out.println("Exit on " + t.getPrice() + ", pnl="+t.getPnl());
					}
				})
*/
				.collect(Collectors.toSet());

	}

	@Test
	public void run4() {
		String strategyName = "EMATenTwentyStrategy";
		String securityName = "ALCX-USDT";
		Long sec_id = barSeriesService.getSecurityId(securityName);
		strategyAnalysis.run(strategyName, sec_id);

		//Verify
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, securityName);
		fs.getStrategyTrades().stream()
				.sorted(Comparator.comparing(StrategyTrade::getDate))
				.peek(t-> System.out.println(ReflectionToStringBuilder.toString(t, ToStringStyle.MULTI_LINE_STYLE)))
				.collect(Collectors.toSet());
	}

	@Test
	public void run5() {
		String strategyName = "HaramiStrategy";
		String securityName = "FIL-EUR";
		Long sec_id = barSeriesService.getSecurityId(securityName);
		strategyAnalysis.run(strategyName, sec_id);
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, securityName);
		System.out.println("fs.name:"+fs.getSecurityName()+",fs.totalGrossReturn:"+fs.getTotalGrossReturn()+",fs.getTotalProfit:"+fs.getTotalProfit());
		fs.getStrategyTrades().stream()
				.sorted(Comparator.comparing(StrategyTrade::getDate))
				//.peek(t-> System.out.println(ReflectionToStringBuilder.toString(t, ToStringStyle.MULTI_LINE_STYLE)))
				.peek(t-> System.out.println("date:"+t.getDate().toGMTString()+",type:"+t.getType()+",price:"+t.getPrice()+",amount:"+t.getAmount()+",pnl-%:"+t.getPnl()+",return-EUR:"+t.getGrossProfit()))
				.collect(Collectors.toSet());

	}


	@Test
	public void runCD() {
		String strategyName = "ConvergenceDivergenceStrategy";
		String securityName = "SHPING-EUR";
		Long sec_id = barSeriesService.getSecurityId(securityName);
		strategyAnalysis.run(strategyName, sec_id);
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName(strategyName, securityName);
		fs.getStrategyTrades().stream()
				.sorted(Comparator.comparing(StrategyTrade::getDate))
				.peek(t-> System.out.println(ReflectionToStringBuilder.toString(t, ToStringStyle.MULTI_LINE_STYLE)))
				.collect(Collectors.toSet());

	}

	@Test
	public void runAll() {
		logger.info("All");
		strategyAnalysis.run(null, null);
	}


	@Test
	public final void runSetBestStrategy() {
		BarSeries barSeries = barSeriesService.getDataSet("BAT-EUR", false, false);

/*
		strategyPerformanceRepository.findAll().forEach(sp-> {
			logger.info("test:sp="+ReflectionToStringBuilder.toString(sp));
		});
*/

		strategyPerformanceRepository.findBySecurityNameAndDate(barSeries.getName(), DateTimeManager.get(barSeries.getLastBar().getEndTime())).forEach(sp-> {
			logger.info("exist:sp="+ReflectionToStringBuilder.toString(sp));
		});

		strategyAnalysis.setBestStrategy(barSeries);
	}

	@Test
	public void testTopPerf() {
		String bestStrategy = "SimpleMovingMomentumStrategy";  //SimpleMovingMomentumStrategy
		strategyPerformanceRepository.findByBestStrategyOrderByTotalProfitLossDesc(bestStrategy).forEach(sp-> {
			System.out.println("sp:"+ReflectionToStringBuilder.toString(sp));
		});
	}

	@Test
	public void runBot() {
		String strategy = "SimpleMovingMomentumStrategy";
		String securityName = "BTC-EUR";
		Long sec_id = barSeriesService.getSecurityId(securityName);
		strategyAnalysis.runBot(strategy,sec_id );
	}


}
