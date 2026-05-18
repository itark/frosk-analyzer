# Exit Rules Inventory

Cross-strategy audit of exit rule composition, stop-loss types, HedgeIndex gating, time-based exits, and trade execution models.

## Full Matrix

| Strategy | Exit Rule | Stop-Loss | HedgeIndex Exit | Time Exit | Exec Model |
|---|---|---|---|---|---|
| ADXStrategy | ADX>20 AND +DI<-DI AND Close<SMA50 OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| CCICorrectionStrategy | LongCCI<-100 AND ShortCCI>100 OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| ConvergenceDivergenceStrategy | NegativeDivergenceIndicator OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| DailyBreakoutStrategy | StopLoss(5-bar low) OR ProfitTarget(2:1 R:R) OR HedgeIndexRiskOff | Static (5-bar low) | Yes (>=8) | No | NextOpen |
| DailyOversoldBounceStrategy | Close>SMA20 OR RSI14>55 OR StopLoss(3-bar low) OR HedgeIndexRiskOff | Static (3-bar low) | Yes (>=8) | No | NextOpen |
| EMATenTenStrategy | EMA10(close) < EMA10(open) OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| EMATenTwentyStrategy | EMA10 < EMA20 OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| EngulfingStrategy | ChandelierExit(5,3) OR StopLoss(2%) OR TrailingStop(2%) | Dual: hard+trailing 2% | No | No | CurrentClose |
| GlobalExtremaStrategy | Close > WeekMaxPrice+0.996% OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| HaramiStrategy | ChandelierExit(22,3) OR StopLoss(2%) OR TrailingStop(2%, 5 bars) | Dual: hard+trailing 2% | No | No | NextOpen |
| HighLanderStrategy | Composite sub-strategy exit OR StopLoss(20%) | Hard 20% (configurable) | Via composite | No | NextOpen |
| MovingMomentumStrategy | EMA9<EMA26 AND StochK<80 AND MACD<EMAMacd OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| OMX30IntradayMomentumStrategy | RSI5>75 OR EMA5<EMA13 OR MaxBarsHeld(12) OR StopLoss(0.5%) | Hard 0.5% | No | Yes (12 bars / 1h) | CurrentClose |
| OMXS30SwingStrategy | RSI14>65 OR AtrStopLoss(2xATR) OR StopGain(3xATR) OR MaxBarsHeld(10) | ATR-based (2x) | No | Yes (10 bars) | NextOpen |
| RSI2Strategy | EMA5<EMA200 AND RSI2>80 AND EMA5<Close OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| RunawayGAPIntradayStrategy | TrailingStop(2%, 3-bar lookback) | Trailing 2% | No | No | CurrentClose |
| RunawayGAPStrategy | TrailingStop(2%, 3-bar lookback) | Trailing 2% | No | No | NextOpen |
| ShortTermMomentumLTStrength | Close<SMA50 OR SMA10<SMA20 OR HedgeIndexRiskOff OR StopLoss(15%) | Hard 15% (configurable) | Yes (>=8) | No | NextOpen |
| SimpleMovingMomentumStrategy | ChandelierExit(5,3) OR StopLoss(2%) OR TrailingStop(2%) | Dual: hard+trailing 2% | No | No | NextOpen |
| SwedishLongTermMomentum | HedgeIndexRiskOff OR SMA50<SMA200 OR ROC126<0 OR CatastrophicStop(15%) | Catastrophic 15% | Yes (>=8) | No | NextOpen |
| ThreeBlackWhiteStrategy | ThreeBlackCrowsIndicator OR CatastrophicStop(15%) | Catastrophic 15% | No | No | NextOpen |
| VWAPStrategy | Close<VWAP OR StopLoss(2%) OR StopGain(2%) | Hard 2% | No | No | NextOpen |

## Summary by Category

### Stop-Loss Coverage

| Type | Count | Strategies |
|---|---|---|
| Catastrophic stop (15%) | 10 | ADX, CCI, Convergence, EMA10/10, EMA10/20, GlobalExtrema, MovingMomentum, RSI2, SwedishLongTermMomentum, ThreeBlackWhite |
| Hard stop (fixed %) | 5 | HighLander (20%), ShortTermMomentumLTStrength (15%), VWAPStrategy (2%), EngulfingStrategy (2%), OMX30IntradayMomentum (0.5%) |
| Dual hard + trailing | 3 | Engulfing (2%), Harami (2%), SimpleMovingMomentum (2%) |
| Trailing only | 2 | RunawayGAP (2%), RunawayGAPIntraday (2%) |
| Static price level | 2 | DailyBreakout (5-bar low), DailyOversoldBounce (3-bar low) |
| ATR-based | 1 | OMXS30Swing (2x ATR) |

### HedgeIndex Gating on Exit

4 of 22 strategies exit on HedgeIndex risk-off (score >= 8):

- DailyBreakoutStrategy
- DailyOversoldBounceStrategy
- ShortTermMomentumLongTermStrengthStrategy
- SwedishLongTermMomentumStrategy

HighLanderStrategy has indirect HedgeIndex exposure through its composite sub-strategy exit rules.

### Time-Based Exits

2 of 22 strategies enforce a maximum holding period:

- OMX30IntradayMomentumStrategy — 12 bars (1 hour on 5-min bars)
- OMXS30SwingStrategy — 10 bars (10 days on daily bars)

### Execution Model

| Model | Count | Strategies |
|---|---|---|
| TradeOnNextOpenModel | 18 | All daily/swing/long-term strategies |
| TradeOnCurrentCloseModel | 4 | EngulfingStrategy, OMX30IntradayMomentumStrategy, RunawayGAPIntradayStrategy, GoldStrategy |

Intraday strategies using CurrentClose also apply a 0.03% fee per trade (`intradayFeePerTradePercent`).

## Risk Gaps (remaining)

**No macro risk-off exit:** 18 of 22 strategies ignore HedgeIndex on exit. Positions remain open through regime changes.

**No time limit:** 20 of 22 strategies can hold indefinitely. Combined with a wide catastrophic stop, stale losing positions can persist.