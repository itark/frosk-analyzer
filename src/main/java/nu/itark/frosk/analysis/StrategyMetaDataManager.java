package nu.itark.frosk.analysis;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.TopStrategy;
import nu.itark.frosk.service.BarSeriesService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.Strategy;
import org.ta4j.core.analysis.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

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
