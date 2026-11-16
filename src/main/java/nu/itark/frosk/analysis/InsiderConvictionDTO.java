package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** S12 clustered-insider-buying conviction for one issuer (>= N distinct insider buyers in a window). */
@Data
@Builder
public class InsiderConvictionDTO {
    private String issuer;
    /** Matched security in our universe, or null if the issuer couldn't be matched. */
    private String securityName;
    private int distinctBuyers;
    private List<String> buyerNames;
    private String firstBuyDate;
    private String lastBuyDate;
    private double totalBuyValue;
}
