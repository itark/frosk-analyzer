package nu.itark.frosk.dataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.RecommendationTrend;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.rapidapi.yhfinance.model.*;
import nu.itark.frosk.repo.RecommendationTrendRepository;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * This class is responsible for retrieving and parsing data from yahoo finance.
 * <p>
 * And save to SecurityPricesRespsitory
 * <p>
 * Depended lib, see : https://financequotes-api.com
 *
 * @author fredrikmoller
 */

@Service
@Slf4j
public class YAHOODataManager {
    @Value("${frosk.download.years}")
    public int years;

    @Value("${rapid.api.enabled}")
    boolean apiEnabled;

    @Value("${enterprise.value.threshold:500000000}")
    int enterpriseValueThreshold;

    @Autowired
    SecurityPriceRepository securityPriceRepository;

    @Autowired
    SecurityRepository securityRepository;

    @Autowired
    RecommendationTrendRepository recommendationTrendRepository;

    @Autowired
    RapidApiManager rapidApiManager;

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");


    /**
     * Download prices and insert into database.
     */
    public void syncronize() {
        if (!apiEnabled) {
            log.info("sync=" + Database.YAHOO.toString() + " apiEnabled=" + apiEnabled + ", aborting.");
            return;
        }
        log.info("sync=" + Database.YAHOO.toString());
        Iterable<Security> securities = securityRepository.findByDatabaseAndActive(Database.YAHOO.toString(), true);
  //    Iterable<Security> securities = securityRepository.findTop3ByDatabaseAndActive(Database.YAHOO.toString(), true);
        securities.forEach(sc -> log.info("NAME=" + sc.getName()));

        List<SecurityPrice> spList;
        try {
            spList = getDataSet(securities);
        } catch (IOException e) {
            log.error("Could not retrieve dataset");
            throw new RuntimeException(e);
        }

        log.info("Adding:{} days", spList.size());
        spList.forEach((sp) -> {
            try {
                saveSingle(sp);
            } catch (DataIntegrityViolationException e) {
                log.error("Delivered duplicates on sp.getSecurityId(): " + sp.getSecurityId() + ", continues....");
                //sort of ok, continue
            }
        });
        log.info("Updated:{} security_price", spList.size());

    }

    /**
     * Saves a single SecurityPrice in its own transaction and commits immediately.
     * Exceptions will rollback this single transaction only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityPrice saveSingle(SecurityPrice sp) {
        return securityPriceRepository.save(sp);
    }

    /**
     * Download prices and insert into database for one security
     */
    public void syncronize(String sec) {
        log.info("sync=" + Database.YAHOO.toString());
        Security security = securityRepository.findByName(sec);

        Assert.notNull(security, "security can not be null");

        List<Security> securities = Arrays.asList(security);
        securities.forEach(sc -> log.info("NAME=" + sc.getName()));

        List<SecurityPrice> spList;
        try {
            spList = getDataSet(securities);
        } catch (IOException e) {
            log.error("Could not retrieve dataset");
            throw new RuntimeException(e);
        }

        log.info("Adding:{} days", spList.size());
        spList.forEach((sp) -> {
            try {
                saveSingle(sp);
            } catch (DataIntegrityViolationException e) {
                log.error("Duplicate ." + e);
                //continue
            }
        });

    }

