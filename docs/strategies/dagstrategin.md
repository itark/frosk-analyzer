# Dagstrategin (Daily Short-Term Swing)

**Execution model:** Runs entirely on **daily OHLCV bars** — the same data already fetched by `YahooFinanceDirectClient`. No intraday data required, no new entity or ingestion pipeline needed. After each daily close, `StrategyExecutor` evaluates signals against the updated daily `BarSeries`. A ranked **next-morning watchlist** is produced for the human trader to act on at the open.

**Holding period:** 2–10 trading days. Not day trading — short-term swing trading on daily bars.

**Universe:** OMXS30 constituents (~30 tickers). No additional fetch cost beyond the existing Tier 1 daily sync.

**Data flow (runs after Tier 1 daily price sync, `0 0 18 * * MON-FRI`):**
1. Tier 1 sync fetches today's daily close for OMXS30 tickers via `YahooFinanceDirectClient`
2. `StrategyExecutor` runs both Dagstrategin strategies across the OMXS30 universe
3. Signals stored as `StrategyTrade`; metrics updated in `FeaturedStrategy`
4. REST endpoint `GET /dagstrategin/watchlist` returns next-morning ranked candidates

**No new data infrastructure required** — uses existing `BarSeriesService.getDataSet()` and `SecurityPrice` table.

---

## Sub-strategy A: Daily Breakout Momentum

Captures multi-day momentum moves when a stock breaks out of a consolidation range on elevated volume.

- Precondition: close > SMA(200) — only trade breakouts in long-term uptrends
- Signal: daily close > 20-day high by at least 0.5% AND volume on signal bar > 1.5× 20-bar average volume
- Entry: open of next trading day after signal
- Stop loss: lowest low of the prior 5 days
- Target: entry + 2× (entry − stop) — RR 2:1
- HedgeIndex gate: score 0–3 → full size; score 4–7 → half size; score ≥ 8 → no new entries, exit open positions
- Max concurrent open positions: 5 across all OMXS30 tickers
- Next-morning watchlist: stocks where signal fired on today's close, ranked by volume ratio (signal bar volume ÷ 20-bar average) — strongest volume confirmation first

**Class:** `DailyBreakoutStrategy extends AbstractStrategy implements IIndicatorValue` — ✅ implemented (95 lines). Wired into `StrategiesMap` (all 5 points).

---

## Sub-strategy B: Daily Oversold Bounce

Captures mean-reversion moves after sharp pullbacks in otherwise uptrending stocks.

- Precondition: close > SMA(200) — stock must be in a long-term uptrend
- Signal: RSI(14) < 30 AND close < lower Bollinger Band (20, 2σ) AND close > prior 52-week low × 1.10 AND volume on signal bar > 1.2× 20-bar average (confirms selling exhaustion, not just drift)
- Entry: open of next trading day after signal
- Stop loss: lowest low of the prior 3 days (wider than prior day's low to avoid noise stop-outs on daily bars)
- Target: SMA(20) at time of signal OR RSI(14) > 55, whichever comes first
- Max 1 open position per stock at a time; max 3 concurrent open positions across all tickers
- Exit also triggered by: HedgeIndex score ≥ 8
- Next-morning watchlist: stocks where signal fired on today's close, ranked by distance of close below SMA(20) — deepest pullback first

**Class:** `DailyOversoldBounceStrategy extends AbstractStrategy implements IIndicatorValue` — ✅ implemented (111 lines). Wired into `StrategiesMap` (all 5 points).

---

## Portfolio Inclusion

Since the 2026-06-11 PnL overhaul, `DailyOversoldBounceStrategy` is included in `PortfolioService.DAILY_STRATEGIES`, so its open positions appear in the daily portfolio snapshot (subject to the quality gate: SQN ≥ `frosk.portfolio.min.sqn`, win rate ≥ `frosk.portfolio.min.win.rate` — note the win rate is on the **percent scale**, 40.0 = 40%). `DailyBreakoutStrategy` is not in the portfolio.

The HedgeIndex gates use the carried-forward level semantics (`frosk.hedge.criteria.risk.threshold=7`): `HedgeIndexRiskOffRule` exits fire at score ≥ 8, and the `HedgeIndexTieredRule(7)` entry gate blocks at 8+.
