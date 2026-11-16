package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.InsiderConvictionDTO;
import nu.itark.frosk.dataset.InsiderTradeClient;
import nu.itark.frosk.model.InsiderTrade;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.InsiderTradeRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * S12 — Finansinspektionen insider conviction (docs/research/omx-equity-strategies-research.md).
 *
 * <p>Clustered insider buying — ≥ {@code minBuyers} distinct insiders buying the same issuer
 * within {@code windowDays} — is a slow, fundamentally-motivated bullish signal. This service
 * syncs FI's register ({@link InsiderTradeClient}) and reports current conviction. Read-only;
 * integrates naturally as an extra vote in bundle-screening (S7).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InsiderConvictionService {

    private final InsiderTradeClient insiderTradeClient;
    private final InsiderTradeRepository insiderTradeRepository;
    private final SecurityRepository securityRepository;

    @Value("${frosk.insider.window.days:30}")
    private int windowDays;

    @Value("${frosk.insider.min.buyers:2}")
    private int minBuyers;

    /** Pull FI's register and persist new rows (deduped by externalId). Returns rows saved. */
    public int sync() {
        List<InsiderTrade> fetched = insiderTradeClient.fetchRecent();
        int saved = 0;
        for (InsiderTrade t : fetched) {
            if (t.getExternalId() != null && !insiderTradeRepository.existsByExternalId(t.getExternalId())) {
                insiderTradeRepository.save(t);
                saved++;
            }
        }
        log.info("S12 insider sync: fetched={}, saved={}", fetched.size(), saved);
        return saved;
    }

    public List<InsiderConvictionDTO> conviction() {
        LocalDate asOf = LocalDate.now();
        Date cutoff = Date.from(asOf.minusDays(windowDays).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<InsiderTrade> buys = insiderTradeRepository.findByTransactionTypeAndTransactionDateAfter("BUY", cutoff);

        List<InsiderConvictionDTO> rows = computeConviction(buys, minBuyers, asOf, windowDays);
        matchToUniverse(rows);
        log.info("S12 insider conviction: window={}d, minBuyers={}, buys={}, issuers={}",
                windowDays, minBuyers, buys.size(), rows.size());
        return rows;
    }

    /** Pure clustering: group BUYs within [asOf-windowDays, asOf] by issuer, keep issuers with >= minBuyers distinct insiders. */
    static List<InsiderConvictionDTO> computeConviction(List<InsiderTrade> buys, int minBuyers, LocalDate asOf, int windowDays) {
        LocalDate from = asOf.minusDays(windowDays);
        Map<String, List<InsiderTrade>> byIssuer = buys.stream()
                .filter(t -> "BUY".equalsIgnoreCase(t.getTransactionType()))
                .filter(t -> t.getIssuer() != null && !t.getIssuer().isBlank())
                .filter(t -> {
                    LocalDate d = toLocalDate(t.getTransactionDate());
                    return d != null && !d.isBefore(from) && !d.isAfter(asOf);
                })
                .collect(Collectors.groupingBy(t -> t.getIssuer().trim()));

        List<InsiderConvictionDTO> out = new ArrayList<>();
        for (Map.Entry<String, List<InsiderTrade>> e : byIssuer.entrySet()) {
            TreeSet<String> buyers = e.getValue().stream()
                    .map(InsiderTrade::getPdmrName)
                    .filter(n -> n != null && !n.isBlank())
                    .collect(Collectors.toCollection(TreeSet::new));
            if (buyers.size() < minBuyers) {
                continue;
            }
            LocalDate first = e.getValue().stream().map(t -> toLocalDate(t.getTransactionDate()))
                    .filter(d -> d != null).min(Comparator.naturalOrder()).orElse(null);
            LocalDate last = e.getValue().stream().map(t -> toLocalDate(t.getTransactionDate()))
                    .filter(d -> d != null).max(Comparator.naturalOrder()).orElse(null);
            double totalValue = e.getValue().stream()
                    .filter(t -> t.getVolume() != null && t.getPrice() != null)
                    .mapToDouble(t -> t.getVolume() * t.getPrice()).sum();
            out.add(InsiderConvictionDTO.builder()
                    .issuer(e.getKey())
                    .distinctBuyers(buyers.size())
                    .buyerNames(new ArrayList<>(buyers))
                    .firstBuyDate(first == null ? null : first.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .lastBuyDate(last == null ? null : last.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .totalBuyValue(Math.round(totalValue * 100.0) / 100.0)
                    .build());
        }
        out.sort(Comparator.comparingInt(InsiderConvictionDTO::getDistinctBuyers).reversed()
                .thenComparing(InsiderConvictionDTO::getIssuer));
        return out;
    }

    /** Best-effort: match FI issuer names to a Security by description (case-insensitive). */
    private void matchToUniverse(List<InsiderConvictionDTO> rows) {
        List<Security> universe = securityRepository.findByDatabaseAndActive("YAHOO", true);
        for (InsiderConvictionDTO r : rows) {
            String issuer = r.getIssuer().toLowerCase();
            universe.stream()
                    .filter(s -> s.getDescription() != null
                            && (s.getDescription().toLowerCase().contains(issuer)
                                || issuer.contains(s.getDescription().toLowerCase())))
                    .findFirst()
                    .ifPresent(s -> r.setSecurityName(s.getName()));
        }
    }

    private static LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
