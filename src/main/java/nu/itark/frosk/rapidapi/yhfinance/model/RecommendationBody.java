package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

import java.util.List;

@Data
public class RecommendationBody {
    private List<RecommendationTrendDTO> trend;
    private int maxAge;
}
