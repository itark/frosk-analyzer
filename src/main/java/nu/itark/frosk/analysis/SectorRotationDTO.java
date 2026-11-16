package nu.itark.frosk.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** S9 sector rotation: HedgeIndex-driven cyclical/defensive/cash allocation on OMX baskets. */
@Data
@Builder
public class SectorRotationDTO {
    private String date;
    private int hedgeIndexScore;
    private String regime;
    private double cyclicalWeight;
    private double defensiveWeight;
    private double cashWeight;
    private List<String> cyclicalBasket;
    private List<String> defensiveBasket;
    private List<RotationHoldingDTO> targetHoldings;
}