    private List<SecurityPrice> getDataSet(Iterable<Security> securities) throws IOException {
        log.info("getDataSet(Iterable<Security> securities)");
        List<SecurityPrice> securityPrices = new ArrayList<>();
        final Map<Long, Collection<StockHistoryDTO.StockData>> stockQuotes = getStocks(securities);
        log.info("Added stockdata for {} stocks", stockQuotes.keySet().size());

        if (stockQuotes != null) {
            stockQuotes.forEach((sec_id, quote) -> {
                quote.forEach(row -> {
                    Date date;
                    try {
                        date = formatter.parse(row.getDate());
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    SecurityPrice securityPrice = new SecurityPrice(sec_id, date, BigDecimal.valueOf(row.getOpen()), BigDecimal.valueOf(row.getHigh()), BigDecimal.valueOf(row.getLow()),
                            BigDecimal.valueOf(row.getClose()), row.getVolume());
                    securityPrices.add(securityPrice);
                });
            });
        }
        return securityPrices;
    }

    private Map<Long, Collection<StockHistoryDTO.StockData>> getStocks(Iterable<Security> securities) {
        log.info("getStocks(Iterable<Security> securities");
        Map<Long, Collection<StockHistoryDTO.StockData>> stocks = new HashMap<>();

        securities.forEach((security) -> {
            Calendar from = Calendar.getInstance(TimeZone.getDefault());
            boolean isToday = false;
            Date toDay = new Date();
            SecurityPrice topSp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
            if (topSp != null) {
                Date lastDate = topSp.getTimestamp();
                log.info("security=" + security.getName() + ", found lastDate=" + lastDate);
                if (DateUtils.isSameDay(lastDate, toDay)) {
                    log.info("isToday ::lastDate=" + lastDate.toString() + ", toDay=" + toDay.toString());
                    isToday = true;
                } else if (DateUtils.isSameDay(lastDate, DateUtils.addDays(toDay, -1))) {
                    log.info("last is yeasterday");
                    from.setTime(lastDate);
                    from.add(Calendar.DATE, 1);
                } else {
                    from.setTime(lastDate);
                    from.add(Calendar.DATE, 1);
                    log.info("Not today, from set to:" + from.getTime().toString());
                }
            } else {
                from.add(Calendar.YEAR, -years);
            }

            if (!isToday) {
                log.info("Retrieving history for " + security.getName() + " from " + from.getTime());
                try {
                    final Map<String, StockHistoryDTO.StockData> history = rapidApiManager.getHistory(security.getName(), RapidApiManager.Interval.ONE_DAY);
                    if (Objects.nonNull(history)) {
                        Map<String, StockHistoryDTO.StockData> filterFromHistory = filterFromHistory(from, history);
                        log.info("Adding:{} stockdata for :{}", filterFromHistory.values().size(), security.getName());
                        stocks.put(security.getId(), filterFromHistory.values());
                    }
                } catch (Exception e) {
                    log.error("ERROR:", e);
                    // throw e;
                }

            } else {
                log.info("Today, no action.");
            }
        });

        return stocks;

    }

    private Map<String, StockHistoryDTO.StockData> filterFromHistory(Calendar from, Map<String, StockHistoryDTO.StockData> history) {
        return history.entrySet().stream()
                .filter(entry -> {
                    try {
                        Date date = formatter.parse(entry.getValue().getDate());
                        return date.after(from.getTime());
                    } catch (ParseException e) {
                        throw new RuntimeException("Invalid date format: " + entry.getValue().getDate(), e);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public void updateSecurityMetaData() {
        if (!apiEnabled) {
            log.info("updateSecurityMetaData, apiEnabled=" + apiEnabled + ", aborting.");
            return;
        }
        Iterable<Security> securities = securityRepository.findByDatabaseAndActive(Database.YAHOO.toString(), true);
        securities.forEach((security -> {
            if (security.getName().contains("^") || security.getName().contains("=")) return;
            updateWithMetaData(security);
        }));
    }

    public void updateSecurityMetaData(String securityName) {
        if (!apiEnabled) {
            log.info("updateSecurityMetaData, apiEnabled=" + apiEnabled + ", aborting.");
            return;
        }
        Security security = securityRepository.findByName(securityName);
        updateWithMetaData(security);
    }

    /**
     * Includes call to Yahoo Finance using RapidApiManager
     *
     * @param security
     */
    void updateWithMetaData(Security security) {
        setIncomeStatementData(security);
        setStatisticsData(security);
        setRecommendationTrend(security);

        securityRepository.save(security);
    }

    private void setStatisticsData(Security security) {
        double pegRatio = 0.0;
        double beta = 0.0;
        double trailingEps = 0.0;
        double forwardEps = 0.0;
        double trailingPe = 0.0;
        double forwardPe = 0.0;

        double forwardPERaw = 0.0;
        double forwardEpsRaw = 0.0;
        double trailingEpsRaw = 0.0;

        long enterpriseValueRaw = 0;

        StatisticsBody moduleStatistics;
        try {
            moduleStatistics = rapidApiManager.getModuleStatistics(security.getName());
            if (moduleStatistics != null && moduleStatistics.getPegRatio().length == 0) {
                // This gives only 1-year growth estimate
                if (moduleStatistics.getForwardPE() != null && moduleStatistics.getForwardEps() != null) {

                    Object forwardPEObj = moduleStatistics.getForwardPE();
                    Object forwardEpsObj = moduleStatistics.getForwardEps();
                    Object trailingEpsObj = moduleStatistics.getTrailingEps();

                    if (forwardPEObj instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) forwardPEObj;
                        Object rawValue = map.get("raw");
                        if (rawValue instanceof Number) {
                            forwardPERaw = ((Number) rawValue).doubleValue();
                            forwardPe = forwardPERaw;
                        } else {
                            throw new IllegalArgumentException("Expected 'raw' to be a number, got: " + rawValue);
                        }
                    }
                    if (forwardEpsObj instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) forwardEpsObj;
                        Object rawValue = map.get("raw");
                        if (rawValue instanceof Number) {
                            forwardEpsRaw = ((Number) rawValue).doubleValue();
                            forwardEps = forwardEpsRaw;
                        } else {
                            throw new IllegalArgumentException("Expected 'raw' to be a number, got: " + rawValue);
                        }
                    }
                    if (trailingEpsObj instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) trailingEpsObj;
                        Object rawValue = map.get("raw");
                        if (rawValue instanceof Number) {
                            trailingEpsRaw = ((Number) rawValue).doubleValue();
                            trailingEps = trailingEpsRaw;
                        } else {
                            throw new IllegalArgumentException("Expected 'raw' to be a number, got: " + rawValue);
                        }
                    }
                    double oneYearGrowthRate = ((forwardEpsRaw - trailingEpsRaw) / trailingEpsRaw) * 100;
                    pegRatio = forwardPERaw / oneYearGrowthRate;
                }
            } else {
                pegRatio = (double) moduleStatistics.getPegRatio()[0];
            }
            if (moduleStatistics.getBeta() != null) {
                Object betaObj = moduleStatistics.getBeta();
                if (betaObj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) betaObj;
                    Object rawValue = map.get("raw");
                    if (rawValue instanceof Number) {
                        beta = ((Number) rawValue).doubleValue();
                    } else {
                        throw new IllegalArgumentException("Expected 'raw' to be a number, got: " + rawValue);
                    }
                }
            }
            if (moduleStatistics.getTrailingEps() != null) {
                Object trailingEpsObj = moduleStatistics.getTrailingEps();
                if (trailingEpsObj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) trailingEpsObj;
                    Object rawValue = map.get("raw");
                    if (rawValue instanceof Number) {
                        trailingEps = ((Number) rawValue).doubleValue();
                    } else {
                        throw new IllegalArgumentException("Expected 'raw' to be a number, got: " + rawValue);
                    }
                }
            }
            if (moduleStatistics.getTrailingPE() != null) {
                Object trailingPEObj = moduleStatistics.getTrailingPE();
                if (trailingPEObj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) trailingPEObj;
                    Object rawValue = map.get("raw");
                    if (rawValue instanceof Number) {
                        trailingPe = ((Number) rawValue).doubleValue();
                    } else {
                        throw new IllegalArgumentException("Expected 'raw' to be a number, got: " + rawValue);
                    }
                }
            }
            if (moduleStatistics.getEnterpriseValue() != null) {
                FinancialValue enterpriseValue = moduleStatistics.getEnterpriseValue();
                enterpriseValueRaw  = enterpriseValue.getRaw();
            }
        } catch (Exception e) {
            log.error("Error in Statistics for:{}, error:{}", security.getName(), e.getMessage());
        }
        security.setPegRatio(pegRatio);
        security.setBeta(beta);
        security.setTrailingEps(trailingEps);
        security.setForwardEps(forwardEps);
        security.setTrailingPe(trailingPe);
        security.setForwardPe(forwardPe);
        security.setEnterpriseValue(enterpriseValueRaw);
        if (enterpriseValueRaw < enterpriseValueThreshold) {
            security.setActive(false);
        }
    }

    private void setIncomeStatementData(Security security) {
        double yoyGrowth = 0;
        try {
            Body module = rapidApiManager.getModuleIncomeStatement(security.getName());
            if (module != null && module.getIncomeStatementHistory() != null) {
                if (module.getIncomeStatementHistory().getIncomeStatementHistory().size() >= 2) {
                    double totalRevenueThisYear = module.getIncomeStatementHistory().getIncomeStatementHistory().get(0).getTotalRevenue().getRaw();
                    double totalRevenueLastYear = module.getIncomeStatementHistory().getIncomeStatementHistory().get(1).getTotalRevenue().getRaw();
                    yoyGrowth = ((totalRevenueThisYear - totalRevenueLastYear) / totalRevenueLastYear) * 100.0;
                }
            };
        } catch (Exception e) {
            log.error("Error in IncomeStatement for:{}", security, e.getMessage());
        }
        security.setYoyGrowth(yoyGrowth);
    }

    private void setRecommendationTrend(Security security) {
        recommendationTrendRepository.deleteBySecurity(security);
        try {
            RecommendationBody moduleRecommendationTrend = rapidApiManager.getModuleRecommendationTrend(security.getName());
            if (moduleRecommendationTrend != null && !moduleRecommendationTrend.getTrend().isEmpty()){
                moduleRecommendationTrend.getTrend().forEach(trend -> {
                    RecommendationTrend recommendationTrend = RecommendationTrend.builder()
                            .security(security)
                            .period(trend.getPeriod())
                            .strongBuy(trend.getStrongBuy())
                            .buy(trend.getBuy())
                            .hold(trend.getHold())
                            .sell(trend.getSell())
                            .strongSell(trend.getStrongSell())
                            .build();
                    recommendationTrendRepository.save(recommendationTrend);
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static double getDoubleFromRaw(Object rawValue) {
        if (rawValue == null) {
            return 0.0; // or Double.NaN, or throw, depending on your needs
        }
        // If it's already a Number, cast and return
        if (rawValue instanceof Number) {
            return ((Number) rawValue).doubleValue();
        }
        // If it's a String, try to parse it
        if (rawValue instanceof String) {
            try {
                return Double.parseDouble((String) rawValue);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        // Handle other unexpected types
        log.warn("Unexpected type for 'raw': " + rawValue.getClass().getSimpleName() + " value: " + rawValue);
        return 0.0;
    }

}
