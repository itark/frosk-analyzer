package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.StrategyTrade;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.Trade;


import java.util.List;

@SpringBootTest
@Slf4j
public class TestJTradesRepository extends BaseIntegrationTest {

	@Autowired
    StrategyTradeRepository tradesRepository;
	
	@Test
	public final void testOpenTrades() {
		List<StrategyTrade> strategyTrade = tradesRepository.findTopByType(Trade.TradeType.BUY.name());
		for (StrategyTrade trade : strategyTrade) {
			log.info("trade:"+ ReflectionToStringBuilder.toString(trade));
		}
	}


	@Test
	public final void testTrades() {
		List<StrategyTrade> strategyTrade = tradesRepository.findTopByType(Trade.TradeType.BUY.name());
		for (StrategyTrade trade : strategyTrade) {
			log.info("trade:"+ ReflectionToStringBuilder.toString(trade));
		}
	}


}
