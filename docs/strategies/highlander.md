# HighLanderStrategy

**Type:** Long-term

Core equity strategy. The entry is a composite of macro/fundamental gates **AND** a fresh price-action trigger (added in the 2026-06-11 PnL overhaul — the composite gates are regime/fundamental conditions that stay true for long stretches, so on their own they triggered entries at arbitrary chart locations).

## Entry (all required)

- Composite sub-strategy entries: `HedgeIndexStrategy` AND `BetaStrategy` AND `PEGRatioStrategy`
- Price trigger: EMA(20) crosses above EMA(50) AND close > SMA(200)

## Exit (first satisfied wins)

- Composite sub-strategy exits
- ATR trailing stop: 3×ATR(14) below the highest close since entry (`frosk.highlander.atr.mult`, via `AtrTrailingStopRule`)
- Hard stop-loss 12% below entry (`frosk.highlander.stoploss.percent`, was 20%)

## Properties

| Key | Value |
|---|---|
| `frosk.highlander.stoploss.percent` | 12.0 |
| `frosk.highlander.atr.mult` | 3.0 |

Backtest impact of the redesign: −4.19%/trade → +13.7%/trade (8 high-quality trades; max drawdown 55% → 14%).
