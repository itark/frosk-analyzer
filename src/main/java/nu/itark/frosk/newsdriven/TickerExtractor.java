package nu.itark.frosk.newsdriven;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Best-effort extraction of a known OMX ticker symbol from free-text news
 * headline/body. Matches against the security universe in the database
 * (Swedish {@code .ST} names), falling back to a hardcoded OMX30-ish list
 * when the database has none (e.g. a fresh/test database).
 *
 * <p>Matching is on the company-name fragment derived from the ticker (e.g.
 * {@code VOLV-B.ST} → {@code "volvo"} is not derivable from the ticker alone,
 * so this matches on the ticker's own symbol fragments — {@code "volv"},
 * {@code "volv b"} — as whole words, which is what Nasdaq Nordic headlines
 * typically carry in parentheses, e.g. "(VOLV B)").
 */
@Component
@Slf4j
public class TickerExtractor {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private static final List<String> FALLBACK_OMX_TICKERS = List.of(
            "VOLV-B.ST", "ERIC-B.ST", "HM-B.ST", "ATCO-A.ST", "ATCO-B.ST",
            "SEB-A.ST", "SWED-A.ST", "SHB-A.ST", "NDA-SE.ST", "INVE-B.ST",
            "SAND.ST", "ALFA.ST", "ASSA-B.ST", "ABB.ST", "TELIA.ST",
            "ESSITY-B.ST", "SKF-B.ST", "EVO.ST", "HEXA-B.ST", "BOL.ST",
            "GETI-B.ST", "ELUX-B.ST", "KINV-B.ST", "SCA-B.ST", "SINCH.ST",
            "EQT.ST", "LIFCO-B.ST", "NIBE-B.ST", "TEL2-B.ST", "SBB-B.ST"
    );

    @Autowired
    private SecurityRepository securityRepository;

    private volatile Map<String, Pattern> tickerPatterns = Map.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public Optional<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        for (Map.Entry<String, Pattern> entry : tickerPatterns().entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private Map<String, Pattern> tickerPatterns() {
        if (Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0 && !tickerPatterns.isEmpty()) {
            return tickerPatterns;
        }
        List<String> tickers = securityRepository.findAll().stream()
                .map(s -> s.getName())
                .filter(name -> name != null && name.endsWith(".ST"))
                .distinct()
                .toList();
        if (tickers.isEmpty()) {
            tickers = FALLBACK_OMX_TICKERS;
        }

        Map<String, Pattern> patterns = new LinkedHashMap<>();
        for (String ticker : tickers) {
            // "VOLV-B.ST" -> match "VOLV-B", "VOLV B" or "VOLV" as a whole word
            String base = ticker.substring(0, ticker.length() - ".ST".length());
            String root = base.contains("-") ? base.substring(0, base.indexOf('-')) : base;
            String pattern = "\\b(" + Pattern.quote(base) + "|" + Pattern.quote(base.replace('-', ' '))
                    + "|" + Pattern.quote(root) + ")\\b";
            patterns.put(ticker, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        }

        tickerPatterns = patterns;
        cachedAt = Instant.now();
        log.debug("TickerExtractor: refreshed {} ticker patterns", patterns.size());
        return patterns;
    }
}
