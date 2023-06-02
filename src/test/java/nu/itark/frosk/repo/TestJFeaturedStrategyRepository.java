package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.*;
import nu.itark.frosk.strategies.SimpleMovingMomentumStrategy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest(classes = {FroskApplication.class})
public class TestJFeaturedStrategyRepository extends BaseIntegrationTest {

	
	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;

	@Autowired
	TradesRepository tradesRepo;	

	@Autowired
	StrategyIndicatorValueRepository indicatorValuesRepo;		
	
	
	@Autowired
	SecurityRepository secRepo;
	
	@Autowired
	DataSetRepository dsRepo;

	
	@Test
	public void testFindByNameAndSecurityName() {
		log.info("count="+ featuredStrategyRepository.count());
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName("SimpleMovingMomentumStrategy", "BTRST-EUR");
		log.info("fs"+ ReflectionToStringBuilder.toString(fs));
	}
	
	@Test
	public void testFindByNameAndMore() {
	List<FeaturedStrategy> fsList = featuredStrategyRepository.findByNameOrderByTotalProfitDesc("RSI2Strategy");
	}

	@Test
	public void testFindByNameAndDataset() {
		List<FeaturedStrategy> returnList = new ArrayList<>();
		DataSet dataset = dsRepo.findByName("OSCAR");
		dataset.getSecurities().forEach(sec -> {
			FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName("RSI2Strategy", secRepo.findByName("SAND.ST").getName());
			log.info("fs="+fs);
			returnList.add(fs);
		});

//		Security security = secRepo.findByName("SAND.ST");
//		DataSet ds = dsRepo.findByName("OMX30");
//
//		logger.info("datasetRepository.findByName");
		
//		if (ds.getSecurities().contains(security)){
//			returnList.add(fs);
//		}
		
		
		
	}


	@Test
	public void testFindByName() {

		List<FeaturedStrategy> fsList = featuredStrategyRepository.findByName("RSI2Strategy");
		assertNotNull(fsList);

		fsList.forEach(fs -> log.info("sec=" + fs.getSecurityName() + ", tr size=" + fs.getTrades().size()));

	}
	
	@Test
	public void testManyToOne() {
	
		tradesRepo.deleteAllInBatch();
		featuredStrategyRepository.deleteAllInBatch();
		
		
		
		String name = "HELOO";
		String securityName = "TEST";
		BigDecimal totalProfit = new BigDecimal(12.0);
		Integer numberOfTicks = 12;
		BigDecimal averageTickProfit = new BigDecimal(12.0);
		Integer numberofTrades = 12;
		BigDecimal profitableTradesRatio = new BigDecimal(12.0);
		BigDecimal maxDD = new BigDecimal(12.0);
		BigDecimal rewardRiskRatio = new BigDecimal(12.0);
		BigDecimal totalTransactionCost = new BigDecimal(12.0);
		String period = "PER";
		Date latestTrade = new Date();
		
		
		FeaturedStrategy featuredStrategy = new FeaturedStrategy();
		
		
		FeaturedStrategy featuredStrategyREs =	featuredStrategyRepository.saveAndFlush(featuredStrategy);

		log.info("featuredStrategyREs id="+featuredStrategyREs.getId());
		
		// save trade
		StrategyTrade trades = new StrategyTrade(new Date(), "X", new BigDecimal(23),new BigDecimal(23),new BigDecimal(23));
		trades.setFeaturedStrategy(featuredStrategyREs);

		StrategyTrade trades2 = new StrategyTrade(new Date(), "Y", new BigDecimal(23),new BigDecimal(23),new BigDecimal(23));
		trades2.setFeaturedStrategy(featuredStrategyREs);

		// save indicatorvalues
		StrategyIndicatorValue indicatorValue = new StrategyIndicatorValue(new Date(), new BigDecimal(21), "EMAIndicator");
		indicatorValue.setFeaturedStrategy(featuredStrategyREs);
		
		
		tradesRepo.saveAndFlush(trades);
		tradesRepo.saveAndFlush(trades2);

		indicatorValuesRepo.saveAndFlush(indicatorValue);
		
		
	}


	@Test
	public void testFindByOpenTrade() {
		List<FeaturedStrategy> fsList = featuredStrategyRepository.findByOpenTrade("BTC-EUR");

		fsList.forEach(fs ->{
			log.info("fs.getName():"+fs.getName() + "fs.getSecurityName():"+fs.getSecurityName());
			fs.getTrades().forEach(t-> {
				log.info("**t.getType:"+t.getType());
			});
		});

	}

	@Test
	public void testFindStrategies() {
		List<TopStrategy> strategies = featuredStrategyRepository.findStrategies();
		strategies.forEach(strategy ->{
			System.out.println("strategy.getName():"+strategy.getName() + "strategy.getTotalProfit():"+strategy.getTotalProfit());
		});
	}

	@Test
	public void testFindTop() {
		FeaturedStrategy strat = featuredStrategyRepository.findTopBySecurityNameOrderByLatestTradeDesc("BTC-EUR");
		System.out.println("strategy.getName():"+strat.getName() + "strategy.getTotalProfit():"+strat.getTotalProfit());
	}

	@Test
	public void testFindTop10ByName() {
		List<FeaturedStrategy> strategies = featuredStrategyRepository.findTop10BySecurityNameOrderByLatestTradeDesc("BTC-EUR");
		strategies.forEach(strategy ->{
			System.out.println("strategy.getName():"+strategy.getName() + "strategy.getTotalProfit():"+strategy.getTotalProfit());
		});	}


	@Test
	public void testFindTop10() {
		List<FeaturedStrategy> strategies = featuredStrategyRepository.findTop10ByOrderByTotalProfitDesc();
		strategies.forEach(strategy ->{
			System.out.println("strategy.getName():"+strategy.getName() + " strategy.getTotalProfit():"+strategy.getTotalProfit());
		});	}




}
