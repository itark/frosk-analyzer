/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014-2017 Marc de Verdelhan, 2017-2021 Ta4j Organization & respective
 * authors (see AUTHORS)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nu.itark.frosk.bot;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.FeaturedStrategyDTO;
import nu.itark.frosk.analysis.StrategiesMap;
import nu.itark.frosk.bot.bot.domain.Order;
import nu.itark.frosk.bot.bot.dto.market.TickerDTO;
import nu.itark.frosk.bot.bot.dto.position.PositionCreationResultDTO;
import nu.itark.frosk.bot.bot.dto.trade.OrderCreationResultDTO;
import nu.itark.frosk.bot.bot.dto.trade.OrderStatusDTO;
import nu.itark.frosk.bot.bot.dto.trade.OrderTypeDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.repository.OrderRepository;
import nu.itark.frosk.bot.bot.repository.StrategyRepository;
import nu.itark.frosk.bot.bot.repository.TradeRepository;
import nu.itark.frosk.bot.bot.service.PositionService;
import nu.itark.frosk.bot.bot.util.base.Base;
import nu.itark.frosk.bot.bot.util.jpa.CurrencyAmount;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.StrategyTradeRepository;
import nu.itark.frosk.repo.TradingAccountRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.TradingAccountService;
import nu.itark.frosk.strategies.filter.StrategyFilter;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
public class TradingBot extends Base {

    @Autowired
    PositionService positionService;

    @Autowired
    BarSeriesService barSeriesService;

    @Autowired
    TradingAccountService tradingAccountService;

    @Autowired
    StrategyRepository strategyRepository;


    @Autowired
    StrategyTradeRepository strategyTradeRepository;

    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    TradingAccountRepository tradingAccountRepository;

    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;

    @Autowired
    StrategyFilter strategyFilter;

    @Autowired
    StrategiesMap strategiesMap;

    public void run(Strategy strategy, BarSeries series) {
        List<BigDecimal> totalBefore = new ArrayList<>();
        List<BigDecimal> totalAfter = new ArrayList<>();
        final List<FeaturedStrategy> topStrategies = featuredStrategyRepository.findTopStrategies(BigDecimal.valueOf(0.1), 4, BigDecimal.valueOf(1.7), BigDecimal.valueOf(0.5), false);
        final Long fsId = topStrategies.get(0).getId();
        System.out.println("fsId:" + fsId);
        final List<StrategyTrade> strategyTradeList = strategyTradeRepository.findByFeaturedStrategyId(fsId);
        strategyTradeList.stream()
                .forEach(trade -> {
                    if (trade.getType().equals(Trade.TradeType.BUY.name())) {
                        totalBefore.add(trade.getPrice().multiply(trade.getAmount()));
                    } else {
                        totalAfter.add(trade.getPrice().multiply(trade.getAmount()));
                    }
                });

        BigDecimal buySum = totalBefore.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sellSum = totalAfter.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = sellSum.subtract(buySum);

        System.out.println("buySum:" + buySum);
        System.out.println("sellSum:" + sellSum);
        System.out.println("totalProfit(EUR):" + totalProfit);


    }


