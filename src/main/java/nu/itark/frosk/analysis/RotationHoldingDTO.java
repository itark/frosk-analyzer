package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

/** One target holding in the S9 sector-rotation allocation. */
@Data
@Builder
public class RotationHoldingDTO {
    private String basket;        // "cyclical" | "defensive"
    private String securityName;
    private double weight;        // fraction of total portfolio
}
