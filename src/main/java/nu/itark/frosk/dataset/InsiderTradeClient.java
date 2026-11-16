package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.InsiderTrade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches Finansinspektionen's public insider (PDMR) register as CSV and parses it into
 * {@link InsiderTrade} rows — S12 in {@code docs/research/omx-equity-strategies-research.md}.
 *
 * <p><b>NOTE:</b> FI's export endpoint and column layout are not verified against the live
 * service here — the URL and header names are configurable, and the parser maps by (Swedish)
 * header name and tolerates missing columns. Any failure yields an empty list — insider data
 * is a slow, supplementary signal and must never break a sync.
 */
@Component
@Slf4j
public class InsiderTradeClient {

    @Value("${frosk.insider.fi.export.url:https://marknadssok.fi.se/publiceringsklient/sv-SE/Search/Search?SearchFunctionType=Insyn&button=export&Page=1}")
    private String exportUrl;

    @Value("${frosk.insider.fi.csv.delimiter:;}")
    private String delimiter;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Fetch and parse the register. Returns an empty list on any error. */
    public List<InsiderTrade> fetchRecent() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(exportUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "text/csv,text/plain,*/*")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().isBlank()) {
                log.warn("InsiderTradeClient: FI export returned status={} (empty body?)", resp.statusCode());
                return List.of();
            }
            return parseCsv(resp.body());
        } catch (Exception e) {
            log.warn("InsiderTradeClient: FI export fetch/parse failed — {}", e.toString());
            return List.of();
        }
    }

    /** Package-visible for testing: parse FI's semicolon-delimited CSV (header-mapped). */
    List<InsiderTrade> parseCsv(String csv) {
        List<InsiderTrade> out = new ArrayList<>();
        String[] lines = csv.replace("\r", "").split("\n");
        if (lines.length < 2) {
            return out;
        }
        String[] headers = lines[0].split(delimiter, -1);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            col.put(headers[i].trim().toLowerCase(), i);
        }
        for (int r = 1; r < lines.length; r++) {
            String line = lines[r];
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(delimiter, -1);
            InsiderTrade t = new InsiderTrade();
            t.setIssuer(get(f, col, "utgivare"));
            t.setPdmrName(get(f, col, "person i ledande ställning"));
            t.setInstrumentName(get(f, col, "instrumentnamn"));
            String karaktar = get(f, col, "karaktär");
            t.setTransactionType(karaktar != null && karaktar.toLowerCase().startsWith("förvärv") ? "BUY" : "SELL");
            t.setTransactionDate(parseDate(get(f, col, "transaktionsdatum")));
            t.setPublicationDate(parseDate(get(f, col, "publiceringsdatum")));
            t.setVolume(parseNum(get(f, col, "volym")));
            t.setPrice(parseNum(get(f, col, "pris")));
            t.setCurrency(get(f, col, "valuta"));
            t.setExternalId(externalId(t));
            if (t.getIssuer() != null && !t.getIssuer().isBlank() && t.getTransactionDate() != null) {
                out.add(t);
            }
        }
        return out;
    }

    private static String get(String[] f, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        if (i == null || i >= f.length) {
            return null;
        }
        String v = f[i].trim();
        return v.isEmpty() ? null : v;
    }

    private static Date parseDate(String s) {
        if (s == null) return null;
        try {
            // FI dates can carry a time suffix; keep the date part only.
            LocalDate d = LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s, ISO);
            return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseNum(String s) {
        if (s == null) return null;
        try {
            return Double.parseDouble(s.replace(" ", "").replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private static String externalId(InsiderTrade t) {
        String raw = String.join("|",
                nz(t.getIssuer()), nz(t.getPdmrName()), nz(t.getInstrumentName()),
                nz(t.getTransactionType()), String.valueOf(t.getTransactionDate()),
                String.valueOf(t.getVolume()), String.valueOf(t.getPrice()));
        return Integer.toHexString(raw.hashCode());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