    public void running(Strategy strategy, BarSeries series) {
        int runBeginIndex = series.getBeginIndex();
        int runEndIndex = series.getEndIndex();
        log.info("Running strategy (indexes: {} -> {}): {}", new Object[]{runBeginIndex, runEndIndex, strategy.getName()});
        TradingRecord tradingRecord = barSeriesService.runConfiguredStrategy(series, strategy);

        int seriesMaxSize;
        TradingAccount tradingAccount = tradingAccountRepository.findAll().get(0);
        final BigDecimal totalValueForSecurities = tradingAccount.getSecurityValue();
        final BigDecimal positionValue = tradingAccount.getPositionValue();
        System.out.println("securityValue:" + totalValueForSecurities);
        System.out.println("positionValue:" + positionValue);

        Num positionIn = DoubleNum.valueOf(positionValue);
        Num buyAmount = DoubleNum.valueOf(1);
        log.info("strategy: {}, securityName:{}", strategy.getName(), series.getName());
        for (seriesMaxSize = runBeginIndex; seriesMaxSize <= runEndIndex; ++seriesMaxSize) {
            if (strategy.shouldEnter(seriesMaxSize)) {
                log.info("shouldEnter");
                //buyAmount = buyAmount == DoubleNum.valueOf(1) ? buyAmount : positionIn.dividedBy(series.getBar(seriesMaxSize).getClosePrice());
                buyAmount = positionIn.dividedBy(series.getBar(seriesMaxSize).getClosePrice());
                log.info("buyAmount:" + buyAmount);
                boolean entered = tradingRecord.enter(seriesMaxSize, series.getBar(seriesMaxSize).getClosePrice(), buyAmount);
                if (entered) {
                    Trade entry = tradingRecord.getLastEntry();
                    log.info("Entered on " + entry.getIndex() + " (net_price=" + entry.getNetPrice().doubleValue() + ", amount=" + entry.getAmount().doubleValue() + ")");
                    //log.info("Entered on netPrice:{}", entry.getNetPrice().doubleValue());
/*
                    longPosition = createLongPosition(strategy, series, entry);
                    createTrade(longPosition,series,entry, OrderTypeDTO.ASK, amount);
                    final Set<OrderDTO> orders = tradeService.getOrders();
                    log.info("Enter orders"+orders);
                    fixOrderStatus(longPosition.getPosition().getOpeningOrder().getOrderId());
*/
                }
            } else if (strategy.shouldExit(seriesMaxSize)) {
                //log.info("shouldExit");
                log.info("buyAmount -> exit:" + buyAmount);
                boolean exited = tradingRecord.exit(seriesMaxSize, series.getBar(seriesMaxSize).getClosePrice(), buyAmount);
                if (exited) {
                    Trade exit = tradingRecord.getLastExit();
                    log.info("Exited on " + exit.getIndex() + " (net_price=" + exit.getNetPrice().doubleValue() + ", amount=" + exit.getAmount().doubleValue() + ")");
                    //log.info("Exited on netPrice:{}", exit.getNetPrice().doubleValue());
/*
                    var dto = closePosition(strategy,series,longPosition.getPosition().getUid(), exit);
                    createTrade(longPosition,series,exit, OrderTypeDTO.BID, amount);
                    final Set<OrderDTO> orders = tradeService.getOrders();
                    log.info("Exit orders"+orders);
                    fixOrderStatus(dto.getOrderId());
                    final List<TradingAccount> all = tradingAccountRepository.findAll();
*/

                }
            }
        }

        double pnl = new ProfitLossPercentageCriterion().calculate(series, tradingRecord).doubleValue();
        log.info("PnL:{}% ", pnl);

        double profit = new ProfitCriterion().calculate(series, tradingRecord).doubleValue();
        log.info("Profit:{}% ", profit);
    }


    public void runningPositions() {
        final List<FeaturedStrategyDTO> topFeaturedStrategies = strategyFilter.getTopFeaturedStrategies();
        log.info("**TopFeaturedStrategies** }");

        topFeaturedStrategies.stream()
                .peek(o -> System.out.println("o.name:" + o.getName() + "o.secName:" + o.getSecurityName()))
                .toList();


        topFeaturedStrategies.stream().forEach(dto -> {
            log.info("FeaturedStrategy: name: {}, security: {}", dto.getName(), dto.getSecurityName());
            log.info(Marker.ANY_MARKER);
            BarSeries series = barSeriesService.getDataSet(dto.getSecurityName(), false, false);
            List<TradingAccount> tradingAccounts = tradingAccountRepository.findAll();
            tradingAccounts.forEach(tradingAccount -> {
                log.info("TradingAccount: type:{}, inherentExitRule:{}", tradingAccount.getAccountType().getType(), tradingAccount.getAccountType().getInherentExitRule());
                tradingAccountService.setActiveTradingAccount(tradingAccount);
                Strategy strategyToRun = strategiesMap.getStrategyToRun(dto.getName() + "Strategy", series);
                runningPositions(strategyToRun, series);
            });
        });
    }

