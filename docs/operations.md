# Operations — Data Layer & Sync Schedule

## Data Layer — RapidApiManager

All external data is fetched through `RapidApiManager` using the **sparior yahoo-finance15** API on RapidAPI. This is the single entry point for all market data — any new data needed must be added here.

**Swedish stock ticker convention:** Nasdaq Stockholm tickers use the `.ST` suffix (e.g. `VOLV-B.ST`, `ERIC-B.ST`). The OMXS30 index is `^OMX`.

### Security Registry — CSV Code Files

Securities are registered in the DB via `DataSetHelper.addDatasetSecuritiesFromCvsFile()`, which reads CSV files from `src/main/resources/codes/`. The filename convention is `<Database>-<DatasetName>-<Description>.csv`. **To register any new ticker for price syncing, it must first be added to the appropriate CSV file.**

| File | Format | Dataset | Contents | Count |
|---|---|---|---|---|
| `YAHOO-SWEDISH-All securities in Sweden.csv` | semicolon-separated, quoted | `SWEDISH` | All Swedish stocks with `.ST` suffix + index tickers `^OMX`, `^OMXSPI`, `^NORDIC`, `^NORDICPI` | 977 |
| `YAHOO-OMX30-All securites included in OMX30.csv` | comma-separated | `OMX30` | OMXS30 constituents | 29 |
| `YAHOO-INDEX-World indexes.csv` | comma-separated | `INDEX` | Global market indicators | 23 |
| `YAHOO-OSCAR-The Money Machine.csv` | comma-separated | `OSCAR` | Small curated portfolio | 4 |

**`YAHOO-INDEX-World indexes.csv` — current tickers:**
`^FTSE`, `^GDAXI`, `^FCHI`, `^GSPC` (S&P 500), `^IXIC` (NASDAQ), `^N225`, `^MXX`, `^NYA`, `^VIX`, `^VVIX`, `^SDEX`, `^DJI`, `CL=F` (WTI Crude), `BZ=F` (Brent), `GC=F` (Gold), `^STOXX50E`, `^V2TX`, `DX-Y.NYB`, `EURUSD=X`, `JPY=X`, `AUDUSD=X`, `^TNX` (US 10Y Treasury), `^IRX` (US 13W T-Bill)

**Adding a new HedgeIndex indicator requires two steps:**
1. Add the ticker to `YAHOO-INDEX-World indexes.csv`
2. Add the scoring rule to `HedgeIndexService`

**Active endpoints (`RapidApiManager.java`):**

| Endpoint | Use | Called from |
|---|---|---|
| `/api/v1/markets/stock/history?interval=1d` | Daily OHLCV | `YAHOODataManager.syncronize()` |
| `/api/v1/markets/stock/modules?module=statistics` | Beta, PEG, EPS, P/E, enterprise value | `updateSecurityMetaData()` |
| `/api/v1/markets/stock/modules?module=income-statement` | Revenue YoY growth | `updateSecurityMetaData()` |
| `/api/v1/markets/stock/modules?module=recommendation-trend` | Analyst ratings | `updateSecurityMetaData()` |
| `/api/v1/markets/stock/modules?module=asset-profile` | Sector, industry | `updateSecurityMetaData()` → `setSectorData()` |

Index tickers (`^` prefix) and futures (`=F` suffix) are skipped by `updateSecurityMetaData()` — only called on actual stock tickers.

### RapidAPI Plan Constraints

Current plan (yahoo-finance15 via RapidAPI):
- **10,000 requests / month** free; **$0.003 per request** overage
- **5 requests / second** rate limit
- **10,240 MB / month** bandwidth; $0.001 per MB overage

**Tier-0 intraday uses a different path** — see below.

### Sync Schedule

**Total securities in DB: ~900.** Daily sync of all 900 = 900 × 22 trading days = **19,800 req/month** — nearly 2× the free quota. A tiered approach is required.

**Tier 0 — Intraday (all ~29 OMX30 securities, every 10 min)**

