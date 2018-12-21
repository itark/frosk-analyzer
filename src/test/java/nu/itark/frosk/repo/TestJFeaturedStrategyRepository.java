package nu.itark.frosk.repo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.ta4j.core.indicators.EMAIndicator;

import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.model.StrategyTrade;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJFeaturedStrategyRepository {
	Logger logger = Logger.getLogger(TestJFeaturedStrategyRepository.class.getName());

	
	@Autowired
	FeaturedStrategyRepository fsRepo;

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
				totalProfitVsButAndHold, period, latestTrade);		
		
//		Security security = securityRepo.findByName("SAND.ST");
		
//		Assert.assertNotNull(security);
		
//		logger.info("security="+security);
//		dataSet.getSecurities().add(security);
		
		Security security = secRepo.findByName("ABB.ST");
//		logger.info("security="+security);

		
//		Security security = secRepo.getOne(new Long(2166050));
//		
//		
//		
//		
//		featuredStrategy.setSecurity(security);
//		
//		FeaturedStrategy featuredStrategyREs =	fsRepo.save(featuredStrategy);
		
		
	}	
	
	

	@Test
	public void testFindByNameAndSecurityName() {
		logger.info("count="+fsRepo.count());	
		
//		List<FeaturedStrategy> fsList = fsRepo.findByNameAndSecurityName("RSI2Strategy", "SAND.ST");
		FeaturedStrategy fs = fsRepo.findByNameAndSecurityName("RSI2Strategy", "SAND.ST");
		
		
//		fsList.forEach(fs -> logger.info("sec="+fs.getSecurityName()+", ld="+fs.getLatestTrade()));

	}
	
	@Test
	public void testFindByNameAndMore() {
	List<FeaturedStrategy> fsList = fsRepo.findByNameOrderByTotalProfitDesc("RSI2Strategy");	
	}
//	
	@Test
	public void testFindByNameAndDataset() {
		List<FeaturedStrategy> returnList = new ArrayList<>();
		DataSet dataset = dsRepo.findByName("OSCAR");
		dataset.getSecurities().forEach(sec -> {
			FeaturedStrategy fs = fsRepo.findByNameAndSecurityName("RSI2Strategy", secRepo.findByName("SAND.ST").getName());
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
	
	List<FeaturedStrategy> fsList = fsRepo.findByName("RSI2Strategy");
	Assert.assertNotNull(fsList);

	fsList.forEach(fs -> logger.info("sec="+fs.getSecurityName()+", tr size="+fs.getTrades().size()));
	
	
//	logger.info("fs size"+fs.size());
	
	
	}
	
	@Test
	public void testManyToOne() {
	
		tradesRepo.deleteAllInBatch();
		fsRepo.deleteAllInBatch();
		
		
		
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
				totalProfitVsButAndHold, period, latestTrade);		
		
		
		FeaturedStrategy featuredStrategyREs =	fsRepo.saveAndFlush(featuredStrategy);

		logger.info("featuredStrategyREs id="+featuredStrategyREs.getId());
		
		// save trade
		StrategyTrade trades = new StrategyTrade(new Date(), "X", new BigDecimal(23));
		trades.setFeaturedStrategy(featuredStrategyREs);

		StrategyTrade trades2 = new StrategyTrade(new Date(), "Y", new BigDecimal(23));
		trades2.setFeaturedStrategy(featuredStrategyREs);

		// save indicatorvalues
		StrategyIndicatorValue indicatorValue = new StrategyIndicatorValue(new Date(), new BigDecimal(21), "EMAIndicator");
		indicatorValue.setFeaturedStrategy(featuredStrategyREs);
		
		
		tradesRepo.saveAndFlush(trades);
		tradesRepo.saveAndFlush(trades2);

		indicatorValuesRepo.saveAndFlush(indicatorValue);
		
		
	}
	
	
}
