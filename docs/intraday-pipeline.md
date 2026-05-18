# Intraday Pipeline — Tier 0 (10-Minute Ticker)

## Overview

Every 10 minutes during Stockholm market hours (09:00–17:59 CET, Mon–Fri) the Tier-0 pipeline:

1. Fetches the latest **5-minute bars** for all securities in the **OMX30 dataset** (~29 tickers) via `RapidApiManager.getHistory(ticker, 5m)` — **1 API request per security per tick**.
2. Upserts new bars into the `intraday_bar` table (idempotent — existing bars are skipped by the `uq_intraday_bar` unique constraint on `(security_id, bar_timestamp, interval_code)`).
3. Prunes bars older than `intraday.retention.days` (default: 7) to keep the table small.
4. Builds a ta4j `BarSeries` per security from the retained window.
5. Runs **two strategies** on each security:
   - `OMX30IntradayMomentumStrategy` — EMA(5)/EMA(13)/RSI(5), MaxBarsHeld(12), StopLoss(0.5%)
   - `RunawayGAPIntradayStrategy` — gap detection with trailing stop (2%)
6. If either strategy's entry or exit rule fires on the **latest bar**, emits an `IntradaySignal` to the `intraday_signal` table.
7. Calls `StrategyExecutor` to update `FeaturedStrategy` backtest results for each (strategy × security) pair.

This is a **signal-generation layer only** — no orders are placed automatically. The human trader monitors signals via the dashboard at `/intraday-strategies-page.html` or via the REST endpoints.

## Call Tree

```
HighLander.syncTier0()
  └─ IntradayStrategyRunner.run()
       ├─ IntradayDataService.syncAndBuildSeries()
       │    ├─ DataSetRepository.findByName("OMX30") → ~29 securities
       │    ├─ for each security:
       │    │    ├─ RapidApiManager.getHistory(ticker, 5m)
       │    │    ├─ upsert IntradayBar rows
       │    │    └─ deleteOlderThan(now - retentionDays)
       │    └─ buildSeriesFromDb() per security → Map<Security, BarSeries>
       ├─ for each (security, series):
       │    ├─ OMX30IntradayMomentumStrategy.buildStrategy(series) → run → emit signal
       │    ├─ RunawayGAPIntradayStrategy.buildStrategy(series) → run → emit signal
       │    └─ StrategyExecutor.execute() for each strategy (FeaturedStrategy upsert)
       └─ log summary
```

## REST Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/intraday/run` | POST | Manual trigger of `syncTier0()` — useful outside market hours |
| `/intradayOpenPositions` | GET | Open OMX30IntradayMomentumStrategy positions with entry/current price and unrealized PnL |
| `/intradayTodaySignals` | GET | All intraday BUY/SELL signals fired today |
| `/intraday/signals` | GET | Latest 20 intraday signals (from `intraday_signal` table) |
| `/intraday/signals/today` | GET | Today's BUY signals only |

## Cost Analysis

| Tier | Frequency | Req/run | Monthly |
|---|---|---|---|
| Tier 0 (intraday) | Every 10 min, 09:00–17:59, Mon–Fri | ~29 | ~32,538 |
| Tier 1 (daily) | MON-FRI 18:00 | ~40 | ~880 |
| Tier 2 (weekly) | SAT 06:00 | ~900 | ~3,600 |
| Tier 3 (monthly) | 1st of month 07:00 | ~2,400 | ~2,400 |
| **Total** | | | **~39,418 / 10,000** |

**Warning:** Fetching all ~29 OMX30 securities at 5-minute intervals far exceeds the free RapidAPI quota (10,000 req/month). This requires an upgraded API plan. To reduce cost within the free tier, limit `IntradayDataService.DATASET_NAME` to a smaller subset or reduce the tick frequency.

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `frosk.run.intraday` | `true` | Gate for the Tier-0 pipeline in `syncTier0()` |
| `intraday.retention.days` | `7` | Days of 5-min bars to retain in `intraday_bar` |
| `scheduler.tier0.cron` | `0 */10 9-17 * * MON-FRI` | Cron expression for Tier-0 tick |
| `exchange.transaction.intradayFeePerTradePercent` | `0.0003` | Fee applied to intraday strategies (0.03%) |
