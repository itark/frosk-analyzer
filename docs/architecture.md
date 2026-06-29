# System Architecture

```
HighLander (startup runner + scheduler target)
  ├─ syncTier0()  → IntradayStrategyRunner.run()
  │    ├─ IntradayDataService.syncAndBuildSeries()
  │    │    ├─ YahooFinanceDirectClient.getHistory(^OMX, 15m) — 1 API call
  │    │    ├─ upsert IntradayBar rows (idempotent)
  │    │    ├─ deleteOlderThan(now - retentionDays)
  │    │    └─ buildSeriesFromDb() → ta4j BarSeries
  │    ├─ OMX30IntradayMomentumStrategy.buildStrategy(series)
  │    ├─ BarSeriesManager.run(strategy) → TradingRecord
  │    └─ emit IntradaySignal (BUY/SELL) if last bar triggers entry/exit
  ├─ StrategyAnalysis.run(strategy, securityId)
  │    ├─ Case 1: null/null  → all strategies × all securities (batch)
  │    │    └─ HedgeIndexService.warmCache() called first
  │    ├─ Case 2: strategy + null  → one strategy × all securities
  │    └─ Case 3: strategy + securityId → one strategy × one security
  │         └─ StrategyExecutor.execute() [@Transactional(REQUIRES_NEW)]
  │              ├─ same-day idempotency check (lastRunDate == today → skip)
  │              ├─ builds BarSeries via BarSeriesService
  │              ├─ builds ta4j Strategy via StrategiesMap.getStrategyToRun()
  │              ├─ runs backtest via BarSeriesService.runConfiguredStrategy()
  │              ├─ upserts FeaturedStrategy (sets lastRunDate)
  │              └─ replaces StrategyTrade + StrategyIndicatorValue (saveAll)
  ├─ StrategyAnalysis.runHedgeIndexStrategies()  → syncs + runs 14 macro strategies → hedgeIndexService.update()
  ├─ StrategyAnalysis.runDagstrateginStrategies() → runs DailyBreakout + DailyOversoldBounce on OMX30
  └─ PortfolioService.build() → snapshots open positions with sector cap + tiered HedgeIndex sizing
```

## FeaturedStrategy — Central Result Entity

`FeaturedStrategy` is one row per **(strategy name × security name)**. Key fields:

- `open` — true if the last trade in the backtest is a BUY without a matching SELL (position currently open)
- `entryDate`, `entryPrice` — from the last open BUY trade
- `lastRunDate` — `LocalDate` of last `StrategyExecutor` run; used for same-day idempotency (nullable for pre-existing rows)
- `sqn`, `expectency`, `profitableTradesRatio`, `totalGrossReturn`, `totalProfit`
- `@OneToMany StrategyTrade` — individual BUY/SELL pairs with `pnl`, `grossProfit`, `price`, `amount`
- `@OneToMany StrategyIndicatorValue` — latest indicator snapshots for charting

`StrategyExecutor` skips execution if `lastRunDate` is today (unless `frosk.strategy.force.rerun=true`). On each run it deletes+re-inserts `StrategyTrade` and `StrategyIndicatorValue`, sets `lastRunDate`, and saves. A `FeaturedStrategy` row is created on first run and updated on subsequent runs.

## Portfolio System

`PortfolioService.build()` snapshots open `FeaturedStrategy` positions filtered to:
- `ShortTermMomentumLongTermStrengthStrategy`
- `HighLanderStrategy`
- `SwedishLongTermMomentumStrategy` — subject to 30%-per-sector cap (`applySectorCap`), tiered HedgeIndex sizing (`computeTieredTopN`: score 0-3 → topN, 4-7 → topN/2, 8+ → 0), and topN limit (default 25, ranked by SQN)

**Portfolio quality filters (applied to ALL positions before sector cap / top-N):**
- `passesQualityGate()` — excludes positions with SQN < `frosk.portfolio.min.sqn` (default 1.0) or win rate < `frosk.portfolio.min.win.rate` (default 0.35)
- HighLander + ShortTermMomentum positions are additionally capped at `frosk.portfolio.other.topN` (default 10), ranked by SQN descending — prevents indefinite accumulation of stale positions

Each call persists a new `Portfolio` + `PortfolioPosition` records. REST endpoints in `DataController`:
- `POST /portfolio/build`
- `GET /portfolio` — latest snapshot
- `GET /portfolio/history`
- `GET /portfolio/{id}`
- `GET /dagstrategin/watchlist` — open Dagstrategin signals ranked by SQN

## BarSeriesService

- `getDataSet(Database)` — batch-loads all prices in one query (`findBySecurityIdInOrderBySecurityIdAscTimestampAsc`), groups in memory; avoids N+1.
- `getDataSet(String, boolean, boolean)` — loads by security name; `api=true` fetches live from Coinbase.
- Trade execution model: `EngulfingStrategy` and `GoldStrategy` use `TradeOnCurrentCloseModel`; all others use `TradeOnNextOpenModel`.
