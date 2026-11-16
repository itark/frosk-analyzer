package nu.itark.frosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.RotationHoldingDTO;
import nu.itark.frosk.analysis.SectorRotationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * S9 — Sector rotation on OMX (docs/research/omx-equity-strategies-research.md). Maps the
 * HedgeIndex tier to a cyclical / defensive / cash allocation over two fixed baskets. Read-only.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SectorRotationService {

    private final HedgeIndexService hedgeIndexService;

    @Value("${frosk.s9.cyclical:VOLV-B.ST,SKF-B.ST,ATCO-A.ST,SAND.ST}")
    private List<String> cyclicalBasket;

    @Value("${frosk.s9.defensive:INVE-B.ST,ESSITY-B.ST,AXFO.ST}")
    private List<String> defensiveBasket;

    public SectorRotationDTO rotate() {
        int score = hedgeIndexService.getScore(ZonedDateTime.now());

        // HedgeIndex tiers (see HedgeIndexService.regimeLabel): risk-on -> cyclical,
        // cautious -> 50/50, defensive -> defensive basket, deep risk-off -> cash.
        double cyclical;
        double defensive;
        double cash;
        if (score <= 3) {
            cyclical = 1.0; defensive = 0.0; cash = 0.0;
        } else if (score <= 7) {
            cyclical = 0.5; defensive = 0.5; cash = 0.0;
        } else if (score <= 11) {
            cyclical = 0.0; defensive = 1.0; cash = 0.0;
        } else {
            cyclical = 0.0; defensive = 0.0; cash = 1.0;
        }

        List<RotationHoldingDTO> holdings = new ArrayList<>();
        addBasket(holdings, "cyclical", cyclicalBasket, cyclical);
        addBasket(holdings, "defensive", defensiveBasket, defensive);

        log.info("S9 sector rotation: score={}, cyclical={}, defensive={}, cash={}",
                score, cyclical, defensive, cash);

        return SectorRotationDTO.builder()
                .date(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .hedgeIndexScore(score)
                .regime(HedgeIndexService.regimeLabel(score))
                .cyclicalWeight(cyclical)
                .defensiveWeight(defensive)
                .cashWeight(cash)
                .cyclicalBasket(cyclicalBasket)
                .defensiveBasket(defensiveBasket)
                .targetHoldings(holdings)
                .build();
    }

    private static void addBasket(List<RotationHoldingDTO> out, String basket, List<String> tickers, double totalWeight) {
        if (totalWeight <= 0 || tickers == null || tickers.isEmpty()) {
            return;
        }
        double per = totalWeight / tickers.size();
        for (String t : tickers) {
            out.add(RotationHoldingDTO.builder()
                    .basket(basket)
                    .securityName(t.trim())
                    .weight(Math.round(per * 10000.0) / 10000.0)
                    .build());
        }
    }
}
