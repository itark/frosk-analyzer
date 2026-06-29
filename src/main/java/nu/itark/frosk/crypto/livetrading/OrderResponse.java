package nu.itark.frosk.crypto.livetrading;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class OrderResponse {
    private String orderId;
    private String clientOrderId;
    private String productId;
    private String side;
    private BigDecimal filledSize;
    private BigDecimal averageFilledPrice;
    private String status;
    private String errorMessage;
}
