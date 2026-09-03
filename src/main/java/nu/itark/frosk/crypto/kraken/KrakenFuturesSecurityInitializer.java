package nu.itark.frosk.crypto.kraken;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

/**
 * Ensures the {@code KRAKEN} security universe exists at application start-up —
 * the {@code kraken-futures} analogue of {@link nu.itark.frosk.dataset.DataSetHelper}'s
 * {@code saveCoinbaseToRepo()}.
 *
 * <p>Two things are registered, mirroring the split on the Coinbase profile:
 * <ul>
 *   <li><b>The curated 15m whitelist</b> ({@code crypto.intraday.products}) — always
 *       ensured, even if the instruments API is unreachable, because the 15m pipeline
 *       depends on it.</li>
 *   <li><b>The full crypto-perpetual universe</b> — when
 *       {@code kraken.futures.enumerate.instruments=true}, every tradeable {@code PF_}
 *       perpetual from {@code /derivatives/api/v3/instruments} is registered as a
 *       {@code KRAKEN} security. Non-crypto instruments (tokenised equities,
 *       commodities, FX, stablecoins, indices, pre-IPO) are dropped by {@code category}.
 *       This is screening / backtest universe only — it does not put anything on the
 *       15m cycle.</li>
 * </ul>
 *
 * <p>Securities are keyed by {@code database = "KRAKEN"} with {@code active = true};
 * {@link nu.itark.frosk.service.BarSeriesService#getDataSet} and the dashboard both
 * filter on {@code (database, active)}, not on {@code DataSet} membership, so that is
 * the field that matters. (The {@code Security.@PreUpdate} enterprise-value gate
 * exempts {@code KRAKEN} for the same reason it exempts {@code COINBASE}.)
 *
 * <p>Runs in {@code @PostConstruct} — before {@code ContextRefreshedEvent}, so the
 * universe exists before {@code FroskStartupApplicationListener} kicks off the first
 * intraday sync.
 */
@Component
@Profile("kraken-futures")
@Slf4j
public class KrakenFuturesSecurityInitializer {

    private static final String DB_TAG       = "KRAKEN";
    private static final String DATASET_NAME = "KRAKEN";
    private static final String QUOTE        = "USD";

    /**
     * Instrument categories from {@code /instruments} that are not crypto directional
     * plays. Everything else ("Layer 1", "DeFi", "Meme", "AI", "Gaming", "Web3",
     * "Layer 2", "Privacy", "Real-world assets", …) is kept.
     */
    private static final Set<String> NON_CRYPTO_CATEGORIES = Set.of(
            "xStocks", "Commodities", "Forex", "Stablecoin", "Indices", "Pre-IPO");

    @Value("${crypto.intraday.products:PF_XBTUSD,PF_ETHUSD,PF_SOLUSD}")
    private List<String> intradayProducts;

    @Value("${kraken.futures.api.baseUrl:https://futures.kraken.com/derivatives/api/v3}")
    private String apiBaseUrl;

    @Value("${kraken.futures.enumerate.instruments:true}")
    private boolean enumerateInstruments;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private DataSetRepository dataSetRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void ensureSecurities() {
        if (dataSetRepository.findByName(DATASET_NAME) == null) {
            dataSetRepository.saveAndFlush(new DataSet(DATASET_NAME, DATASET_NAME));
            log.info("KrakenFuturesSecurityInitializer: created dataset '{}'", DATASET_NAME);
        }

        // The curated 15m whitelist must exist regardless of API reachability.
        int newIntraday = 0;
        for (String raw : intradayProducts) {
            String symbol = raw.trim();
            if (symbol.isEmpty()) continue;
            if (registerSymbol(symbol, symbol + " Perpetual Future")) newIntraday++;
        }
        log.info("KrakenFuturesSecurityInitializer: {} intraday-whitelist securities ensured ({} newly registered)",
                intradayProducts.size(), newIntraday);

        if (enumerateInstruments) {
            syncInstruments();
        } else {
            log.info("KrakenFuturesSecurityInitializer: instrument enumeration disabled "
                    + "(kraken.futures.enumerate.instruments=false)");
        }
    }

    /**
     * Registers every tradeable crypto {@code PF_} perpetual from the Kraken Futures
     * instruments API. Best-effort: on any failure the curated whitelist above is
     * still in place, so the 15m pipeline keeps working.
     */
    private void syncInstruments() {
        InstrumentsResponse resp;
        try {
            resp = restTemplate.getForObject(apiBaseUrl + "/instruments", InstrumentsResponse.class);
        } catch (Exception e) {
            log.warn("KrakenFuturesSecurityInitializer: /instruments fetch failed — {} "
                    + "(curated whitelist still registered)", e.toString());
            return;
        }
        if (resp == null || resp.getInstruments() == null) {
            log.warn("KrakenFuturesSecurityInitializer: /instruments returned no data");
            return;
        }

        int cryptoPerps = 0, newly = 0, skipped = 0;
        for (Instrument inst : resp.getInstruments()) {
            if (!isCryptoPerpetual(inst)) {
                skipped++;
                continue;
            }
            cryptoPerps++;
            String base = inst.getBase() != null ? inst.getBase() : inst.getSymbol();
            String desc = base + "/" + QUOTE + " Perpetual Future";
            if (registerSymbol(inst.getSymbol(), desc)) newly++;
        }
        log.info("KrakenFuturesSecurityInitializer: enumerated {} crypto perpetuals from /instruments "
                        + "({} newly registered, {} non-crypto/untradeable instruments skipped)",
                cryptoPerps, newly, skipped);
    }

    private boolean isCryptoPerpetual(Instrument inst) {
        return inst != null
                && inst.getSymbol() != null
                && inst.getSymbol().startsWith("PF_")
                && Boolean.TRUE.equals(inst.getTradeable())
                && "flexible_futures".equals(inst.getType())
                && !NON_CRYPTO_CATEGORIES.contains(inst.getCategory());
    }

    /**
     * Insert the security if missing, or re-activate it if a stray update had
     * cleared the flag. {@code active} and {@code database} are the fields the
     * strategy pipeline and dashboard filter on.
     *
     * @return {@code true} when a new row was inserted
     */
    private boolean registerSymbol(String symbol, String description) {
        Security existing = securityRepository.findByName(symbol);
        if (existing == null) {
            Security s = new Security(symbol, description, DB_TAG, QUOTE);
            s.setActive(true);
            securityRepository.save(s);
            log.info("KrakenFuturesSecurityInitializer: registered security {}", symbol);
            return true;
        }
        if (!existing.isActive()) {
            existing.setActive(true);
            securityRepository.save(existing);
            log.info("KrakenFuturesSecurityInitializer: re-activated security {}", symbol);
        }
        return false;
    }

    // ── /instruments response POJOs ─────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class InstrumentsResponse {
        private String result;
        private List<Instrument> instruments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Instrument {
        private String symbol;
        private String type;
        private Boolean tradeable;
        private String category;
        private String base;
        private String quote;
    }
}
