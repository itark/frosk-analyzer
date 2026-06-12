# ShortTermMomentumLongTermStrengthStrategy

**Type:** Medium-term

Redesigned in the 2026-06-11 PnL overhaul: entry is now a fresh momentum **event** (not a level, so the strategy does not re-enter on every bar of an existing uptrend), with an ADX trend filter and a HedgeIndex regime gate.

## Entry (all required)

- SMA(10) crosses above SMA(20) — `CrossedUpIndicatorRule`, a fresh cross event
- Close above both SMA(50) and SMA(100) — long-term strength
- ADX(14) > 25 (`frosk.slms.adx.threshold`) — trend strength filter, skips sideways chop
- HedgeIndex score ≤ 7 (`frosk.slms.hedge.max.score`, via `HedgeIndexMaxScoreRule`) — no new entries in defensive/risk-off regimes

## Exit (first satisfied wins)

- ATR trailing stop (chandelier): close falls 2.5×ATR(14) below the highest close since entry (`frosk.slms.atr.mult`, via `AtrTrailingStopRule`)
- Close crosses below SMA(50) — loss of medium-term strength
- HedgeIndex score > 9 (`frosk.slms.hedge.exit.score`) — strong risk-off only; the exit bar deliberately sits above the entry gate, since exiting already at the 8-point defensive tier sold into weakness in backtests
- Hard stop-loss 10% below entry (`frosk.slms.stoploss.percent`)

The old SMA(10) < SMA(20) whipsaw exit was deleted.

## Properties

| Key | Value |
|---|---|
| `frosk.slms.stoploss.percent` | 10.0 |
| `frosk.slms.adx.threshold` | 25.0 |
| `frosk.slms.atr.mult` | 2.5 |
| `frosk.slms.hedge.max.score` | 7 |
| `frosk.slms.hedge.exit.score` | 9 |

Backtest impact of the redesign (test-DB universe): 6,802 trades at −0.60%/trade → 158 trades at +1.48%/trade; average max drawdown 64% → 19%.
