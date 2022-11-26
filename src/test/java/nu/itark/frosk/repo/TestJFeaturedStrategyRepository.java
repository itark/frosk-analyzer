package nu.itark.frosk.repo;

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

@SpringBootTest
public class TestJFeaturedStrategyRepository {
	Logger logger = Logger.getLogger(TestJFeaturedStrategyRepository.class.getName());

	
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
	public final void testSaveFeaturedStrategy() {

//		fsRepo.deleteAllInBatch();

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
		BigDecimal buyAndHold = new BigDecimal(12.0);
		BigDecimal totalProfitVsButAndHold = new BigDecimal(25.0);
		String period = "PER";
		Date latestTrade = new Date();
		
		
		FeaturedStrategy featuredStrategy = new FeaturedStrategy(name, securityName, totalProfit, numberOfTicks, averageTickProfit,
				numberofTrades, profitableTradesRatio, maxDD, rewardRiskRatio, totalTransactionCost, buyAndHold,
				totalProfitVsButAndHold, period, latestTrade, false);
		
	}
	
	@Test
	public void testFindByNameAndSecurityName() {
		logger.info("count="+ featuredStrategyRepository.count());
		FeaturedStrategy fs = featuredStrategyRepository.findByNameAndSecurityName("SimpleMovingMomentumStrategy", "BTRST-EUR");
		logger.info("fs"+ ReflectionToStringBuilder.toString(fs));
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
			logger.info("fs="+fs);
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

		fsList.forEach(fs -> logger.info("sec=" + fs.getSecurityName() + ", tr size=" + fs.getTrades().size()));

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
		BigDecimal buyAndHold = new BigDecimal(12.0);
		BigDecimal totalProfitVsButAndHold = new BigDecimal(25.0);
		String period = "PER";
		Date latestTrade = new Date();
		
		
		FeaturedStrategy featuredStrategy = new FeaturedStrategy(name, securityName, totalProfit, numberOfTicks, averageTickProfit,
				numberofTrades, profitableTradesRatio, maxDD, rewardRiskRatio, totalTransactionCost, buyAndHold,
				totalProfitVsButAndHold, period, latestTrade, false);
		
		
		FeaturedStrategy featuredStrategyREs =	featuredStrategyRepository.saveAndFlush(featuredStrategy);

		logger.info("featuredStrategyREs id="+featuredStrategyREs.getId());
		
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
			System.out.println("fs.getName():"+fs.getName() + "fs.getSecurityName():"+fs.getSecurityName());
			fs.getTrades().forEach(t-> {
				System.out.println("**t.getType:"+t.getType());
			});
		});

	}

}
