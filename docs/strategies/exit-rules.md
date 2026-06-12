# Exit Rules Inventory

Cross-strategy audit of exit rule composition, stop-loss types, HedgeIndex gating, time-based exits, and trade execution models. Updated for the 2026-06-11 PnL overhaul (SLMS, HighLander, SwedishLongTermMomentum and the three intraday strategies were redesigned; `OMX30IntradayMomentumStrategy` and `RunawayGAPIntradayStrategy` no longer exist).

## Full Matrix

| Strategy | Exit Rule | Stop-Loss | HedgeIndex Exit | Time Exit | Exec Model |
|---|---|---|---|---|---|
| ADXStrategy | ADX>20 AND +DI<-DI AND Close<SMA50 OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| CANSLIMStrategy | Close<SMA50 OR DeathCross(SMA50<SMA200) OR HedgeIndex composite exit OR StopLoss(15%) | Hard 15% (configurable) | Via composite | No | NextOpen |
| CCICorrectionStrategy | LongCCI<-100 AND ShortCCI>100 OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| ConvergenceDivergenceStrategy | NegativeDivergenceIndicator OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| DailyBreakoutStrategy | StopLoss(5-bar low) OR ProfitTarget(2:1 R:R) OR HedgeIndexRiskOff | Static (5-bar low) | Yes (>=8) | No | NextOpen |
| DailyOversoldBounceStrategy | Close>SMA20 OR RSI14>55 OR StopLoss(3-bar low) OR HedgeIndexRiskOff | Static (3-bar low) | Yes (>=8) | No | NextOpen |
| EMATenTenStrategy | EMA10(close) < EMA10(open) OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| EMATenTwentyStrategy | EMA10 < EMA20 OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| EngulfingStrategy | ChandelierExit(5,3) OR StopLoss(2%) OR TrailingStop(2%) | Dual: hard+trailing 2% | No | No | CurrentClose |
| GapReversalIntradayStrategy | GapFill(Close>prevDayClose) OR AtrStop(max(1xATR14, 0.6%)) OR MaxBarsHeld(20) | ATR with 0.6% floor | No (entry gate only, <=9) | Yes (20 bars / ~5h) | CurrentClose |
| GlobalExtremaStrategy | Close > WeekMaxPrice+0.996% OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| HaramiStrategy | ChandelierExit(22,3) OR StopLoss(2%) OR TrailingStop(2%, 5 bars) | Dual: hard+trailing 2% | No | No | NextOpen |
| HighLanderStrategy | Composite sub-strategy exit OR AtrTrailingStop(3xATR14) OR StopLoss(12%) | Hard 12% + 3xATR trail | Via composite | No | NextOpen |
| MovingMomentumStrategy | EMA9<EMA26 AND StochK<80 AND MACD<EMAMacd OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| OMXS30SwingStrategy | RSI14 crosses above 65 OR AtrStopLoss(2xATR14) OR StopGain(6%) OR MaxBarsHeld(10) | ATR-based (2x) | No | Yes (10 bars) | NextOpen |
| OpeningRangeBreakoutIntradayStrategy | ProfitTarget(1.5x OR-width) OR Close<ORLow OR MaxBarsHeld(16) OR StopLoss(0.8%) | OR-low + hard 0.8% | No (entry gate only, <=9) | Yes (16 bars / ~4h) | CurrentClose |
| RSI2Strategy | EMA5<EMA200 AND RSI2>80 AND EMA5<Close OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| RunawayGAPStrategy | TrailingStop(2%, 3-bar lookback) | Trailing 2% | No | No | NextOpen |
| ShortTermMomentumLTStrength | AtrTrailingStop(2.5xATR14) OR CrossedDown(Close,SMA50) OR HedgeIndex score>9 OR StopLoss(10%) | Hard 10% + 2.5xATR trail | Yes (>9) | No | NextOpen |
| SimpleMovingMomentumStrategy | ChandelierExit(5,3) OR StopLoss(2%) OR TrailingStop(2%) | Dual: hard+trailing 2% | No | No | NextOpen |
| SwedishLongTermMomentum | HedgeIndex score>9 OR DeathCross(SMA50<SMA200) OR ROC126<-5 OR AtrTrailingStop(3xATR14) OR CatastrophicStop(15%) | Catastrophic 15% + 3xATR trail | Yes (>9) | No | NextOpen |
| ThreeBlackWhiteStrategy | ThreeBlackCrowsIndicator OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| VWAPMeanReversionIntradayStrategy | Close>SessionVWAP OR RSI5>65 OR StopLoss(0.6%) OR MaxBarsHeld(12) | Hard 0.6% | No (entry gate only, <=9) | Yes (12 bars / ~3h) | CurrentClose |
| VWAPStrategy | Close<VWAP OR StopLoss(2%) OR StopGain(2%) | Hard 2% | No | No | NextOpen |

