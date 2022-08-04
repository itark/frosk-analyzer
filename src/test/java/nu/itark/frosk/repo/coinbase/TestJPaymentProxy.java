package nu.itark.frosk.repo.coinbase;

import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.payments.PaymentType;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import nu.itark.frosk.crypto.coinbase.PaymentProxy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@Import({IntegrationTestConfiguration.class})
@Slf4j
public class TestJPaymentProxy extends BaseIntegrationTest {


    @Autowired
    PaymentProxy paymentProxy;

   @Test
   public final void testGetAccounts() {
	   List<PaymentType> accountList = paymentProxy.getAccounts();
       print(accountList);
   }

    void print(List<PaymentType> list) {
        list.forEach(o -> {
            log.info(ReflectionToStringBuilder.toString(o, ToStringStyle.MULTI_LINE_STYLE));
        });
    }

}
