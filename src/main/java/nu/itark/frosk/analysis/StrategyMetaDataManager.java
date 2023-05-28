package nu.itark.frosk.analysis;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.TopStrategy;
import nu.itark.frosk.service.BarSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class StrategyMetaDataManager {

    @Autowired
    BarSeriesService timeSeriesService;

    @Autowired
    DataSetRepository datasetRepository;

    @Autowired
    FeaturedStrategyRepository featuredStrategyRepository;

    /**
     * Gets all featured strategies, hence per all strategies and all securities.
     *
     * @return
     */
    public List<TopStrategyDTO> getTopStrategies() {
        List<TopStrategyDTO> returnList = new ArrayList<>();
        featuredStrategyRepository.findStrategies().forEach(fs->{
                returnList.add(getDTO(fs));
        });
        return returnList;
    }

    public TopStrategyDTO getDTO(TopStrategy ts) {
        return TopStrategyDTO.builder()
                .name(ts.getName())
                .totalProfit(BigDecimal.valueOf(ts.getTotalProfit().doubleValue()).round(new MathContext(2)))
                .build();
    }

}
