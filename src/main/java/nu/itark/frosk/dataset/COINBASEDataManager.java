package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Candle;
import nu.itark.frosk.crypto.coinbase.model.Candles;
import nu.itark.frosk.crypto.coinbase.model.Granularity;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.model.SecurityPrice;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;
import nu.itark.frosk.util.DateTimeManager;
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
        log.info("sync=" + Database.COINBASE.toString()+ " on EUR");
        List<Security> securities = securityRepository.findByDatabaseAndActiveAndQuoteCurrency(Database.COINBASE.toString(), true, "EUR");
        log.info("About to sync {} active securities", securities.size());
        if(securities.size() == 0) {
            log.error("Something is wrong with securities, total rows of securities:{}", securityRepository.count());
        }

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
                log.error("Delivered duplicates on : " + byId.get().getName() + ", continues....");
                //sort of ok, continue
            }
        });

    }

    /**
     * Download prices and insert into database for one security
     */
    public void syncronize(String sec) {
        log.info("sync=" + Database.COINBASE.toString());
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

        log.info("1. {} candles to update", currencyCandlesMap.size());

        currencyCandlesMap.forEach((sec_id, candleList) -> {

            if (candleList.isEmpty()) {
                Security security = securityRepository.findById(sec_id).get();
                log.info("No candles for:{}", security.getName());
                security.setActive(false);
                securityRepository.saveAndFlush(security);
            } else {
                Security security = securityRepository.findById(sec_id).get();
                log.info("Candles exist for:{}", security.getName());
                security.setActive(true);
                securityRepository.saveAndFlush(security);
            }

            candleList.forEach(row -> {
                Date date = Date.from(row.getStart());
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

        log.info("2. {} candles to update", sp.size());

        return sp;
    }

    public Map<Long, List<Candle>> getCandles(Iterable<Security> securities, Granularity granularity) throws IOException {
        if (granularity.equals(Granularity.ONE_DAY) || granularity.equals(Granularity.FIFTEEN_MINUTE)) {
        } else {
            throw new RuntimeException("Granularity not supported!");
        }

        Map<Long, List<Candle>> candlesMap = new HashMap<Long, List<Candle>>();
        Instant endTime = DateTimeManager.truncatedToDays(Instant.now());
        final Iterator<Security> securityIterator = securities.iterator();

        Security security = null;
        while (securityIterator.hasNext()) {
            security = securityIterator.next();
            Instant startTime = DateTimeManager.truncatedToDays(Instant.now());
            SecurityPrice topSp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());
            if (Objects.nonNull(topSp)) {
                Date lastDate = topSp.getTimestamp();
                if (DateUtils.isSameInstant(Date.from(startTime), lastDate)) {
                    continue;
                }
                if (lastDate.toInstant().isBefore(startTime)) {
                    startTime = (Instant) lastDate.toInstant().adjustInto(startTime);
                    startTime = startTime.plus(1, ChronoUnit.DAYS);

                } else {
                    startTime = (Instant) lastDate.toInstant().adjustInto(startTime);
                    startTime = startTime.plus(1, ChronoUnit.DAYS);
                }
            } else {
                startTime = startTime.minus(nrOfCandles, ChronoUnit.DAYS);
            }
            try {
                //https://docs.cloud.coinbase.com/exchange/docs/rest-rate-limits
                Candles candles = productProxy.getCandles(security.getName(), startTime, endTime, granularity);
                ThreadUtils.sleep(Duration.ofMillis(1000));
                candlesMap.put(security.getId(), candles.getCandles());
            } catch (Exception e) {
                log.error("Could not get candles for:" + security.getName() + ", continue...");
            }


        }
        return candlesMap;
    }

    private Map<Long, List<Candle>> getCandlesOLD(Iterable<Security> securities, Granularity granularity) throws IOException {
        Map<Long, List<Candle>> candlesMap = new HashMap<Long, List<Candle>>();
        Instant endTime = Instant.now();

        if (granularity.equals(Granularity.ONE_DAY)) {
            log.info("japp");
            Instant startTime = DateTimeManager.truncatedToDays(Instant.now());
        } else {
            throw new RuntimeException("Granularity not supported!");
        }


        securities.forEach(security -> {
            //Instant startTime = Instant.now();
            Instant startTime = DateTimeManager.truncatedToDays(Instant.now());
            SecurityPrice topSp = securityPriceRepository.findTopBySecurityIdOrderByTimestampDesc(security.getId());

            if (Objects.nonNull(topSp)) {
                Date lastDate = topSp.getTimestamp();
                if (lastDate.toInstant().isBefore(startTime)) {
                    startTime = (Instant) lastDate.toInstant().adjustInto(startTime);
                } else {
                    startTime = (Instant) lastDate.toInstant().adjustInto(startTime);
                    startTime = startTime.plus(1, ChronoUnit.DAYS);
                }
            } else {
                startTime = startTime.minus(nrOfCandles, ChronoUnit.DAYS);
            }

            try {
                Candles candles = productProxy.getCandles(security.getName(), startTime, endTime, granularity);
                candlesMap.put(security.getId(), candles.getCandles());
            } catch (Exception e) {
                log.error("Could not get candles for:" + security.getName() + ", continue...");
            }
        });
        return candlesMap;
    }

}
