package nu.itark.frosk.service;

import nu.itark.frosk.analysis.IntradayTodaySignalDTO;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.controller.DataController;
import nu.itark.frosk.model.IntradayBar;
import nu.itark.frosk.model.IntradaySignal;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.IntradayBarRepository;
import nu.itark.frosk.repo.IntradaySignalRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A signal fired at the entry price must still show today's latest intraday
 * bar close as "current price" — not the price the signal fired at, and not
 * null. Guards the /intradayTodaySignals fix: currentPrice used to not exist
 * on the DTO at all, so the dashboard's "Current Price" column read blank for
 * every row regardless of intraday bar data being available.
 */
public class TestJIntradayTodaySignalsCurrentPrice extends BaseIntegrationTest {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Autowired
    DataController dataController;

    @Autowired
    SecurityRepository securityRepository;

    @Autowired
    IntradayBarRepository intradayBarRepository;

    @Autowired
    IntradaySignalRepository intradaySignalRepository;

    @Test
    public void todaysSignalReportsLatestIntradayBarCloseAsCurrentPrice() {
        Security security = null;
        IntradayBar bar = null;
        IntradaySignal signal = null;

        try {
            security = securityRepository.save(
                    new Security("TEST-CURR.ST", "Test Current Price Co", "YAHOO", "SEK"));

            // Signal fired at 145.00 — the entry/close price recorded at signal time.
            long signalTs = ZonedDateTime.now(STOCKHOLM).toEpochSecond();
            signal = intradaySignalRepository.save(new IntradaySignal(
                    "OMX30IntradayMomentumStrategy", "TEST-CURR.ST", signalTs, "BUY",
                    new BigDecimal("145.00")));

            // Market has since moved: latest 5m bar closes at 150.00 — this is "current price".
            bar = intradayBarRepository.save(new IntradayBar(
                    security.getId(), Instant.now().getEpochSecond(), "5m",
                    new BigDecimal("148.00"), new BigDecimal("151.00"), new BigDecimal("147.50"),
                    new BigDecimal("150.00"), 12345L));

            List<IntradayTodaySignalDTO> signals = dataController.getIntradayTodaySignals();

            IntradayTodaySignalDTO found = signals.stream()
                    .filter(s -> "TEST-CURR.ST".equals(s.getSecurityName()) && "BUY".equals(s.getType()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(found, "seeded signal should appear in /intradayTodaySignals");
            assertEquals(0, new BigDecimal("145.00").compareTo(found.getPrice()),
                    "price should stay the entry/signal price, price=" + found.getPrice());
            assertNotNull(found.getCurrentPrice(), "currentPrice must not be null when a bar exists");
            assertEquals(0, new BigDecimal("150.00").compareTo(found.getCurrentPrice()),
                    "currentPrice should be the latest intraday bar close, currentPrice=" + found.getCurrentPrice());
            assertTrue(found.getCurrentPrice().compareTo(found.getPrice()) != 0,
                    "current price must be distinct from the signal price, else this test proves nothing");
        } finally {
            if (signal != null) intradaySignalRepository.deleteById(signal.getId());
            if (bar != null) intradayBarRepository.deleteById(bar.getId());
            if (security != null) securityRepository.deleteById(security.getId());
        }
    }

    @Test
    public void currentPriceIsNullWhenNoIntradayBarExistsForTheSecurity() {
        Security security = null;
        IntradaySignal signal = null;

        try {
            security = securityRepository.save(
                    new Security("TEST-NOBAR.ST", "Test No Bar Co", "YAHOO", "SEK"));

            long signalTs = ZonedDateTime.now(STOCKHOLM).toEpochSecond();
            signal = intradaySignalRepository.save(new IntradaySignal(
                    "OMX30IntradayMomentumStrategy", "TEST-NOBAR.ST", signalTs, "BUY",
                    new BigDecimal("100.00")));
            // Deliberately no IntradayBar row for this security.

            List<IntradayTodaySignalDTO> signals = dataController.getIntradayTodaySignals();

            IntradayTodaySignalDTO found = signals.stream()
                    .filter(s -> "TEST-NOBAR.ST".equals(s.getSecurityName()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(found, "seeded signal should appear in /intradayTodaySignals");
            assertEquals(null, found.getCurrentPrice(),
                    "currentPrice should be null (not the signal price, not zero) when no bar data exists");
        } finally {
            if (signal != null) intradaySignalRepository.deleteById(signal.getId());
            if (security != null) securityRepository.deleteById(security.getId());
        }
    }
}
