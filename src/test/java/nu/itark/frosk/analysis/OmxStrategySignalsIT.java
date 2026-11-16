package nu.itark.frosk.analysis;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.service.EquityFactorScreeningService;
import nu.itark.frosk.service.InsiderConvictionService;
import nu.itark.frosk.service.MarketTimingService;
import nu.itark.frosk.service.SectorRotationService;
import nu.itark.frosk.service.TailRiskHedgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Smoke/report harness for the S3–S12 signals against the seeded DB. Read-only. */
public class OmxStrategySignalsIT extends BaseIntegrationTest {

    @Autowired MarketTimingService timing;
    @Autowired SectorRotationService rotation;
    @Autowired TailRiskHedgeService hedge;
    @Autowired EquityFactorScreeningService screening;
    @Autowired InsiderConvictionService insider;

    @Test
    public void reportSignals() {
        SeasonalTimingDTO s3 = timing.seasonal();
        MarketRegimeDTO s4 = timing.marketRegime();
        TurnOfMonthDTO s5 = timing.turnOfMonth();
        NordicCalendarDTO s8 = timing.nordicCalendar();
        BundleKopScreeningDTO s6 = screening.screenBundleKop();
        SectorRotationDTO s9 = rotation.rotate();
        TailRiskHedgeDTO s11 = hedge.hedge();
        java.util.List<InsiderConvictionDTO> s12 = insider.conviction();

        for (Object o : new Object[]{s3, s4, s5, s8, s6, s9, s11, s12}) {
            assertNotNull(o);
        }
        System.out.println("\n=== OMX STRATEGY SIGNALS ===");
        System.out.printf("S3 seasonal     : season=%s momPos=%s invested=%s (OMX mom=%.2f%%)%n",
                s3.isHalloweenSeason(), s3.isMomentumPositive(), s3.isInvested(), s3.getOmxMomentum12mPct());
        System.out.printf("S4 marketRegime : %s | exposure=%.2f tsmom=%.2f%% (bull=%s) closeVsSMA200=%.2f%% (bull=%s) volRatio=%.2f%n",
                s4.getRegime(), s4.getExposure(), s4.getTsmom252Pct(), s4.isTsmomBull(),
                s4.getCloseVsSma200Pct(), s4.isSma200Bull(), s4.getVolRatio());
        System.out.printf("S5 turnOfMonth  : inWindow=%s%n", s5.isInWindow());
        System.out.printf("S8 nordicCal    : paydayToM=%s reportMonth=%s%n", s8.isPaydayTomWindow(), s8.isReportMonth());
        System.out.printf("S6 bundleKop    : universe=%d marketBull=%s selected=%d (regime=%s)%n",
                s6.getUniverseSize(), s6.isMarketBull(), s6.getSelectedCount(), s6.getMarketRegime());
        s6.getSelected().stream().limit(8).forEach(c ->
                System.out.printf("                   %-14s composite=%5.2f%n", c.getSecurityName(), c.getComposite()));
        System.out.printf("S9 rotation     : score=%d regime=%s cyc=%.2f def=%.2f cash=%.2f holdings=%d%n",
                s9.getHedgeIndexScore(), s9.getRegime(), s9.getCyclicalWeight(), s9.getDefensiveWeight(),
                s9.getCashWeight(), s9.getTargetHoldings().size());
        System.out.printf("S11 tailHedge   : score=%d regime=%s hedge=%.1f%% instrument=%s%n",
                s11.getHedgeIndexScore(), s11.getRegime(), s11.getHedgeRatioPct(), s11.getHedgeInstrument());
        System.out.printf("S12 insider     : conviction issuers=%d%n", s12.size());
    }
}
