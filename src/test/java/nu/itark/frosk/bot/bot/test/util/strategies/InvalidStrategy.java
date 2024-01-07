package nu.itark.frosk.bot.bot.test.util.strategies;

import nu.itark.frosk.bot.bot.strategy.CassandreStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Invalid strategy (used for tests).
 */
@CassandreStrategy(strategyName = "Invalid strategy")
@ConditionalOnProperty(
		value = InvalidStrategy.PARAMETER_INVALID_STRATEGY_ENABLED,
		havingValue = "true")
public class InvalidStrategy {

	/** Invalid strategy enabled parameter. */
	public static final String PARAMETER_INVALID_STRATEGY_ENABLED = "invalidStrategy.enabled";

}
