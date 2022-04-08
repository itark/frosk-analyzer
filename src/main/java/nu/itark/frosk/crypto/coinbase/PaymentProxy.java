package nu.itark.frosk.crypto.coinbase;

import com.coinbase.exchange.api.payments.PaymentService;
import com.coinbase.exchange.api.payments.PaymentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * https://docs.pro.coinbase.com/#payment-methods
 *
 * This endpoint requires the "transfer" permission.
 */
@Component
public class PaymentProxy {

	@Autowired
    PaymentService paymentService;

    public List<PaymentType> getAccounts() {
        return paymentService.getPaymentTypes();
    }

}
