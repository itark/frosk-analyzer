package nu.itark.frosk.bot.bot.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nu.itark.frosk.bot.bot.batch.PositionFlux;
import nu.itark.frosk.bot.bot.domain.Position;
import nu.itark.frosk.bot.bot.dto.market.TickerDTO;
import nu.itark.frosk.bot.bot.dto.position.PositionCreationResultDTO;
import nu.itark.frosk.bot.bot.dto.position.PositionDTO;
import nu.itark.frosk.bot.bot.dto.position.PositionRulesDTO;
import nu.itark.frosk.bot.bot.dto.position.PositionTypeDTO;
import nu.itark.frosk.bot.bot.dto.strategy.StrategyDTO;
import nu.itark.frosk.bot.bot.dto.trade.OrderCreationResultDTO;
import nu.itark.frosk.bot.bot.dto.trade.TradeDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyAmountDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.dto.util.GainDTO;
import nu.itark.frosk.bot.bot.repository.PositionRepository;
import nu.itark.frosk.bot.bot.repository.StrategyRepository;
import nu.itark.frosk.bot.bot.strategy.internal.CassandreStrategy;
import nu.itark.frosk.bot.bot.strategy.internal.CassandreStrategyInterface;
import nu.itark.frosk.bot.bot.util.base.service.BaseService;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.service.BarSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.Strategy;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.HALF_UP;
import static nu.itark.frosk.bot.bot.dto.position.PositionStatusDTO.*;
import static nu.itark.frosk.bot.bot.dto.position.PositionTypeDTO.LONG;
import static nu.itark.frosk.bot.bot.dto.position.PositionTypeDTO.SHORT;
import static nu.itark.frosk.bot.bot.util.math.MathConstants.BIGINTEGER_SCALE;
import static nu.itark.frosk.bot.bot.util.math.MathConstants.ONE_HUNDRED_BIG_DECIMAL;

/**
 * Position service - Implementation of {@link PositionService}.
 */
@RequiredArgsConstructor
public class PositionServiceCassandreImplementation extends BaseService implements PositionService {

    /** Minimum amount for creating a position. */
    private static final BigDecimal MINIMUM_AMOUNT_FOR_POSITION = new BigDecimal("0.000000001");

    /** Position repository. */
    private final PositionRepository positionRepository;

    /** Trade service. */
    private final TradeService tradeService;

    /** Position flux. */
    private final PositionFlux positionFlux;

    @Autowired
    private FeaturedStrategyRepository featuredStrategyRepository;

    @Autowired
    private StrategyRepository strategyRepository;

    @Autowired
    private BarSeriesService barSeriesService;

    @Override
    public final PositionCreationResultDTO createLongPosition(@NonNull final CassandreStrategy strategy,
                                                              @NonNull final CurrencyPairDTO currencyPair,
                                                              @NonNull final BigDecimal amount,
                                                              @NonNull final PositionRulesDTO rules) {
        return createPosition(strategy, LONG, currencyPair, amount, rules);
    }

    @Override
    public final PositionCreationResultDTO createLongPosition(@NonNull final Strategy strategy,
                                                              @NonNull final CurrencyPairDTO currencyPair,
                                                              @NonNull final BigDecimal amount,
                                                              @NonNull final BigDecimal limitPrice,
                                                              final PositionRulesDTO rules) {

        final FeaturedStrategy featuredStrategy = featuredStrategyRepository.findByNameAndSecurityName(strategy.getName(), currencyPair.toString());
        return createPosition(featuredStrategy, LONG, currencyPair, amount, limitPrice);
    }


    @Override
    public final PositionCreationResultDTO createShortPosition(@NonNull final CassandreStrategy strategy,
                                                               @NonNull final CurrencyPairDTO currencyPair,
                                                               @NonNull final BigDecimal amount,
                                                               @NonNull final PositionRulesDTO rules) {
        return createPosition(strategy, SHORT, currencyPair, amount, rules);
    }

