# Existing Strategies — Inventory

| Class | Type | Description |
|---|---|---|
| `HighLanderStrategy` | Long-term | Core equity strategy — gated by HedgeIndex; entries only in risk-on regime. Exit: composite sub-strategy exits OR 20% hard stop-loss. |
| `ShortTermMomentumLongTermStrengthStrategy` | Medium-term | Combines short-term momentum with long-term strength filter. Exit: price < SMA(50) OR SMA(10) < SMA(20) OR HedgeIndex risk-off OR 15% stop-loss. |
| `EngulfingStrategy` | Short-term | Candlestick pattern — uses `TradeOnCurrentCloseModel` |
| `GoldStrategy` | Commodity | Gold-specific rules — uses `TradeOnCurrentCloseModel` |
| `OMXS30SwingStrategy` | Swing (2–10 days) | **"Nordic Momentum Filter"** — Fredrik's own strategy. Entry: EMA(20) > EMA(50) AND RSI(14) crosses above 40 AND OBV > OBV SMA(5). Exit: RSI(14) > 65 OR ATR stop-loss (2× ATR) OR stop-gain (3× ATR × 2) OR max 10 bars held. Uses two custom inner rules: `AtrStopLossRule` and `MaxBarsHeldRule`. 137 lines. |
| `SwedishLongTermMomentumStrategy` | Quarterly factor | **Månadsportföljen** — 6M/12-1 momentum, low-vol filter, relative strength vs ^OMX, PEG/Beta gates, tiered HedgeIndex. 231 lines. |
| `DailyBreakoutStrategy` | Swing (2–10 days) | **Dagstrategin A** — 20-day breakout on elevated volume in uptrend. Uptrend filter, 0.5% breakout threshold, stop-loss at 5-bar low. 95 lines. |
| `DailyOversoldBounceStrategy` | Swing (2–10 days) | **Dagstrategin B** — RSI(14) < 30 + lower Bollinger Band bounce in uptrend. Volume confirmation, stop-loss at 3-bar low. 111 lines. |
| `OMX30IntradayMomentumStrategy` | Intraday (1-hour max hold) | **Tier-0** — EMA(5) > EMA(13) AND RSI(5) crosses above 50 on 5-minute OMX30 bars. Exit: RSI(5) > 75 OR EMA crossover reversal OR 12 bars held. Uses `TradeOnCurrentCloseModel` with 0.03% fee. |

> All strategies extend `AbstractStrategy` and implement `IIndicatorValue`. All must be registered in `StrategiesMap` at 5 points. `EngulfingStrategy` and `GoldStrategy` use `TradeOnCurrentCloseModel`; all others use `TradeOnNextOpenModel`.
