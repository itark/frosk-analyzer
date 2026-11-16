package nu.itark.frosk.service;

import nu.itark.frosk.analysis.InsiderConvictionDTO;
import nu.itark.frosk.model.InsiderTrade;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the S12 clustered-insider-buying logic. */
public class TestJInsiderConviction {

    private static InsiderTrade buy(String issuer, String person, LocalDate d) {
        return trade(issuer, person, "BUY", d);
    }

    private static InsiderTrade trade(String issuer, String person, String type, LocalDate d) {
        InsiderTrade t = new InsiderTrade();
        t.setIssuer(issuer);
        t.setPdmrName(person);
        t.setTransactionType(type);
        t.setTransactionDate(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        t.setVolume(100.0);
        t.setPrice(10.0);
        return t;
    }

    @Test
    public void flagsIssuersWithTwoOrMoreDistinctBuyers() {
        LocalDate asOf = LocalDate.of(2026, 6, 1);
        List<InsiderTrade> buys = List.of(
                buy("Volvo AB", "Alice", asOf.minusDays(5)),
                buy("Volvo AB", "Bob", asOf.minusDays(3)),
                buy("Volvo AB", "Alice", asOf.minusDays(1)),   // same person again — still 2 distinct
                buy("Lonely AB", "Carol", asOf.minusDays(2))   // only 1 distinct buyer
        );
        List<InsiderConvictionDTO> res = InsiderConvictionService.computeConviction(buys, 2, asOf, 30);
        assertEquals(1, res.size(), "only Volvo clears the 2-buyer threshold");
        assertEquals("Volvo AB", res.get(0).getIssuer());
        assertEquals(2, res.get(0).getDistinctBuyers());
        assertTrue(res.get(0).getTotalBuyValue() > 0);
    }

    @Test
    public void ignoresTradesOutsideTheWindowAndSells() {
        LocalDate asOf = LocalDate.of(2026, 6, 1);
        List<InsiderTrade> buys = List.of(
                buy("Acme AB", "Alice", asOf.minusDays(45)),   // outside 30d window
                buy("Acme AB", "Bob", asOf.minusDays(2)),
                trade("Acme AB", "Carol", "SELL", asOf.minusDays(1)) // a sell, not a buy
        );
        List<InsiderConvictionDTO> res = InsiderConvictionService.computeConviction(buys, 2, asOf, 30);
        assertTrue(res.isEmpty(), "only one in-window distinct buyer remains after filtering");
    }
}
