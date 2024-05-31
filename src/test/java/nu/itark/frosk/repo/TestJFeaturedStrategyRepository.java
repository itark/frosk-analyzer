package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.*;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest(classes = {FroskApplication.class})
public class TestJFeaturedStrategyRepository extends BaseIntegrationTest {

	
	@Autowired
	FeaturedStrategyRepository featuredStrategyRepository;

	@Autowired
    StrategyTradeRepository tradesRepo;

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

		fsList.forEach(fs -> log.info("sec=" + fs.getSecurityName() + ", tr size=" + fs.getStrategyTrades().size()));

	}
	
	@Test
	public void testFindStrategies() {
		List<TopStrategy> strategies = featuredStrategyRepository.findBestPerformingStrategies();
		strategies.forEach(strategy ->{
			System.out.println("strategy.getName():"+strategy.getName() + "strategy.getTotalProfit():"+strategy.getTotalProfit());
		});
	}

	@Test
	public void testFindTopBySecurityNameOrderByLatestTradeDesc() {
		FeaturedStrategy strat = featuredStrategyRepository.findTopBySecurityNameOrderByLatestTradeDesc("BTC-EUR");
		System.out.println("strategy.getName():"+strat.getName() + "strategy.getTotalProfit():"+strat.getTotalProfit());
	}

	@Test
	public void testFindTop() {
		BigDecimal profitableTradesRatio = new BigDecimal(0.1);
		Integer nrOfTrades = 4;
		BigDecimal sqn = new BigDecimal(1.7);
		BigDecimal expectency = new BigDecimal(0.5);

		List<FeaturedStrategy> strategies = featuredStrategyRepository.findTopStrategies(profitableTradesRatio, nrOfTrades, sqn, expectency, Boolean.FALSE );
		strategies.forEach(strategy ->{
			System.out.println("name:"+strategy.getName() + ", totalProfit:"+strategy.getTotalProfit() + ", securityName:"+ strategy.getSecurityName());
		});	}


}
