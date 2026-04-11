package nu.itark.frosk.rapidapi.yhfinance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetProfileBody {
    private String sector;
    private String industry;
}
