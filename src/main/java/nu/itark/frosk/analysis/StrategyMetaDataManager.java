package nu.itark.frosk.analysis;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.model.dto.AccountTypeDTO;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.FeaturedStrategyRepository;
import nu.itark.frosk.repo.TopStrategy;
import nu.itark.frosk.repo.TradingAccountRepository;
import nu.itark.frosk.service.BarSeriesService;
import nu.itark.frosk.service.TradingAccountService;
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

    @Autowired
    TradingAccountService tradingAccountService;

    /**
     * Gets all featured strategies, hence per all strategies and all securities.
     *
     * @return
     */
    public List<TopStrategyDTO> findBestPerformingStrategies() {
        List<TopStrategyDTO> returnList = new ArrayList<>();
        featuredStrategyRepository.findBestPerformingStrategies().forEach(fs->{
                returnList.add(getDTO(fs));
        });
        return returnList;
    }

    public TopStrategyDTO getDTO(TopStrategy ts) {
        return TopStrategyDTO.builder()
                .name(ts.getName())
                .totalProfit(BigDecimal.valueOf(ts.getTotalProfit().doubleValue()).round(new MathContext(2)))
                .sqn(ts.getSqn().abs().doubleValue() < 100 ? BigDecimal.valueOf(ts.getSqn().doubleValue()).round(new MathContext(2)): BigDecimal.valueOf(0L))
                .sqnRaw(ts.getSqn())
                .build();
    }

    public TotalTradingDTO getTradingInfo() {
        TradingAccount tradingAccount = tradingAccountService.getTradingAccount();
        return TotalTradingDTO.builder()
                .createDate(tradingAccount.getCreateDate())
                .initTotalValue(tradingAccount.getInitTotalValue())
                .positionValue(tradingAccount.getPositionValue())
                .securityValue(tradingAccount.getTotalValue())
                .totalValue(tradingAccount.getTotalValue())
                .totalReturnPercentage(tradingAccount.getTotalReturnPercentage())
                .build();
    }

}
