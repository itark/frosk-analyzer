package nu.itark.frosk.crypto.coinbase.service;

import nu.itark.frosk.bot.bot.util.xchange.CancelOrderParams;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.exceptions.FundsExceededException;
import org.knowm.xchange.service.trade.params.CancelOrderByIdParams;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.random.RandomGenerator;

//@Component
public class CoinbaseProTradeService  {


  //TODO fix!

  public String placeMarketOrder(MarketOrder marketOrder) throws IOException {
/*
    return placeCoinbaseProOrder(CoinbaseProAdapters.adaptCoinbaseProPlaceMarketOrder(marketOrder))
         .getId();
*/

    return UUID.randomUUID().toString();

  }

  //TODO fix

  public String placeLimitOrder(LimitOrder limitOrder) throws IOException, FundsExceededException {
/*
    return placeCoinbaseProOrder(CoinbaseProAdapters.adaptCoinbaseProPlaceLimitOrder(limitOrder))
        .getId();
*/

    return UUID.randomUUID().toString();

  }


  public boolean cancelOrder(String orderId) throws IOException {
   // return cancelCoinbaseProOrder(orderId);

    return true;

  }

  public boolean cancelOrder(CancelOrderParams orderParams) throws IOException {
    if (orderParams instanceof CancelOrderByIdParams) {
      return cancelOrder(((CancelOrderByIdParams) orderParams).getOrderId());
    } else {
      return false;
    }
  }


}