    public void runningPositions(Strategy strategy, BarSeries series) {
        TradingRecord tradingRecord = barSeriesService.runConfiguredStrategy(series, strategy);
        //log.info("TradingInfo" + tradingAccountService.getTradingAccounts());
        for (Position position : tradingRecord.getPositions()) {
            // log.info("Entered on " + position.getEntry().getIndex() + " (net_price=" + position.getEntry().getNetPrice().doubleValue() + ", amount=" + position.getEntry().getAmount().doubleValue() + ")");
            // log.info("Exited on " + position.getExit().getIndex() + " (net_price=" + position.getExit().getNetPrice().doubleValue() + ", amount=" + position.getExit().getAmount().doubleValue() + ")");
            // log.info("position.profit=" + position.getProfit());
            // tradingAccountService.updateAccountOnProfit(position.getProfit());
            tradingAccountService.updateAccount(position);
        }
        // log.info("Leaving strategy: *** {} ***, accountValue: {}", strategy.getName(), tradingAccountService.getAccountValue());
        tradingAccountService.updateTotalReturnPercentage();
        log.info("strategy: *** {} ***, securityName:{}, accountValue: {}", strategy.getName(), series.getName(), tradingAccountService.getAccountValue());

        // log.info("TradingInfo : {}", tradingAccountService.getTradingAccounts());
    }

    private void fixOrderStatus(String orderId) {
        final Order order = orderRepository.findByOrderId(orderId).orElseThrow();
        order.setStatus(OrderStatusDTO.CLOSED);
        orderRepository.saveAndFlush(order);
    }

    private void createTrade(PositionCreationResultDTO longPosition, BarSeries barSeries, Trade entry, OrderTypeDTO type, Num amount) {
        final Order byOrderId = orderRepository.findByOrderId(longPosition.getPosition().getOpeningOrder().getOrderId()).get();
        CurrencyPairDTO currencyPairDTO = new CurrencyPairDTO(barSeries.getName());
        BigDecimal limitPrice = new BigDecimal(entry.getNetPrice().doubleValue());

        CurrencyAmount currencyAmountPrice = new CurrencyAmount();
        currencyAmountPrice.setValue(limitPrice);
        currencyAmountPrice.setCurrency(currencyPairDTO.getBaseCurrency().getCurrencyCode());

        CurrencyAmount currencyAmount = new CurrencyAmount();
        currencyAmount.setValue(BigDecimal.valueOf(amount.doubleValue()));
        currencyAmount.setCurrency(currencyPairDTO.getBaseCurrency().getCurrencyCode());

        CurrencyAmount currencyFee = new CurrencyAmount();
        currencyFee.setValue(BigDecimal.valueOf(0.006));
        currencyFee.setCurrency(currencyPairDTO.getBaseCurrency().getCurrencyCode());

        nu.itark.frosk.bot.bot.domain.Trade newTrade = new nu.itark.frosk.bot.bot.domain.Trade();
        newTrade.setTradeId(UUID.randomUUID().toString());
        newTrade.setPrice(currencyAmountPrice);
        newTrade.setAmount(currencyAmount);
        newTrade.setOrder(byOrderId);
        newTrade.setType(type);
        newTrade.setCurrencyPair(barSeries.getName());
        newTrade.setTimestamp(ZonedDateTime.now());
        newTrade.setFee(currencyFee);
        tradeRepository.save(newTrade);

    }


    private void createStrategy(Strategy strategy) {
        final Optional<nu.itark.frosk.bot.bot.domain.Strategy> strategyInDatabase = strategyRepository.findByStrategyId(strategy.getName());
        nu.itark.frosk.bot.bot.domain.Strategy newStrategy = new nu.itark.frosk.bot.bot.domain.Strategy();
        if (!strategyInDatabase.isPresent()) {
            newStrategy.setStrategyId(strategy.getName());
            newStrategy.setName(strategy.getName());
            strategyRepository.saveAndFlush(newStrategy);
            log.info("Strategy created in database: {}", newStrategy);
        }
    }


    private PositionCreationResultDTO createLongPosition(Strategy strategy, BarSeries barSeries, Trade entry) {
        CurrencyPairDTO currencyPairDTO = new CurrencyPairDTO(barSeries.getName());
        BigDecimal amount = new BigDecimal(1);
        BigDecimal limitPrice = new BigDecimal(entry.getNetPrice().doubleValue());
        return positionService.createLongPosition(strategy, currencyPairDTO, amount, limitPrice, null);
    }


    private OrderCreationResultDTO closePosition(Strategy strategy, BarSeries barSeries, long positionId, Trade exit) {
        CurrencyPairDTO currencyPairDTO = new CurrencyPairDTO(barSeries.getName());
        BigDecimal netPrice = new BigDecimal(exit.getNetPrice().doubleValue());
        TickerDTO tickerDTO = TickerDTO.builder()
                .currencyPair(currencyPairDTO)
                .last(netPrice)
                .build();
        return positionService.closePosition(strategy, positionId, tickerDTO);
    }


}
