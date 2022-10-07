package nu.itark.frosk.analysis;

import com.coinbase.exchange.model.Granularity;
import nu.itark.frosk.dataset.Database;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.service.BarSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingInt;

@Component
public class SecurityMetaDataManager {

    @Autowired
    BarSeriesService timeSeriesService;

    @Autowired
    DataSetRepository datasetRepository;

    public List<SecurityDTO> getSecurityMetaData() {
        List<SecurityDTO> securityDTOList = new ArrayList<SecurityDTO>();
        DataSet dataset = datasetRepository.findByName(Database.COINBASE.name());

        dataset.getSecurities().forEach(s -> {
            SecurityDTO securityDTO = new SecurityDTO(s.getName());
            addMetaData(securityDTO);
            securityDTOList.add(securityDTO);
        });

        return securityDTOList;
    }

    private void addMetaData(SecurityDTO securityDTO) {
        securityDTO.setOneDayPercent(getBarPercent(securityDTO.getName(),1));
        securityDTO.setOneWeekPercent(getBarPercent(securityDTO.getName(),7));
        securityDTO.setOneMonthPercent(getBarPercent(securityDTO.getName(),30));
        securityDTO.setThreeMonthPercent(getBarPercent(securityDTO.getName(),90));
        securityDTO.setSixMonthPercent(getBarPercent(securityDTO.getName(),180));
    }

    protected BigDecimal getBarPercent(String securityName, int nrOfBars) {
        BarSeries timeSeries = timeSeriesService.getDataSet(securityName, false);
        //Sanitycheck
        if (timeSeries.getBarCount() <= nrOfBars) {
            return null;
        }
        Num entryOpen = timeSeries.getBar((timeSeries.getBarCount()-1) - nrOfBars).getOpenPrice();
        Num exitOpen = timeSeries.getLastBar().getOpenPrice();
        Num grossProfit = exitOpen.minus(entryOpen);
        Num pnl = grossProfit.dividedBy(entryOpen);
        if (pnl.isNaN()) {
            return null;
        } else {
            return BigDecimal.valueOf(pnl.doubleValue()).round(new MathContext(2));
        }
    }

}