## Summary by Category

### Stop-Loss Coverage

| Type | Count | Strategies |
|---|---|---|
| Catastrophic stop (15%) | 10 | ADX, CCI, Convergence, EMA10/10, EMA10/20, GlobalExtrema, MovingMomentum, RSI2, SwedishLongTermMomentum, ThreeBlackWhite |
| Hard stop (fixed %) | 7 | CANSLIM (15%), HighLander (12%), ShortTermMomentumLTStrength (10%), VWAPStrategy (2%), Engulfing (2%), ORB (0.8%), VWAP-MR (0.6%) |
| ATR trailing stop (`AtrTrailingStopRule`, chandelier) | 3 | HighLander (3x), ShortTermMomentumLTStrength (2.5x), SwedishLongTermMomentum (3x) |
| ATR stop-loss (`AtrStopLossRule`, from entry) | 2 | OMXS30Swing (2x), GapReversalIntraday (1x with 0.6% floor) |
| Dual hard + trailing | 3 | Engulfing (2%), Harami (2%), SimpleMovingMomentum (2%) |
| Trailing only | 1 | RunawayGAP (2%) |
| Static price level | 2 | DailyBreakout (5-bar low), DailyOversoldBounce (3-bar low) |

`AtrStopLossRule`, `AtrTrailingStopRule`, and `MaxBarsHeldRule` are shared classes under `strategies/rules/` (extracted in the PnL overhaul; the inner-class duplicates in OMXS30Swing and the intraday strategies were removed).

### HedgeIndex Gating on Exit

Direct HedgeIndex exits (4 strategies):

- DailyBreakoutStrategy — `HedgeIndexRiskOffRule`, score >= 8
- DailyOversoldBounceStrategy — `HedgeIndexRiskOffRule`, score >= 8
- ShortTermMomentumLongTermStrengthStrategy — score > 9 (`frosk.slms.hedge.exit.score`)
- SwedishLongTermMomentumStrategy — score > 9 (`frosk.swedish.longterm.hedge.exit.score`)

For SLMS and SwedishLongTermMomentum the exit bar (>9) deliberately sits **above** the entry gate (<=7): exiting already at the 8-point defensive tier sold into weakness in backtests.

HighLanderStrategy and CANSLIMStrategy have indirect HedgeIndex exposure through their composite sub-strategy exit rules. The three intraday strategies gate **entries** on score <= `frosk.intraday.hedge.max.score` (9) but have no HedgeIndex exit — their time exits close positions within hours anyway.

### Time-Based Exits

4 strategies enforce a maximum holding period (all via `MaxBarsHeldRule`):

- OMXS30SwingStrategy — 10 bars (10 days on daily bars)
- OpeningRangeBreakoutIntradayStrategy — 16 bars (~4 hours on 15m bars)
- VWAPMeanReversionIntradayStrategy — 12 bars (~3 hours)
- GapReversalIntradayStrategy — 20 bars (~5 hours)

### Execution Model

| Model | Count | Strategies |
|---|---|---|
| TradeOnNextOpenModel | 19 | All daily/swing/long-term strategies |
| TradeOnCurrentCloseModel | 5 | EngulfingStrategy, GoldStrategy, and the three intraday strategies (ORB, VWAP-MR, GapReversal) |

Intraday strategies using CurrentClose also apply a 0.03% fee per trade (`intradayFeePerTradePercent`).

## Risk Gaps (remaining)

**No macro risk-off exit:** most batch strategies (ADX, CCI, EMA crossovers, etc.) still ignore HedgeIndex on exit. Positions remain open through regime changes. The six actively-portfolio'd strategies (SLMS, HighLander, SwedishLongTermMomentum, DailyOversoldBounce, CANSLIM, plus DailyBreakout) all have direct or composite HedgeIndex exits.

**No time limit:** outside OMXS30Swing and the intraday strategies, positions can hold indefinitely. Combined with a wide catastrophic stop, stale losing positions can persist — though SLMS, HighLander, and SwedishLongTermMomentum now bank winners via ATR trailing stops.
