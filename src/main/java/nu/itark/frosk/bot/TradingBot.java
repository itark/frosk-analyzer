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
import nu.itark.frosk.bot.bot.domain.Order;
import nu.itark.frosk.bot.bot.dto.market.TickerDTO;
import nu.itark.frosk.bot.bot.dto.position.PositionCreationResultDTO;
import nu.itark.frosk.bot.bot.dto.trade.OrderCreationResultDTO;
import nu.itark.frosk.bot.bot.dto.trade.OrderDTO;
import nu.itark.frosk.bot.bot.dto.trade.OrderTypeDTO;
import nu.itark.frosk.bot.bot.dto.trade.TradeDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyAmountDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyDTO;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.repository.OrderRepository;
import nu.itark.frosk.bot.bot.repository.StrategyRepository;
import nu.itark.frosk.bot.bot.repository.TradeRepository;
import nu.itark.frosk.bot.bot.service.PositionService;
import nu.itark.frosk.bot.bot.service.TradeService;
import nu.itark.frosk.bot.bot.util.base.Base;
import nu.itark.frosk.bot.bot.util.jpa.CurrencyAmount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.analysis.cost.CostModel;
import org.ta4j.core.analysis.cost.LinearTransactionCostModel;
import org.ta4j.core.criteria.pnl.ProfitLossPercentageCriterion;
import org.ta4j.core.num.DoubleNum;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

@Service
@Slf4j
public class TradingBot extends Base {

    @Value("${exchange.transaction.feePerTradePercent}")
    private double feePerTrade;

    @Autowired
    PositionService positionService;

    @Autowired
    TradeService tradeService;

    @Autowired
    StrategyRepository strategyRepository;

    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    OrderRepository orderRepository;

    public void run(Strategy strategy, BarSeries series) {
        int runBeginIndex = series.getBeginIndex();
        int runEndIndex = series.getEndIndex();
        log.info("Running strategy (indexes: {} -> {}): {}", new Object[]{runBeginIndex, runEndIndex, strategy.getName()});
        //CostModel transactionCostModel = new LinearTransactionCostModel(feePerTrade);
       // BarSeriesManager seriesManager = new BarSeriesManager(series, transactionCostModel, null);
        BarSeriesManager seriesManager = new BarSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        System.out.println("XXX: Total percentage: " + new ProfitLossPercentageCriterion().calculate(series, tradingRecord).doubleValue());

        double totalProfit = new ProfitLossPercentageCriterion().calculate(series, tradingRecord).doubleValue();
        log.info("XXX Total PnL:{} % ",totalProfit);

        int seriesMaxSize;
        createStrategy(strategy);
        PositionCreationResultDTO longPosition = null;

        for (seriesMaxSize = runBeginIndex; seriesMaxSize <= runEndIndex; ++seriesMaxSize) {
            if (strategy.shouldEnter(seriesMaxSize)) {
                // Our strategy should enter
                boolean entered = tradingRecord.enter(seriesMaxSize, series.getBar(seriesMaxSize).getClosePrice(), DoubleNum.valueOf(10));
                if (entered) {
                    Trade entry = tradingRecord.getLastEntry();
                    log.info("Entered on " + entry.getIndex() + " (price=" + entry.getNetPrice().doubleValue() + ", amount=" + entry.getAmount().doubleValue() + ")");
                    //log.info("strategy: {}, securityName:{}, netPrice:{}", strategy.getName(), series.getName(), entry.getNetPrice().doubleValue());
/*
                    longPosition = createLongPosition(strategy, series, entry);
                    createTrade(longPosition,series,entry, OrderTypeDTO.ASK);
                    final Set<OrderDTO> orders = tradeService.getOrders();
                    log.info("Enter orders"+orders);
*/
                }
            } else if (strategy.shouldExit(seriesMaxSize)) {
                // Our strategy should exit
                boolean exited = tradingRecord.exit(seriesMaxSize, series.getBar(seriesMaxSize).getClosePrice(), DoubleNum.valueOf(10));
                if (exited) {
                    Trade exit = tradingRecord.getLastExit();
                    log.info("Exited on " + exit.getIndex() + " (price=" + exit.getNetPrice().doubleValue() + ", amount=" + exit.getAmount().doubleValue() + ")");
/*
                    closePosition(strategy,series,longPosition.getPosition().getUid(), exit);
                    createTrade(longPosition,series,exit, OrderTypeDTO.BID);
                    final Set<OrderDTO> orders = tradeService.getOrders();
                    log.info("Exit orders"+orders);
*/
                }
            }
        }

        double totalProfit2 = new ProfitLossPercentageCriterion().calculate(series, tradingRecord).doubleValue();
        log.info("YYY Total PnL:{}% ",totalProfit2);

    }

    private void createTrade(PositionCreationResultDTO longPosition, BarSeries barSeries, Trade entry, OrderTypeDTO type) {
        final Order byOrderId = orderRepository.findByOrderId(longPosition.getPosition().getOpeningOrder().getOrderId()).get();
        CurrencyPairDTO currencyPairDTO = new CurrencyPairDTO(barSeries.getName());
        BigDecimal limitPrice = new BigDecimal(entry.getNetPrice().doubleValue());

        CurrencyAmount currencyAmountPrice = new CurrencyAmount();
        currencyAmountPrice.setValue(limitPrice);
        currencyAmountPrice.setCurrency(currencyPairDTO.getBaseCurrency().getCurrencyCode());

        CurrencyAmount currencyAmount = new CurrencyAmount();
        currencyAmount.setValue(new BigDecimal(1));
        currencyAmount.setCurrency(currencyPairDTO.getBaseCurrency().getCurrencyCode());

        nu.itark.frosk.bot.bot.domain.Trade newTrade = new nu.itark.frosk.bot.bot.domain.Trade();
        newTrade.setTradeId(UUID.randomUUID().toString());
        newTrade.setPrice(currencyAmountPrice);
        newTrade.setAmount(currencyAmount);
        newTrade.setOrder(byOrderId);
        newTrade.setType(type);
        newTrade.setCurrencyPair(barSeries.getName());
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
