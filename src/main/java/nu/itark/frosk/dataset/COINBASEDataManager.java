package nu.itark.frosk.dataset;

import com.coinbase.exchange.model.Candle;
import com.coinbase.exchange.model.Candles;
import com.coinbase.exchange.model.Granularity;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class COINBASEDataManager {

    @Value("${frosk.download.years}")
    public int years;

    @Autowired
    SecurityPriceRepository securityPriceRepository;

    @Autowired
    SecurityRepository securityRepository;

    @Autowired
    ProductProxy productProxy;

    /**
     * Download prices and insert into database.
     */
    public void syncronize() {
        log.info("sync="+Database.COINBASE.toString());
        Iterable<Security> securities = securityRepository.findByDatabase(Database.COINBASE.toString());

        securities.forEach(sec -> log.info("NAME="+ sec.getName()));

        List<SecurityPrice> spList;
        try {
            spList = getDataSet(securities);
        } catch (IOException e) {
            log.error("Could not retrieve dataset");
            throw new RuntimeException(e);
        }

        spList.forEach((sp) -> {
            try {
                securityPriceRepository.save(sp);
            } catch (DataIntegrityViolationException e) {
                log.error("Delivered duplicates on sp.getSecurityId(): "+ sp.getSecurityId()+ ", continues....");
                //sort of ok, continue
            }
        });

    }

    /**
     * Download prices and insert into database for one security
     */
    public void syncronize(String sec) {
        log.info("sync="+Database.COINBASE.toString());
        Security security = securityRepository.findByName(sec);
        Assert.notNull(security, "security can not be null");
        List<Security> securities = Arrays.asList(security);
        List<SecurityPrice> spList;

        try {
            spList = getDataSet(securities);
        } catch (IOException e) {
            log.error("Could not retrieve dataset");
            throw new RuntimeException(e);
        }

        spList.forEach((sp) -> {
            try {
                securityPriceRepository.save(sp);
            } catch (DataIntegrityViolationException e) {
                log.error("Duplicate ." + e);
                //continue
            }
        });
    }

    private List<SecurityPrice> getDataSet(Iterable<Security> securities) throws IOException {
        log.info("getDataSet(Iterable<Security> names)");
        List<SecurityPrice> sp = new ArrayList<>();
        Map<Long, List<Candle>> currencyCandlesMap = getCandles(securities);

        currencyCandlesMap.forEach((sec_id, candleList) -> {
            candleList.forEach(row -> {
                Date date = Date.from(row.getTime());
                SecurityPrice securityPrice = null;
                if (date != null && row.getOpen() != null && row.getHigh() != null && row.getLow() != null
                        && row.getClose() != null && row.getVolume() != null) {
                    securityPrice = new SecurityPrice(sec_id,
                            date,
                            row.getOpen(),
                            row.getHigh(),
                            row.getLow(),
                            row.getClose(),
                            row.getVolume().longValue());
                    sp.add(securityPrice);
                }
            });
        });
        return sp;
    }

    private Map<Long, List<Candle>> getCandles(Iterable<Security> securities) throws IOException {
        log.info("getCurrencies(Iterable<Security> securities");
        Map<Long, List<Candle>> candlesMap = new HashMap<Long, List<Candle>>();

//        Instant startTime = Instant.now().minus(100, ChronoUnit.DAYS);
        Instant endTime = Instant.now();

        securities.forEach((security) -> {
            Instant startTime = Instant.now();
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
                    startTime.adjustInto(lastDate.toInstant());
                    startTime.plus(1, ChronoUnit.DAYS);
                } else {
                    startTime.adjustInto(lastDate.toInstant());
                    startTime.plus(1, ChronoUnit.DAYS);
                    log.info("Not today, startTime set to:" + startTime);
                }
            } else {
                startTime.plus(-years, ChronoUnit.YEARS);
            }

            if (!isToday) {
                log.info("Retrieving history for " + security.getName() + " startTime " + startTime);
                try {
                    Candles candles = productProxy.getCandles("BTC-EUR", startTime,endTime, Granularity.ONE_DAY );
                    System.out.println("candles.size:"+candles.getCandleList().size());
                    candlesMap.put(security.getId(), candles.getCandleList());
//        close=41233.35
//        high=41650.96
//        low=40143.18
//        open=40352.85
//        time=2021-09-18T00:00:00Z
//        volume=818.92870287
                } catch (Exception e) {
                    log.error("ERROR:", e);
                    // throw e;
                }

            } else {
                log.info("Today, no action.");
            }
        });
        return candlesMap;
    }
}
