# Intraday Pipeline — Tier 0 (10-Minute Ticker)

## Overview

Every 10 minutes during Stockholm market hours (09:00–17:59 CET, Mon–Fri) the Tier-0 pipeline:

1. Fetches the latest **15-minute bars** for all securities in the configured datasets (`intraday.datasets`, default: OMX30) via `YahooFinanceDirectClient.getIntradayBars()` — direct Yahoo Finance v8 API, free. `yahoo.fetch.delay.ms` (default 300ms) between tickers.
2. Upserts new bars into the `intraday_bar` table (idempotent — existing bars are skipped by the `uq_intraday_bar` unique constraint on `(security_id, bar_timestamp, interval_code)`).
3. Prunes bars older than `intraday.retention.days` (30 in `application.properties`) — a rolling window large enough to evaluate intraday strategy changes on a real sample.
4. Builds a ta4j `BarSeries` per security from the retained window.
5. Runs all registered `IntradayStrategy` implementations on each security. Strategies are auto-discovered via Spring DI — any `@Component` implementing `IntradayStrategy` is included.
6. If a strategy's entry or exit rule fires on the **latest bar**, emits an `IntradaySignal` to the `intraday_signal` table.
7. Calls `StrategyExecutor` to update `FeaturedStrategy` backtest results for each (strategy × security) pair.

This is a **signal-generation layer only** — no orders are placed automatically. The human trader monitors signals via the dashboard at `/intraday-strategies-page.html` or via the REST endpoints.

## Adding a New Intraday Strategy

1. Create the strategy class extending `AbstractStrategy` and implementing both `IIndicatorValue` and `IntradayStrategy`.
2. Annotate with `@Component`.
3. Register in **all five places** in `StrategiesMap` (see CLAUDE.md).
4. Add the class name to `frosk.strategies.exclude` in both property files (intraday strategies run via the Tier-0 pipeline, not the batch all-strategies run).

The strategy will be automatically picked up by `IntradayStrategyRunner` via Spring's `List<IntradayStrategy>` injection.

## Call Tree

```
HighLander.syncTier0()
  └─ IntradayStrategyRunner.run()
       ├─ IntradayDataService.syncAndBuildAllSeries()
       │    ├─ for each dataset in intraday.datasets:
       │    │    └─ DataSetRepository.findByName(dataset) → securities
       │    ├─ for each security:
       │    │    ├─ YahooFinanceDirectClient.getIntradayBars(ticker, 15m, 5d)
       │    │    ├─ upsert IntradayBar rows
       │    │    └─ deleteOlderThan(now - retentionDays)
       │    └─ buildSeriesFromDb() per security → Map<Security, BarSeries>
       ├─ for each (security, series):
       │    └─ for each IntradayStrategy:
       │         └─ buildStrategy(series) → run → emit signal if entry/exit fires
       ├─ for each IntradayStrategy:
       │    └─ StrategyExecutor.execute(strategyName, eligibleSeries)
       └─ log summary
```

`IntradayDataService.buildAllSeriesFromDb()` builds the same per-security series from retained bars **without** syncing — used by the offline validation harness (`StrategyComparisonReportIT#intradayReport`).

## REST Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/intraday/run` | POST | Manual trigger of `syncTier0()` — useful outside market hours |
| `/intradayTodaySignals` | GET | All intraday BUY/SELL signals fired today (from FeaturedStrategy trades) |
| `/intraday/signals` | GET | Latest 20 intraday signals (from `intraday_signal` table) |
| `/intraday/signals/today` | GET | Today's BUY signals only |
| `/intraday/pnl` | GET | PnL round-trip report per (ticker, strategy), **net of the 2× 0.03% round-trip fee** |
| `/portfolio/intraday/build` | POST | Manual trigger of `PortfolioService.buildIntraday()` |
| `/portfolio/intraday` | GET | Latest intraday portfolio snapshot |
| `/portfolio/intraday/history` | GET | All intraday portfolio snapshots (headers only) |

## Intraday Portfolio PnL

`PortfolioService.buildIntraday()` snapshots the intraday portfolio on every Tier-0 cycle (no same-day idempotency — positions change frequently). Unlike the daily portfolio (average unrealized PnL of open positions), the intraday `totalPnlPercent` is **additive day-PnL**:

- `realizedPnlPercent` — today's BUY→SELL `IntradaySignal`s paired per (ticker, strategy), each round trip netted of the 2× 0.03% fee, summed
- `closedTradeCount` — number of round trips closed today
- `totalPnlPercent` = realized + unrealized PnL of positions still open

Intraday round trips complete within hours, so the old open-positions-only average read 0.0000 essentially always. Test: `TestJIntradayPortfolioPnl`.

## Sync Volume

All tiers fetch via `YahooFinanceDirectClient` — free, no API key, no quota. Volumes below are for load awareness, not cost.

| Tier | Frequency | Client | Req/run |
|---|---|---|---|
| Tier 0 (intraday) | Every 10 min, 08:00–17:59, Mon–Fri | `YahooFinanceDirectClient` | ~29 |
| Tier 1 (daily) | MON-FRI 18:00 | `YahooFinanceDirectClient` | ~40 |
| Tier 2 (weekly) | SAT 06:00 | `YahooFinanceDirectClient` | ~900 |
| Tier 3 (monthly) | 1st of month 07:00 | `YahooFinanceDirectClient` | ~2,400 |

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `frosk.run.intraday` | `true` | Gate for the Tier-0 pipeline in `syncTier0()` |
| `intraday.datasets` | `OMX30` | Comma-separated list of datasets whose securities are synced for intraday bars |
| `intraday.retention.days` | `30` | Days of 15-min bars to retain in `intraday_bar` (code fallback: 7) |
| `frosk.intraday.hedge.max.score` | `9` | HedgeIndex entry gate for all intraday strategies — only strong risk-off (score > 9) blocks entries |
| `scheduler.tier0.cron` | `0 */10 9-17 * * MON-FRI` | Cron expression for Tier-0 tick |
| `exchange.transaction.intradayFeePerTradePercent` | `0.0003` | Fee applied to intraday strategies (0.03% per trade); also used to net round-trip PnL |
