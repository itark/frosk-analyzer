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
import org.apache.commons.lang3.ThreadUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class COINBASEDataManager {

    @Value("${frosk.download.years}")
    public int noOfYears;

    @Value("${frosk.download.candles}")
    public int nrOfCandles;

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
                final Optional<Security> byId = securityRepository.findById(sp.getSecurityId());
                log.error("Delivered duplicates on : "+ byId.get().getName() + ", continues....");
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
        List<SecurityPrice> sp = new ArrayList<>();
        Map<Long, List<Candle>> currencyCandlesMap = getCandles(securities, Granularity.ONE_DAY);

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

    private Map<Long, List<Candle>> getCandles(Iterable<Security> securities, Granularity granularity) throws IOException {
        Map<Long, List<Candle>> candlesMap = new HashMap<Long, List<Candle>>();
        Instant endTime = Instant.now();
        int count = 0;
        final Iterator<Security> securityIterator = securities.iterator();

        Security security = null;
        //ThreadUtils.sleep(Duration.ofSeconds(12));
        LocalDateTime twoSecondsLater = LocalDateTime.now().plusSeconds(2);
        while (securityIterator.hasNext()) {
            security = securityIterator.next();
            Instant startTime = Instant.now();
            SecurityPrice topSp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
            if (Objects.nonNull(topSp)) {
                Date lastDate = topSp.getTimestamp();
                if (lastDate.toInstant().isBefore(startTime)) {
                    startTime = (Instant) lastDate.toInstant().adjustInto(startTime);
                    if (DateUtils.isSameInstant(Date.from(startTime),lastDate )){
                        continue;
                    }
                } else {
                    startTime = (Instant) lastDate.toInstant().adjustInto(startTime);
                    startTime = startTime.plus(1, ChronoUnit.DAYS);
                }
            } else {
                startTime=  startTime.minus(nrOfCandles, ChronoUnit.DAYS);
            }
            try {
                //https://docs.cloud.coinbase.com/exchange/docs/rest-rate-limits
                Candles candles = productProxy.getCandles(security.getName(), startTime,endTime, granularity );
                while (LocalDateTime.now().isAfter(twoSecondsLater)) {
                    ThreadUtils.sleep(Duration.ofSeconds(2));
                    twoSecondsLater = LocalDateTime.now().plusSeconds(2);
                }
                candlesMap.put(security.getId(), candles.getCandleList());
            } catch (Exception e) {
                log.error("Could not get candles for:"+security.getName()+", continue...");
            }


        }
        return candlesMap;
    }

    private Map<Long, List<Candle>> getCandlesORG(Iterable<Security> securities, Granularity granularity) throws IOException {
        Map<Long, List<Candle>> candlesMap = new HashMap<Long, List<Candle>>();
        Instant endTime = Instant.now();
        int count = 0;

        securities.forEach((security) -> {
            Instant startTime = Instant.now();
            SecurityPrice topSp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
            if (Objects.nonNull(topSp)) {
                Date lastDate = topSp.getTimestamp();
                if (lastDate.toInstant().isBefore(startTime)) {
                    startTime = (Instant) lastDate.toInstant().adjustInto(startTime);
                    if (DateUtils.isSameInstant(Date.from(startTime),lastDate )){
                        return;
                    }
                } else {
                    startTime = (Instant) lastDate.toInstant().adjustInto(startTime);
                    startTime = startTime.plus(1, ChronoUnit.DAYS);
                }
            } else {
                startTime=  startTime.minus(nrOfCandles, ChronoUnit.DAYS);
            }
            // log.info("Retrieving candles for " + security.getName() + " startTime " + startTime);
            try {
                Candles candles = productProxy.getCandles(security.getName(), startTime,endTime, granularity );
                candlesMap.put(security.getId(), candles.getCandleList());
            } catch (Exception e) {
                log.error("Could not get candles for:"+security.getName()+", continue...");
            }
        });
        return candlesMap;
    }




}
