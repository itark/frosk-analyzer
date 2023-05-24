package nu.itark.frosk.analysis;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.cost.CostModel;
import org.ta4j.core.cost.LinearBorrowingCostModel;
import org.ta4j.core.cost.LinearTransactionCostModel;

@Component
@Data
public class Costs {
    @Value("${exchange.transaction.feePerTradePercent}")
    double feePerTrade;
    @Value("${exchange.transaction.borrowingFee}")
    double borrowingFee;
    private CostModel transactionCostModel = new LinearTransactionCostModel(feePerTrade);
    private CostModel borrowingCostModel = new LinearBorrowingCostModel(borrowingFee);
}
