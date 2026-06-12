# Existing Strategies — Inventory

| Class | Type | Description |
|---|---|---|
| `HighLanderStrategy` | Long-term | Core equity strategy — composite HedgeIndex/Beta/PEG gates AND a fresh price trigger (EMA(20) crosses above EMA(50), close > SMA(200)). Exit: composite exits OR 3×ATR trailing stop OR 12% hard stop. See `highlander.md`. |
| `ShortTermMomentumLongTermStrengthStrategy` | Medium-term | Fresh SMA(10)/SMA(20) cross + close > SMA(50)/SMA(100) + ADX(14) > 25 + HedgeIndex ≤ 7. Exit: 2.5×ATR trailing stop OR close crosses below SMA(50) OR HedgeIndex > 9 OR 10% stop. See `short-term-momentum.md`. |
| `CANSLIMStrategy` | Medium/long-term | CAN SLIM screen: HedgeIndex risk-on, golden-cross leader, YoY growth, positive EPS, volume surge, near 52-week high. Exit: close < SMA(50) OR death cross OR HedgeIndex exit OR 15% stop. In the daily portfolio. |
| `EngulfingStrategy` | Short-term | Candlestick pattern — uses `TradeOnCurrentCloseModel` |
| `GoldStrategy` | Commodity | Gold-specific rules — uses `TradeOnCurrentCloseModel` |
| `OMXS30SwingStrategy` | Swing (2–10 days) | **"Nordic Momentum Filter"** — Fredrik's own strategy. Entry: EMA(20) > EMA(50) AND RSI(14) crosses above 40 AND OBV > OBV SMA(5). Exit: RSI(14) crosses above 65 OR ATR stop-loss (2×ATR, shared `AtrStopLossRule`) OR 6% stop-gain OR max 10 bars held (`MaxBarsHeldRule`). |
| `SwedishLongTermMomentumStrategy` | Monthly factor | **Månadsportföljen** — monthly rebalance window (first 7 days), 6M momentum, golden cross, relative strength vs ^OMX, PEG/Beta gates, returns-based vol cap 0.60, tiered HedgeIndex. Exit: HedgeIndex > 9 / death cross / ROC6m < −5 / 3×ATR trail. See `manadsportfoljen.md`. |
| `DailyBreakoutStrategy` | Swing (2–10 days) | **Dagstrategin A** — 20-day breakout on elevated volume in uptrend. Uptrend filter, 0.5% breakout threshold, stop-loss at 5-bar low. |
| `DailyOversoldBounceStrategy` | Swing (2–10 days) | **Dagstrategin B** — RSI(14) < 30 + lower Bollinger Band bounce in uptrend. Volume confirmation, stop-loss at 3-bar low. In the daily portfolio. |
| `OpeningRangeBreakoutIntradayStrategy` | Intraday (15m bars) | **Tier-0, "Frukosthandeln"** — fresh cross above the 30-min opening-range high, morning-only entries (≤ bar 12), ±1.5% gap filter. Exit: 1.5×OR-width target / OR-low stop / 16 bars / 0.8% hard stop. See `omx30-intraday.md`. |
| `VWAPMeanReversionIntradayStrategy` | Intraday (15m bars) | **Tier-0, "Gummibandshandeln"** — entry ≥ 0.15% below session-anchored VWAP (`SessionVWAPIndicator`) with RSI(5) < 30 in an intraday uptrend. Exit: back above VWAP / RSI(5) > 65 / 0.6% stop / 12 bars. |
| `GapReversalIntradayStrategy` | Intraday (15m bars) | **Tier-0, "Morgonfällan"** — fades 0.5–3.5% overnight gap-downs in the morning when support holds. Exit: gap fill / max(1×ATR, 0.6%) stop / 20 bars. |

> All strategies extend `AbstractStrategy` and implement `IIndicatorValue`. All must be registered in `StrategiesMap` at 5 points. `EngulfingStrategy` and `GoldStrategy` use `TradeOnCurrentCloseModel`; the intraday strategies use `TradeOnCurrentCloseModel` with 0.03% fee; all others use `TradeOnNextOpenModel`.

## Portfolio membership

`PortfolioService.DAILY_STRATEGIES` (positions eligible for the daily portfolio snapshot): `ShortTermMomentumLongTermStrengthStrategy`, `HighLanderStrategy`, `SwedishLongTermMomentumStrategy`, `DailyOversoldBounceStrategy`, `CANSLIMStrategy`.

`PortfolioService.INTRADAY_STRATEGIES` (intraday portfolio, additive day-PnL): the three intraday strategies above.
