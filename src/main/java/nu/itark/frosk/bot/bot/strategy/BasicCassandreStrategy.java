package nu.itark.frosk.bot.bot.strategy;

import nu.itark.frosk.bot.bot.strategy.internal.CassandreStrategy;

/**
 * BasicCassandreStrategy - User inherits this class this one to make a basic strategy.
 * <p>
 * These are the classes used by Cassandre to manage a position.
 * - CassandreStrategyInterface list the methods a strategy type must implement to be able to interact with the Cassandre framework.
 * - CassandreStrategyConfiguration contains the configuration of the strategy.
 * - CassandreStrategyDependencies contains all the dependencies required by a strategy and provided by the Cassandre framework.
 * - CassandreStrategyImplementation is the default implementation of CassandreStrategyInterface, this code manages the interaction between Cassandre framework and a strategy.
 * - CassandreStrategy (class) is the class that every strategy used by user ({@link BasicCassandreStrategy} must extend. It contains methods to access data and manage orders, trades, positions.
 * There are the classes used by the developer.
 * - CassandreStrategy (interface) is the annotation allowing you Cassandre to recognize a user strategy.
 * - BasicCassandreStrategy - User inherits this class this one to make a basic strategy.
 */
@SuppressWarnings("unused")
public abstract class BasicCassandreStrategy extends CassandreStrategy {

}
