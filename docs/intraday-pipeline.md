# Intraday Pipeline — Tier 0 (10-Minute Ticker)

## Overview

Every 10 minutes during Stockholm market hours (09:00–17:59 CET, Mon–Fri) the Tier-0 pipeline:

1. Fetches the latest **15-minute bars** for all securities in the configured datasets (`intraday.datasets`, default: OMX30) via `YahooFinanceDirectClient.getIntradayBars()` — direct Yahoo Finance v8 API, no RapidAPI cost. 500ms sleep between tickers.
2. Upserts new bars into the `intraday_bar` table (idempotent — existing bars are skipped by the `uq_intraday_bar` unique constraint on `(security_id, bar_timestamp, interval_code)`).
3. Prunes bars older than `intraday.retention.days` (default: 7) to keep the table small.
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

## REST Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/intraday/run` | POST | Manual trigger of `syncTier0()` — useful outside market hours |
| `/intradayTodaySignals` | GET | All intraday BUY/SELL signals fired today (from FeaturedStrategy trades) |
| `/intraday/signals` | GET | Latest 20 intraday signals (from `intraday_signal` table) |
| `/intraday/signals/today` | GET | Today's BUY signals only |
| `/intraday/pnl` | GET | PnL round-trip report per (ticker, strategy) |

## Cost Analysis

| Tier | Frequency | Client | Req/run | Monthly RapidAPI cost |
|---|---|---|---|---|
| Tier 0 (intraday) | Every 10 min, 09:00–17:59, Mon–Fri | `YahooFinanceDirectClient` | ~29 | **$0** (free) |
| Tier 1 (daily) | MON-FRI 18:00 | `RapidApiManager` | ~40 | ~880 |
| Tier 2 (weekly) | SAT 06:00 | `RapidApiManager` | ~900 | ~3,600 |
| Tier 3 (monthly) | 1st of month 07:00 | `RapidApiManager` | ~2,400 | ~2,400 |
| **RapidAPI total** | | | | **~6,880 / 10,000** |

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `frosk.run.intraday` | `true` | Gate for the Tier-0 pipeline in `syncTier0()` |
| `intraday.datasets` | `OMX30` | Comma-separated list of datasets whose securities are synced for intraday bars |
| `intraday.retention.days` | `7` | Days of 15-min bars to retain in `intraday_bar` |
| `scheduler.tier0.cron` | `0 */10 9-17 * * MON-FRI` | Cron expression for Tier-0 tick |
| `exchange.transaction.intradayFeePerTradePercent` | `0.0003` | Fee applied to intraday strategies (0.03%) |
