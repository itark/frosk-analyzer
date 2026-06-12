# Månadsportföljen (Long-Term Factor Portfolio)

**Goal:** Hold 15–25 quality Swedish large/mid-cap stocks, rebalanced monthly. Beat OMXS30 over 3–5 years.

**Universe:** Active Swedish stocks (`active=true AND name LIKE '%.ST'`). The `active` flag filters to `enterpriseValue > 500M` — this is appropriate for a large/mid-cap strategy and requires no override.

**Class:** `SwedishLongTermMomentumStrategy extends AbstractStrategy implements IIndicatorValue`. Wired into `StrategiesMap` (all 5 points) and `PortfolioService`.

## Entry Rules (ALL must be met)

1. **Monthly rebalance window** — first 7 calendar days of every month (`MonthlyRebalanceRule`). The original design was a quarterly window, which combined with ten AND-ed conditions produced 3 trades in three years across the whole universe.
2. **HedgeIndex score ≤ 7** (`HedgeIndexTieredRule`; 8+ blocks)
3. **6-month momentum** — ROC(126) > 0
4. **Golden Cross** — close > SMA(50) AND SMA(50) > SMA(200)
5. **3-month relative strength vs ^OMX** — stock ROC(63) outperforms OMXS30 ROC(63) (date-aligned lookup against the `^OMX` series)
6. **Valuation** — PEG ratio < 2.5 (`frosk.swedish.longterm.pegratio.threshold`; gate skipped if no data). Uses its own key, no longer shared with HighLander's `frosk.hedge.criteria.pegratio.threshold`.
7. **Beta** — < 2.0 (filters extreme high-vol names; gate skipped if no data)
8. **Low volatility** — 252-day annualized standard deviation of **daily returns** < 0.60 (`frosk.swedish.longterm.maxVolatility`). Must be computed on returns, not price levels — the stddev of price itself measures how far the stock trended, which blocked every mover and let only dead-flat names through.

The redundant 12M−1 momentum rule was dropped — it duplicated the 6-month momentum gate and silenced any listing younger than 252 bars.

## Exit Rules (ANY triggers)

- HedgeIndex score > 9 (`frosk.swedish.longterm.hedge.exit.score`) — strong risk-off only. Exiting already at the 8-point defensive tier dumped positions a few weeks after every monthly entry, since the score oscillates between 4 and 9.
- Death cross: SMA(50) < SMA(200)
- 6-month momentum decisively negative: ROC(126) < −5 (a marginal dip around 0 caused churn)
- ATR trailing stop: 3×ATR(14) below the highest close since entry (`frosk.swedish.longterm.atr.mult`) — banks winners before the slower factor exits trigger
- Catastrophic stop (`frosk.strategy.catastrophic.stop.pct`, 15%)

## Portfolio Integration

`PortfolioService.build()` includes `SwedishLongTermMomentumStrategy` positions with:

- 30%-per-sector cap (requires the `Security.sector` field — populated via `RapidApiManager.getModuleAssetProfile()` inside `YAHOODataManager.updateSecurityMetaData()`)
- Tiered HedgeIndex sizing (`computeTieredTopN()`, day-floored score): score 0–3 → full topN; 4–7 → topN/2; 8+ → 0 positions
- topN limit ranked by SQN (`frosk.swedish.longterm.topN`, default 25)

## Properties

| Key | Value |
|---|---|
| `frosk.swedish.longterm.topN` | 25 |
| `frosk.swedish.longterm.maxVolatility` | 0.60 |
| `frosk.swedish.longterm.pegratio.threshold` | 2.5 |
| `frosk.swedish.longterm.hedge.exit.score` | 9 |
| `frosk.swedish.longterm.atr.mult` | 3.0 |

Backtest impact of the 2026-06-11 redesign: 2 trades ever → 135 trades at +1.23%/trade (34% win rate).
