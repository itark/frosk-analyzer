package nu.itark.frosk.repo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.Security;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJFeaturedStrategyRepository {
	Logger logger = Logger.getLogger(TestJFeaturedStrategyRepository.class.getName());

	
	@Autowired
	FeaturedStrategyRepository fsRepo;
	
	@Autowired
	SecurityRepository secRepo;

	
	@Test
	public final void testSaveFeaturedStrategy() {

//		fsRepo.deleteAllInBatch();

		String name = "HELOO";
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
		
		
		FeaturedStrategy featuredStrategy = new FeaturedStrategy(name, totalProfit, numberOfTicks, averageTickProfit,
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
	public void testFindBySecurity() {
		logger.info("count="+fsRepo.count());	
		
		List<FeaturedStrategy> fsList = fsRepo.findBySecurityName("SAND.ST");
		
		logger.info("fsList="+fsList.size());
		
//		fsList.forEach(fs -> logger.info("sec="+fs.getSecurityName()+", ld="+fs.getLatestTrade()));

	}
	
	@Test
	public void testFindByNameAndSecurity() {
	
//	FeaturedStrategy fs = fsRepo.findByNameAndSecurityName("RSI2Strategy", "ABB.ST");
//	Assert.assertNotNull(fs);
//	logger.info("fs"+fs);
	
	
	}
	
	@Test
	public void testFindByNameAndDataset() {
	
	List<FeaturedStrategy> fs = fsRepo.findByName("RSI2Strategy");
	Assert.assertNotNull(fs);
	logger.info("fs"+fs);
	
	
	}	
	
	
}
