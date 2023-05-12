package nu.itark.frosk.crypto.coinbase.api.payments;

import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

/**
 * Created by robevansuk on 16/02/2017.
 */
public class PaymentService {

    private static final String PAYMENT_METHODS_ENDPOINT = "/payment-methods";
    private static final String COINBASE_ACCOUNTS_ENDPOINT = "/coinbase-accounts";

    final Coinbase coinbaseExchange;

    public PaymentService(final Coinbase coinbaseExchange) {
        this.coinbaseExchange = coinbaseExchange;
    }

    public List<PaymentType> getPaymentTypes() {
        return coinbaseExchange.getAsList(PAYMENT_METHODS_ENDPOINT, new ParameterizedTypeReference<PaymentType[]>(){});
    }

    public List<CoinbaseAccount> getCoinbaseAccounts() {
        return coinbaseExchange.getAsList(COINBASE_ACCOUNTS_ENDPOINT, new ParameterizedTypeReference<CoinbaseAccount[]>() {});
    }
}