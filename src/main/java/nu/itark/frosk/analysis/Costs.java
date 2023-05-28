package nu.itark.frosk.analysis;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.analysis.cost.CostModel;
import org.ta4j.core.analysis.cost.LinearBorrowingCostModel;
import org.ta4j.core.analysis.cost.LinearTransactionCostModel;

@Component
@Data
public class Costs {
    @Value("${exchange.transaction.feePerTradePercent}")
    private double feePerTrade;
    @Value("${exchange.transaction.borrowingFee}")
    private double borrowingFee;

    //TODO, översyn på denna, funkar förmodligen inte.

    private CostModel transactionCostModel = new LinearTransactionCostModel(feePerTrade);
    private CostModel borrowingCostModel = new LinearBorrowingCostModel(borrowingFee);
}