Tier-0 uses `YahooFinanceDirectClient` — a direct call to Yahoo Finance's v8 chart API, bypassing RapidAPI entirely. **No API key needed, no cost.** A 500ms sleep between ticker fetches keeps the request rate well under Yahoo's ~2 req/sec limit.

| Job | Cron | Client | Req/run | Monthly cost |
|---|---|---|---|---|
| 5m bar sync — OMX30 dataset | `0 */10 9-17 * * MON-FRI` | `YahooFinanceDirectClient` | ~29 | **$0** (free, no RapidAPI) |

**Tier 1 — Daily (live signal securities only, ~40 tickers)**

| Job | Cron | Req/run | Monthly |
|---|---|---|---|
| Price sync — portfolio + macro symbols | `0 0 18 * * MON-FRI` | ~40 | ~880 |
| HedgeIndex macro sync | `0 10 18 * * MON-FRI` | 6 (subset of above) | ~0 extra |

**Tier 2 — Weekly (full universe, all ~900 securities)**

| Job | Cron | Req/run | Monthly |
|---|---|---|---|
| Full price sync | `0 0 6 * * SAT` | ~900 | ~3,600 |

**Tier 3 — Monthly (fundamental metadata, ~600 actual stocks)**

| Job | Cron | Req/run | Monthly |
|---|---|---|---|
| Metadata update (4 req × 600 stocks) | `0 0 7 1 * *` | ~2,400 | ~2,400 |

**RapidAPI budget (Tier 1+2+3 only): ~6,880 / 10,000 req/month** — leaves ~3,120 headroom.

**Note:** `Scheduler.java` has `@EnableScheduling` enabled with four methods (`tier0IntradaySync`, `tier1DailySync`, `tier2WeeklySync`, `tier3MonthlyMetadata`) driven by cron properties. Skip logic is built in at each tier.

## Data Prerequisites

| Needed | Status | Notes |
|---|---|---|
| Swedish stock daily OHLCV | ✅ Exists | 977 securities in `YAHOO-SWEDISH` dataset; multi-year history via `RapidApiManager` |
| OMXS30 constituents (daily prices) | ✅ Exists | 29 tickers in `YAHOO-OMX30` dataset; used for Dagstrategin universe |
| OMXS30 benchmark (`^OMX`) | ✅ Exists | Present in `YAHOO-SWEDISH` dataset (line 973 of CSV); also `^OMXSPI` (all-share), `^NORDIC`, `^NORDICPI` are available |
| Beta, PEG, Revenue Growth, Dividend Yield | ⚠️ Partial | `yoyGrowth` fetched on security insert; `beta`, `pegRatio`, `dividendYield` fields exist — populated via `updateSecurityMetaData()`. Run Tier 3 sync to backfill. |
| HedgeIndex new indicators | ✅ CSV complete | All planned tickers added to `YAHOO-INDEX-World indexes.csv`; VSTOXX, FX pairs, DXY, rates, yield curve, OMX vs STOXX50 all implemented in `HedgeIndexService`. Remaining unimplemented: A/D line, HY OAS, TED Spread (no Yahoo Finance ticker) |

## Yahoo Finance Direct Client (Tier-0)

Tier-0 intraday fetching uses `YahooFinanceDirectClient` — a direct call to Yahoo Finance's native v8 chart API, bypassing RapidAPI. No API key, no cost.

**Endpoint:** `GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=5m&range=5d`

- Same Yahoo Finance ticker format already in use (e.g. `VOLV-B.ST`, `ERIC-B.ST`)
- `range=5d` ensures the 7-day retention window is populated on restart
- `User-Agent: Mozilla/5.0` header avoids occasional 401 rejections
- 500ms sleep between ticker fetches keeps rate well under ~2 req/sec limit

**Fallback:** The v8 endpoint is unofficial. If Yahoo changes the format or adds authentication, re-enable `RapidApiManager.getHistory()` for intraday by reverting `IntradayDataService` to inject `RapidApiManager` instead of `YahooFinanceDirectClient`. All errors are caught and logged — a single ticker failure does not crash the pipeline.

**Config:** `yahoo.finance.direct.base-url` (default: `https://query1.finance.yahoo.com`) — override for testing.