    /**
     * Creates a position.
     *
     * @param strategy     strategy
     * @param type         long or short
     * @param currencyPair currency pair
     * @param amount       amount
     * @param rules        rules
     * @return position creation result
     */
    private PositionCreationResultDTO createPosition(final CassandreStrategy strategy,
                                                     final PositionTypeDTO type,
                                                     final CurrencyPairDTO currencyPair,
                                                     final BigDecimal amount,
                                                     final PositionRulesDTO rules) {
        logger.debug("Creating a {} position for {} on {} with the rules: {}",
                type.toString().toLowerCase(Locale.ROOT),
                amount,
                currencyPair,
                rules);

        // It's forbidden to create a position with a too small amount.
        if (MINIMUM_AMOUNT_FOR_POSITION.compareTo(amount) > 0) {
            logger.error("Impossible to create a position for such a small amount ({})", amount);
            return new PositionCreationResultDTO("Impossible to create a position for such a small amount: " + amount, null);
        }

        // =============================================================================================================
        // Creates the order on the exchange.
        final OrderCreationResultDTO orderCreationResult;
        if (type == LONG) {
            // Long position - we buy.
            orderCreationResult = tradeService.createBuyMarketOrder(strategy, currencyPair, amount);
        } else {
            // Short position - we sell.
            orderCreationResult = tradeService.createSellMarketOrder(strategy, currencyPair, amount);
        }

        // If it works, we create the position.
        if (orderCreationResult.isSuccessful()) {
            // =========================================================================================================
            // Creates the position in database.
            Position position = new Position();
            position.setStrategy(STRATEGY_MAPPER.mapToStrategy(strategy.getConfiguration().getStrategyDTO()));
            position = positionRepository.save(position);

            // =========================================================================================================
            // Creates the position dto.
            PositionDTO p = new PositionDTO(position.getUid(), type, strategy.getConfiguration().getStrategyDTO(), currencyPair, amount, orderCreationResult.getOrder(), rules);
            positionRepository.save(POSITION_MAPPER.mapToPosition(p));
            logger.debug("Position {} opened with order {}",
                    p.getPositionId(),
                    orderCreationResult.getOrder().getOrderId());

            // =========================================================================================================
            // Emit the position, creates and return the position creation result.
            positionFlux.emitValue(p);
            return new PositionCreationResultDTO(p);
        } else {
            logger.error("Position creation failure: {}", orderCreationResult.getErrorMessage());
            return new PositionCreationResultDTO(orderCreationResult.getErrorMessage(), orderCreationResult.getException());
        }
    }

    private PositionCreationResultDTO createPosition(final FeaturedStrategy strategy,
                                                     final PositionTypeDTO type,
                                                     final CurrencyPairDTO currencyPair,
                                                     final BigDecimal amount,
                                                     final BigDecimal limitPrice) {
        logger.debug("Creating a {} position for {} on {} with the rules: {}",
                type.toString().toLowerCase(Locale.ROOT),
                amount,
                currencyPair);

        // It's forbidden to create a position with a too small amount.
        if (MINIMUM_AMOUNT_FOR_POSITION.compareTo(amount) > 0) {
            logger.error("Impossible to create a position for such a small amount ({})", amount);
            return new PositionCreationResultDTO("Impossible to create a position for such a small amount: " + amount, null);
        }

        // =============================================================================================================
        // Creates the order on the exchange.
        final OrderCreationResultDTO orderCreationResult;
        if (type == LONG) {
            // Long position - we buy.
            orderCreationResult = tradeService.createBuyMarketOrder(strategy, currencyPair, amount, limitPrice);
        } else {
            // Short position - we sell.
            orderCreationResult = tradeService.createSellMarketOrder(strategy, currencyPair, amount, limitPrice);
        }

        // If it works, we create the position.
        if (orderCreationResult.isSuccessful()) {
            // =========================================================================================================
            // Creates the position in database.
            Position position = new Position();

            // If the strategy is NOT in database.
            nu.itark.frosk.bot.bot.domain.Strategy newStrategy = new nu.itark.frosk.bot.bot.domain.Strategy();
            newStrategy.setStrategyId(String.valueOf(strategy.getId()));
            newStrategy.setName(strategy.getName());
            StrategyDTO strategyDTO = STRATEGY_MAPPER.mapToStrategyDTO(strategyRepository.saveAndFlush(newStrategy));
            nu.itark.frosk.bot.bot.domain.Strategy strategyee= strategyRepository.saveAndFlush(newStrategy);
            logger.debug("Strategy created in database: {}", newStrategy);
            //position.setStrategy(STRATEGY_MAPPER.mapToStrategy(featuredStrategy.getConfiguration().getStrategyDTO()));
            position.setStrategy(strategyee);
            position = positionRepository.saveAndFlush(position);
            // =========================================================================================================
            // Creates the position dto.
            //TODO
//            PositionDTO p = new PositionDTO(position.getUid(), type, featuredStrategy.getConfiguration().getStrategyDTO(), currencyPair, amount, orderCreationResult.getOrder(), rules);

/*
            StrategyDTO strategyDTO = StrategyDTO.builder()
                    .name(strategyee.getName())
                    .strategyId(strategyee.getStrategyId())
                    .build();
*/

            PositionDTO p = new PositionDTO(position.getUid(), type, strategyDTO, currencyPair, amount, orderCreationResult.getOrder(), null);

            positionRepository.save(POSITION_MAPPER.mapToPosition(p));
            logger.debug("Position {} opened with order {}",
                    p.getPositionId(),
                    orderCreationResult.getOrder().getOrderId());

            // =========================================================================================================
            // Emit the position, creates and return the position creation result.
            positionFlux.emitValue(p);
            return new PositionCreationResultDTO(p);
        } else {
            logger.error("Position creation failure: {}", orderCreationResult.getErrorMessage());
            return new PositionCreationResultDTO(orderCreationResult.getErrorMessage(), orderCreationResult.getException());
        }
    }


