package nu.itark.frosk.newsdriven;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rule-based sentiment scoring for news headlines.
 *
 * <p>Returns an integer score in [-5, +5]. Positive scores indicate
 * bullish news, negative scores bearish. Used by {@link NewsService}
 * to filter actionable headlines for intraday trading signals.
 */
@Component
public class KeywordSentimentAnalyzer {

    private static final Map<String, Integer> SCORES = new LinkedHashMap<>();

    static {
        // Strong positive (+3)
        SCORES.put("raises guidance",       +3);
        SCORES.put("beats estimates",       +3);
        SCORES.put("record revenue",        +3);
        SCORES.put("höjer prognos",         +3);
        SCORES.put("rekordresultat",        +3);
        SCORES.put("överträffar",           +3);
        SCORES.put("förvärvar",             +3);
        SCORES.put("vinner order",          +3);
        SCORES.put("stororder",             +3);
        SCORES.put("rekordhög",             +3);

        // Medium positive (+2)
        SCORES.put("acquisition",           +2);
        SCORES.put("upgrade",               +2);
        SCORES.put("strong quarter",        +2);
        SCORES.put("uppgraderar",           +2);
        SCORES.put("rekar köp",             +2);
        SCORES.put("vinst över förväntan",  +2);
        SCORES.put("höjer utdelning",       +2);
        SCORES.put("ny order",              +2);
        SCORES.put("ramavtal",              +2);
        SCORES.put("kontrakt",              +2);
        // Swedish quarterly/annual reporting signals
        SCORES.put("delårsrapport",         +2);
        SCORES.put("bokslutskommuniké",     +2);
        SCORES.put("kvartalsrapport",       +2);
        SCORES.put("interim report",        +2);
        // Market analyst signals
        SCORES.put("höjer riktkurs",        +2);
        SCORES.put("köprekommendation",     +2);
        SCORES.put("outperform",            +2);
        SCORES.put("buy rating",            +2);

        // Weak positive (+1)
        SCORES.put("positive",              +1);
        SCORES.put("strong",                +1);
        SCORES.put("growth",                +1);
        SCORES.put("positiv",               +1);
        SCORES.put("stark",                 +1);
        SCORES.put("tillväxt",              +1);
        SCORES.put("ökar",                  +1);
        SCORES.put("förbättrar",            +1);
        SCORES.put("rapport",               +1);
        SCORES.put("avtal",                 +1);
        SCORES.put("order",                 +1);
        SCORES.put("launches",              +1);
        SCORES.put("expands",               +1);

        // Strong negative (-3)
        SCORES.put("profit warning",        -3);
        SCORES.put("misses estimates",      -3);
        SCORES.put("vinstvarning",          -3);
        SCORES.put("sänker prognos",        -3);

        // Medium negative (-2)
        SCORES.put("downgrades",            -2);
        SCORES.put("loss",                  -2);
        SCORES.put("nedgraderar",           -2);
        SCORES.put("förlust",               -2);
        SCORES.put("varning",               -2);

        // Weak negative (-1)
        SCORES.put("weak",                  -1);
        SCORES.put("decline",               -1);
        SCORES.put("svag",                  -1);
        SCORES.put("minskar",               -1);
        SCORES.put("negativ",               -1);
    }

    /**
     * Scores a single headline.
     *
     * @param headline raw news title
     * @return sentiment score in [-5, +5]
     */
    public int analyze(String headline) {
        if (headline == null || headline.isBlank()) return 0;
        String lower = headline.toLowerCase();
        int score = 0;
        for (Map.Entry<String, Integer> entry : SCORES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        return Math.max(-5, Math.min(5, score));
    }
}
