# Månadsportföljen (Long-Term Factor Portfolio)

**Goal:** Hold 15–25 quality Swedish large/mid-cap stocks, rebalanced quarterly. Beat OMXS30 over 3–5 years.

**Universe:** Active Swedish stocks (`active=true AND name LIKE '%.ST'`). The `active` flag filters to `enterpriseValue > 500M` — this is appropriate for a large/mid-cap strategy and requires no override.

## Selection Factors — Swedish Adaptation of the Equity Model

| Factor | ta4j Implementation | Weight | Notes |
|---|---|---|---|
| 6-month momentum | `ROCIndicator(closePriceSeries, 126)` | 30% | 126 trading days ≈ 6 months |
| 12-month momentum (12-1) | `(close[t-21] - close[t-252]) / close[t-252]` | 30% | Skips most recent month to avoid short-term reversal — **not** `ROCIndicator(series, 252)` which includes the reversal month |
| Low volatility | `StandardDeviationIndicator(closePriceSeries, 252)` | 20% | Inverted in composite score — lower vol = higher score |
| Relative strength vs OMXS30 | `ROCIndicator(stock, 63)` minus `ROCIndicator(^OMX, 63)` | 20% | 3-month outperformance vs benchmark |
| Golden Cross | Price > SMA(50) AND Price > SMA(200) | Hard filter | Binary — stock excluded from ranking if not met |
| Dividend yield | From `Security.dividendYield` | Soft tilt | Populated from statistics; used for portfolio ranking, not as hard entry filter |

**Composite score:** Each factor is rank-normalised across the candidate universe (rank 1 = worst, rank N = best), then weighted and summed. Stocks failing the Golden Cross hard filter are excluded before ranking.

## Entry/Exit Rules

- Entry: stock enters top-N ranked list at quarterly rebalance date (first trading day of Jan, Apr, Jul, Oct) AND Golden Cross condition met
- Exit: stock drops below rank threshold OR composite score falls below bottom quartile OR death cross (close < SMA200)
- Max weight per stock: 10%; max per sector: 30% (requires `sector` field — see below)
- HedgeIndex gate: score 0–3 → full allocation (top-N positions); score 4–7 → reduce to top-N/2 positions, no new entries into lowest-ranked half; score 8+ → no new entries, trim bottom 25% of positions

**Quarterly rebalance implementation note:** ta4j runs bar-by-bar so quarterly date detection requires a helper — check if `bar.getEndTime().getMonth()` is in {Jan, Apr, Jul, Oct} AND `bar.getEndTime().getDayOfMonth() <= 5` (first week of quarter) to approximate the rebalance trigger.

**Class:** `SwedishLongTermMomentumStrategy extends AbstractStrategy implements IIndicatorValue` — ✅ implemented (231 lines). Wired into `StrategiesMap` (all 5 points) and `PortfolioService`.

**Portfolio filter:** `PortfolioService.build()` includes `SwedishLongTermMomentumStrategy` with sector cap, tiered HedgeIndex sizing, and topN limit.

**Properties:** `frosk.swedish.longterm.topN` (default: 25) — max positions. `frosk.swedish.longterm.maxVolatility` (default: 0.40) — max annualized vol for entry.

**Sector field:** `sector` (`String`) column on `Security`. Populated via `RapidApiManager.getModuleAssetProfile()` (`asset-profile` module) called inside `YAHOODataManager.updateSecurityMetaData()`. Run `updateSecurityMetaData()` once to backfill all existing stocks before the 30%-per-sector cap in `PortfolioService` takes effect.
