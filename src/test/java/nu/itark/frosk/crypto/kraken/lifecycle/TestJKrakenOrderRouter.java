package nu.itark.frosk.crypto.kraken.lifecycle;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Which venue each signal reaches, per mode. The property that matters most:
 * PAPER never reaches Kraken, and exits reach every venue so a mode change
 * cannot strand an open position.
 */
public class TestJKrakenOrderRouter {

    private static final String STRATEGY = "CryptoShortIntradayStrategy";

    private final KrakenStrategyModeService modeService = mock(KrakenStrategyModeService.class);
    private final PaperKrakenOrderDispatcher paper = mock(PaperKrakenOrderDispatcher.class);
    private final LiveKrakenOrderDispatcher live = mock(LiveKrakenOrderDispatcher.class);
    private final ShadowKrakenOrderDispatcher shadow = mock(ShadowKrakenOrderDispatcher.class);
    private final KrakenOrderRouter router = new KrakenOrderRouter(modeService, paper, live, shadow);

    private static OrderRequest order(String type) {
        return new OrderRequest(type, STRATEGY, "PF_XBTUSD", BigDecimal.valueOf(60000), null, null);
    }

    private void givenMode(StrategyMode mode) {
        when(modeService.getMode(STRATEGY)).thenReturn(mode);
    }

    @Test
    void paperEntry_goesToPaperOnly_neverToKraken() {
        givenMode(StrategyMode.PAPER);
        OrderRequest o = order("SHRT");

        router.route(o);

        verify(paper).dispatch(o);
        verifyNoInteractions(live, shadow);
    }

    @Test
    void shadowEntry_goesToShadowDispatcher() {
        givenMode(StrategyMode.SHADOW);
        OrderRequest o = order("BUY");

        router.route(o);

        verify(shadow).dispatch(o);
        verifyNoInteractions(paper, live);
    }

    @Test
    void liveEntry_goesToKrakenOnly() {
        givenMode(StrategyMode.LIVE);
        OrderRequest o = order("BUY");

        router.route(o);

        verify(live).dispatch(o);
        verifyNoInteractions(paper, shadow);
    }

    @Test
    void disabledEntry_goesNowhere() {
        givenMode(StrategyMode.DISABLED);

        router.route(order("SHRT"));

        verifyNoInteractions(paper, live, shadow);
    }

    @Test
    void exitAfterRollbackToPaper_stillClosesTheLivePosition() {
        // Position opened while LIVE, strategy then rolled back: the COVR must still reach Kraken.
        givenMode(StrategyMode.PAPER);
        OrderRequest o = order("COVR");

        router.route(o);

        verify(paper).dispatch(o, StrategyMode.PAPER);
        verify(live).dispatch(eq(o), any(StrategyMode.class));
        verify(shadow, never()).dispatch(any());
    }

    @Test
    void exitAfterPromotionToLive_stillClosesThePaperPosition() {
        givenMode(StrategyMode.LIVE);
        OrderRequest o = order("SELL");

        router.route(o);

        verify(paper).dispatch(o, StrategyMode.PAPER);
        verify(live).dispatch(o, StrategyMode.LIVE);
    }
}
