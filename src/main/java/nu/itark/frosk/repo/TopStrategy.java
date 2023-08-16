package nu.itark.frosk.repo;

import java.math.BigDecimal;

public interface TopStrategy {
    BigDecimal getSqn();
    BigDecimal getTotalProfit();
    String getName();
}
