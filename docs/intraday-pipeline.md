# Intraday Pipeline — Tier 0 (10-Minute Ticker)

## Overview

Every 10 minutes during Stockholm market hours (09:00–17:59 CET, Mon–Fri) the Tier-0 pipeline:

1. Fetches the latest **5-minute bars** for `^OMX` (OMXS30 index) via `RapidApiManager.getHistory(^OMX, 5m)` — **1 API request per tick**.
2. Upserts new bars into the `intraday_bar` table (idempotent — existing bars are skipped by the `uq_intraday_bar` unique constraint on `(security_id, bar_timestamp, interval_code)`).
3. Prunes bars older than `intraday.retention.days` (default: 7) to keep the table small.
4. Builds a ta4j `BarSeries` from the retained window and runs `OMX30IntradayMomentumStrategy` as a full backtest.
5. If the strategy's entry or exit rule fires on the **latest bar**, emits an `IntradaySignal` to the `intraday_signal` table.

This is a **signal-generation layer only** — no orders are placed automatically.  The human trader monitors signals via the H2 console or the `GET /intraday/signals` endpoint (if added).

## Cost Analysis

| Tier | Frequency | Req/run | Monthly |
|---|---|---|---|
| Tier 0 (intraday) | Every 10 min, 09:00–17:59, Mon–Fri | 1 | ~1,122 |
| Tier 1 (daily) | MON-FRI 18:00 | ~40 | ~880 |
| Tier 2 (weekly) | SAT 06:00 | ~900 | ~3,600 |
| Tier 3 (monthly) | 1st of month 07:00 | ~2,400 | ~2,400 |
| **Total** | | | **~8,002 / 10,000** |

## Adding More Intraday Tickers (Future)

Currently only `^OMX` is fetched. To add individual OMXS30 constituents:
1. Add ticker to the `YAHOO-OMX30` or `YAHOO-SWEDISH` CSV (already present for active stocks).
2. Extend `IntradayDataService.syncAndBuildSeries()` to accept a ticker argument, or introduce a list of tickers in `application.properties` (`intraday.tickers=^OMX,VOLV-B.ST,...`).
3. **Cost constraint**: each additional ticker adds ~1,122 req/month — at 30 tickers that would be ~33,660/month (over the 10,000 free limit). Upgrade RapidAPI plan before fetching more than ~8 intraday tickers.
