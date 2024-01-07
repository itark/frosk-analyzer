package nu.itark.frosk.strategies.cassandre.ta4j;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.strategies.cassandre.SimpleStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tech.cassandre.trading.bot.configuration.DatabaseAutoConfiguration;
import tech.cassandre.trading.bot.repository.OrderRepository;
import tech.cassandre.trading.bot.test.mock.TickerFluxMock;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Simple strategy test.
 */
@SpringBootTest
@Import({TickerFluxMock.class, DatabaseAutoConfiguration.class})
//@Import({TickerFluxMock.class})
//@Import({TickerFluxMock.class})
@DisplayName("Simple strategy test")
public class SimpleStrategyTest { //extends BaseIntegrationTest {

	@Autowired
	private TickerFluxMock tickerFluxMock;

	/** Dumb strategy. */
	@Autowired
	private SimpleStrategy strategy;

	/**
	 * Check data reception.
	 */
	@Test
	@DisplayName("Check strategy behavioir")
	public void checkStrategy() {
		await().forever().until(() -> tickerFluxMock.isFluxDone());

		// Waiting to see if the strategy received the accounts update.
		await().untilAsserted(() -> assertEquals(strategy.getAccounts().size(), 3));
	}

}