    @Override
    public final void updatePositionRules(final long positionUid, @NonNull final PositionRulesDTO newRules) {
        logger.debug("Updating position {} with the rules: {}", positionUid, newRules);
        final Optional<Position> p = positionRepository.findById(positionUid);
        // If position exists and position is not closed.
        if (p.isPresent() && p.get().getStatus() != CLOSED) {
            // Stop gain.
            if (newRules.isStopGainPercentageSet()) {
                positionRepository.updateStopGainRule(positionUid, newRules.getStopGainPercentage());
            } else {
                positionRepository.updateStopGainRule(positionUid, null);
            }
            // Stop loss.
            if (newRules.isStopLossPercentageSet()) {
                positionRepository.updateStopLossRule(positionUid, newRules.getStopLossPercentage());
            } else {
                positionRepository.updateStopLossRule(positionUid, null);
            }
        }
    }

    @Override
    public final OrderCreationResultDTO closePosition(@NonNull final CassandreStrategyInterface strategy,
                                                      final long positionUid,
                                                      @NonNull final TickerDTO ticker) {
        logger.debug("Trying to close position {}.", positionUid);
        final Optional<Position> position = positionRepository.findById(positionUid);
        if (position.isPresent()) {
            final PositionDTO positionDTO = POSITION_MAPPER.mapToPositionDTO(position.get());
            final OrderCreationResultDTO orderCreationResult;

            // =========================================================================================================
            // Here, we create the order creation depending on the position type (Short or long).
            if (positionDTO.getType() == LONG) {
                // Long - We just sell.
                orderCreationResult = tradeService.createSellMarketOrder(strategy, positionDTO.getCurrencyPair(), positionDTO.getAmount().getValue());
            } else {
                // Short - We buy back with the money we get from the original selling.
                // On opening, we had:
                // CP2: ETH/USDT - 1 ETH costs 10 USDT - We sold 1 ETH, and it will give us 10 USDT.
                // We will use those 10 USDT to buy back ETH when the rule is triggered.
                // CP2: ETH/USDT - 1 ETH costs 2 USDT - We buy 5 ETH, and it will cost us 10 USDT.
                // We can now use those 10 USDT to buy 5 ETH (amount sold / price).
                final BigDecimal amountToBuy = positionDTO.getAmountToLock().getValue().divide(ticker.getLast(), HALF_UP).setScale(BIGINTEGER_SCALE, FLOOR);
                orderCreationResult = tradeService.createBuyMarketOrder(strategy, positionDTO.getCurrencyPair(), amountToBuy);
            }

            // =========================================================================================================
            // If the order is successful, we set the position as closed using closePositionWithOrder().
            if (orderCreationResult.isSuccessful()) {
                positionDTO.closePositionWithOrder(orderCreationResult.getOrder());
                logger.debug("Position {} closed with order {}", positionDTO.getPositionId(), orderCreationResult.getOrder().getOrderId());
            } else {
                logger.error("Position {} not closed, failed to create order: {}", positionDTO.getPositionId(), orderCreationResult.getErrorMessage());
            }

            // =========================================================================================================
            // We emit the position to save it and send events to strategies.
            positionFlux.emitValue(positionDTO);
            return orderCreationResult;
        } else {
            logger.error("Impossible to close position {} because we couldn't find it in database", positionUid);
            return new OrderCreationResultDTO("Impossible to close position " + positionUid + " because we couldn't find it in database", null);
        }
    }

    @Override
    public final void setAutoClose(final long positionUid, final boolean value) {
        logger.debug("Set auto close to {} on position {}", value, positionUid);
        positionRepository.updateAutoClose(positionUid, value);
    }

    @Override
    public final void forcePositionClosing(final long positionUid) {
        logger.debug("Force position {} to close", positionUid);
        positionRepository.updateForceClosing(positionUid, true);
    }

