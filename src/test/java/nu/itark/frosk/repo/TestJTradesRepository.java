package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.StrategyTrade;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.Order;

import java.util.List;
import java.util.logging.Logger;

@SpringBootTest
@Slf4j
public class TestJTradesRepository {

	@Autowired
	TradesRepository tradesRepository;
	
	@Test
	public final void testOpenTrades() {
		List<StrategyTrade> strategyTrade = tradesRepository.findTopByType(Order.OrderType.BUY.name());
		for (StrategyTrade trade : strategyTrade) {
			log.info("trade:"+ ReflectionToStringBuilder.toString(trade));
		}
	}


}
