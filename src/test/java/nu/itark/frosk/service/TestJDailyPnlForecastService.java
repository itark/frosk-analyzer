package nu.itark.frosk.service;

import nu.itark.frosk.forecast.DailyPnlPoint;
import nu.itark.frosk.forecast.ForecastPoint;
import nu.itark.frosk.forecast.ProphetForecastResponse;
import nu.itark.frosk.forecast.ProphetServiceClient;
import nu.itark.frosk.repo.DailyPnlRow;
import nu.itark.frosk.repo.StrategyTradeRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Plain unit test, no Spring context — DailyPnlForecastService is a pure
 * constructor-injected POJO service, same style as TestJRegimeForecastService.
 */
public class TestJDailyPnlForecastService {

    private static DailyPnlRow row(String date, double pnl) {
        DailyPnlRow row = mock(DailyPnlRow.class);
        when(row.getTradeDate()).thenReturn(Date.valueOf(date));
        when(row.getDailyPnl()).thenReturn(BigDecimal.valueOf(pnl));
        return row;
    }

    private static ProphetForecastResponse response() {
        return new ProphetForecastResponse(
                List.of(new ForecastPoint("2026-09-08", 3.2, -10.0, 16.4)),
                14, 60, true);
    }

    @Test
    public void disabled_neverQueriesDbOrCallsClient_andReturnsEmpty() {
        ProphetServiceClient client = mock(ProphetServiceClient.class);
        StrategyTradeRepository repo = mock(StrategyTradeRepository.class);
        DailyPnlForecastService service = new DailyPnlForecastService(client, repo, false);

        Optional<ProphetForecastResponse> result = service.getForecast("AnyStrategy", 14);

        assertTrue(result.isEmpty());
        verifyNoInteractions(client);
        verifyNoInteractions(repo);
        assertEquals(false, service.isEnabled());
    }

    @Test
    public void enabled_clientUnavailable_failsClosedGracefully() {
        ProphetServiceClient client = mock(ProphetServiceClient.class);
        StrategyTradeRepository repo = mock(StrategyTradeRepository.class);
        List<DailyPnlRow> rows = List.of(row("2026-09-01", 5.0));
        when(repo.findDailyPnlByStrategyName(anyString())).thenReturn(rows);
        when(client.fetchForecast(anyString(), any(), anyInt())).thenReturn(Optional.empty());

        DailyPnlForecastService service = new DailyPnlForecastService(client, repo, true);

        Optional<ProphetForecastResponse> result = service.getForecast("ThinStrategy", 14);

        assertTrue(result.isEmpty());
    }

    @Test
    public void enabled_success_aggregatesRowsAndReturnsResponse() {
        ProphetServiceClient client = mock(ProphetServiceClient.class);
        StrategyTradeRepository repo = mock(StrategyTradeRepository.class);
        List<DailyPnlRow> rows = List.of(row("2026-09-01", 5.0), row("2026-09-02", -3.5));
        List<DailyPnlPoint> expectedPoints = List.of(
                new DailyPnlPoint("2026-09-01", 5.0),
                new DailyPnlPoint("2026-09-02", -3.5));
        when(repo.findDailyPnlByStrategyName("STLT")).thenReturn(rows);
        when(client.fetchForecast("STLT", expectedPoints, 14))
                .thenReturn(Optional.of(response()));

        DailyPnlForecastService service = new DailyPnlForecastService(client, repo, true);

        Optional<ProphetForecastResponse> result = service.getForecast("STLT", 14);

        assertTrue(result.isPresent());
        assertEquals(60, result.get().dataPointsUsed());
        assertTrue(result.get().sufficientData());
    }

    @Test
    public void resultIsCached_secondCallForSameDayDoesNotHitDbOrClientAgain() {
        ProphetServiceClient client = mock(ProphetServiceClient.class);
        StrategyTradeRepository repo = mock(StrategyTradeRepository.class);
        List<DailyPnlRow> rows = List.of(row("2026-09-01", 5.0));
        when(repo.findDailyPnlByStrategyName(anyString())).thenReturn(rows);
        when(client.fetchForecast(anyString(), any(), anyInt())).thenReturn(Optional.of(response()));

        DailyPnlForecastService service = new DailyPnlForecastService(client, repo, true);

        Optional<ProphetForecastResponse> first = service.getForecast("STLT", 14);
        Optional<ProphetForecastResponse> second = service.getForecast("STLT", 14);

        assertEquals(first, second);
        verify(repo, times(1)).findDailyPnlByStrategyName(anyString());
        verify(client, times(1)).fetchForecast(anyString(), any(), anyInt());
    }
}