    @Override
    public final Set<PositionDTO> getPositions() {
        logger.debug("Retrieving all positions");
        return positionRepository.findByOrderByUid().stream()
                .map(POSITION_MAPPER::mapToPositionDTO)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public final Optional<PositionDTO> getPositionByUid(final long positionUid) {
        logger.debug("Retrieving position by its uid {}", positionUid);
        return positionRepository.findById(positionUid).map(POSITION_MAPPER::mapToPositionDTO);
    }

    @Override
    public final Map<Long, CurrencyAmountDTO> getAmountsLockedByPosition() {
        logger.debug("Retrieving amounts locked by position");
        return positionRepository.findByStatusIn(Stream.of(OPENING, OPENED).toList())
                .stream()
                .map(POSITION_MAPPER::mapToPositionDTO)
                .collect(Collectors.toMap(PositionDTO::getUid, PositionDTO::getAmountToLock, (key, value) -> key, HashMap::new));
    }

    @Override
    public final Map<CurrencyDTO, GainDTO> getGains(final long strategyUid) {
        logger.debug("Retrieving gains for all positions");
        HashMap<CurrencyDTO, BigDecimal> totalBefore = new LinkedHashMap<>();
        HashMap<CurrencyDTO, BigDecimal> totalAfter = new LinkedHashMap<>();
        List<CurrencyAmountDTO> openingOrdersFees = new LinkedList<>();
        List<CurrencyAmountDTO> closingOrdersFees = new LinkedList<>();
        HashMap<CurrencyDTO, GainDTO> gains = new LinkedHashMap<>();

        // We calculate, by currency, the amount bought & sold.
        positionRepository.findByStatus(CLOSED)
                .stream()
                // If we have a strategyUid equals to 0, it means we calculate all gains on all closed positions.
                // If we have a strategyUid different from 0, we only retrieved the position of a particular strategy.
                .filter(position -> strategyUid == 0 || position.getStrategy().getUid() == strategyUid)
                .map(POSITION_MAPPER::mapToPositionDTO)
                .forEach(positionDTO -> {
                    // We retrieve the currency and initiate the maps if they are empty
                    CurrencyDTO currency;
                    if (positionDTO.getType() == LONG) {
                        // LONG.
                        currency = positionDTO.getCurrencyPair().getQuoteCurrency();
                    } else {
                        // SHORT.
                        currency = positionDTO.getCurrencyPair().getBaseCurrency();
                    }
                    gains.putIfAbsent(currency, null);
                    totalBefore.putIfAbsent(currency, ZERO);
                    totalAfter.putIfAbsent(currency, ZERO);

                    // We calculate the amounts bought and amount sold.
                    if (positionDTO.getType() == LONG) {
                        totalBefore.put(currency, positionDTO.getOpeningOrder().getTrades()
                                .stream()
                                .map(t -> t.getAmountValue().multiply(t.getPriceValue()))
                                .reduce(totalBefore.get(currency), BigDecimal::add));
                        totalAfter.put(currency, positionDTO.getClosingOrder().getTrades()
                                .stream()
                                .map(t -> t.getAmountValue().multiply(t.getPriceValue()))
                                .reduce(totalAfter.get(currency), BigDecimal::add));
                    } else {
                        totalBefore.put(currency, positionDTO.getOpeningOrder().getTrades()
                                .stream()
                                .map(TradeDTO::getAmountValue)
                                .reduce(totalBefore.get(currency), BigDecimal::add));
                        totalAfter.put(currency, positionDTO.getClosingOrder().getTrades()
                                .stream()
                                .map(TradeDTO::getAmountValue)
                                .reduce(totalAfter.get(currency), BigDecimal::add));
                    }

                    // And now the fees.
                    positionDTO.getOpeningOrder().getTrades()
                            .stream()
                            .filter(tradeDTO -> tradeDTO.getFee() != null)
                            .forEach(tradeDTO -> openingOrdersFees.add(tradeDTO.getFee()));
                    positionDTO.getClosingOrder().getTrades()
                            .stream()
                            .filter(tradeDTO -> tradeDTO.getFee() != null)
                            .forEach(tradeDTO -> closingOrdersFees.add(tradeDTO.getFee()));
                });

        gains.keySet()
                .forEach(currency -> {
                    // We make the calculation.
                    BigDecimal before = totalBefore.get(currency);
                    BigDecimal after = totalAfter.get(currency);
                    BigDecimal gainAmount = after.subtract(before);
                    BigDecimal gainPercentage = ((after.subtract(before)).divide(before, HALF_UP)).multiply(ONE_HUNDRED_BIG_DECIMAL);

                    GainDTO g = GainDTO.builder()
                            .percentage(gainPercentage.setScale(2, HALF_UP).doubleValue())
                            .amount(CurrencyAmountDTO.builder()
                                    .value(gainAmount)
                                    .currency(currency)
                                    .build())
                            .openingOrderFees(openingOrdersFees)
                            .closingOrderFees(closingOrdersFees)
                            .build();
                    gains.put(currency, g);
                });
        return gains;
    }

    @Override
    public final Map<CurrencyDTO, GainDTO> getGains() {
        return getGains(0);
    }

}
