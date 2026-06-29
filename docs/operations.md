# Operations — Data Layer & Sync Schedule

## Data Layer — YahooFinanceDirectClient

All Yahoo market data is fetched through `YahooFinanceDirectClient` — direct calls to Yahoo Finance's public v8 (chart) and v10 (quoteSummary) endpoints. **No API key, no cost, no monthly quota.** This is the single entry point for all Yahoo data — any new data needed must be added here. (Crypto price data uses the Coinbase API directly via `CryptoIntradayDataService` / `ProductService`.)

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

**Active endpoints (`YahooFinanceDirectClient.java`):**

| Endpoint | Use | Called from |
|---|---|---|
| `/v8/finance/chart/{symbol}?interval=1d` | Daily OHLCV | `YAHOODataManager.syncronize()` |
| `/v8/finance/chart/{symbol}?interval=15m` | Intraday bars | `IntradayDataService` |
| `/v10/finance/quoteSummary?modules=defaultKeyStatistics` | Beta, PEG, EPS, P/E, enterprise value | `updateSecurityMetaData()` |
| `/v10/finance/quoteSummary?modules=incomeStatementHistory` | Revenue YoY growth | `updateSecurityMetaData()` |
| `/v10/finance/quoteSummary?modules=recommendationTrend` | Analyst ratings | `updateSecurityMetaData()` |
| `/v10/finance/quoteSummary?modules=assetProfile` | Sector, industry | `updateSecurityMetaData()` → `setSectorData()` |

Index tickers (`^` prefix) and futures (`=F` suffix) are skipped by `updateSecurityMetaData()` — only called on actual stock tickers.

### Rate / Resilience Notes

The v8/v10 endpoints are unofficial and free, but not unlimited — Yahoo can throttle or IP-block on excessive request rates. The data layer stays polite via:
- A `User-Agent: Mozilla/5.0` header (avoids occasional 401 rejections)
- Crumb/cookie acquisition with a crumb-refresh retry on 401 (`fetchQuoteSummaryModule`)
- A configurable delay between consecutive ticker fetches, `yahoo.fetch.delay.ms` (default 300ms, set to 0 to disable). Applied in **both** the full-universe daily/weekly loop (`YAHOODataManager.getStocks()`, ~900 tickers, Tier-2) and the Tier-0 intraday loop (`IntradayDataService`).

### Sync Schedule

**Total Yahoo securities in DB: ~900.** The tier split is no longer driven by an API request budget (data is free) — it spreads load across the week and matches each data type's natural refresh cadence.

**Tier 0 — Intraday (all ~29 OMX30 securities, every 10 min)**

| Job | Cron | Client | Req/run |
|---|---|---|---|
| 15m bar sync — OMX30 dataset | `0 */10 8-17 * * MON-FRI` | `YahooFinanceDirectClient` | ~29 |

**Tier 1 — Daily (live signal securities only, ~40 tickers)**

| Job | Cron | Req/run |
|---|---|---|
| Price sync — portfolio + macro symbols | `0 0 18 * * MON-FRI` | ~40 |
| HedgeIndex macro sync | (subset of above) | 6 |

**Tier 2 — Weekly (full universe, all ~900 securities)**

| Job | Cron | Req/run |
|---|---|---|
| Full price sync | `0 0 6 * * SAT` | ~900 |

**Tier 3 — Monthly (fundamental metadata, ~600 actual stocks)**

| Job | Cron | Req/run |
|---|---|---|
| Metadata update (4 req × 600 stocks) | `0 0 7 1 * *` | ~2,400 |

**Note:** `Scheduler.java` has `@EnableScheduling` enabled with four methods (`tier0IntradaySync`, `tier1DailySync`, `tier2WeeklySync`, `tier3MonthlyMetadata`) driven by cron properties. Skip logic is built in at each tier.

## Data Prerequisites

| Needed | Status | Notes |
|---|---|---|
| Swedish stock daily OHLCV | ✅ Exists | 977 securities in `YAHOO-SWEDISH` dataset; multi-year history via `YahooFinanceDirectClient` |
| OMXS30 constituents (daily prices) | ✅ Exists | 29 tickers in `YAHOO-OMX30` dataset; used for Dagstrategin universe |
| OMXS30 benchmark (`^OMX`) | ✅ Exists | Present in `YAHOO-SWEDISH` dataset (line 973 of CSV); also `^OMXSPI` (all-share), `^NORDIC`, `^NORDICPI` are available |
| Beta, PEG, Revenue Growth, Dividend Yield | ⚠️ Partial | `yoyGrowth` fetched on security insert; `beta`, `pegRatio`, `dividendYield` fields exist — populated via `updateSecurityMetaData()`. Run Tier 3 sync to backfill. |
| HedgeIndex new indicators | ✅ CSV complete | All planned tickers added to `YAHOO-INDEX-World indexes.csv`; VSTOXX, FX pairs, DXY, rates, yield curve, OMX vs STOXX50 all implemented in `HedgeIndexService`. Remaining unimplemented: A/D line, HY OAS, TED Spread (no Yahoo Finance ticker) |

## Yahoo Finance Direct Client (Tier-0)

Tier-0 intraday fetching uses `YahooFinanceDirectClient` — a direct call to Yahoo Finance's native v8 chart API. No API key, no cost.

**Endpoint:** `GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=5m&range=5d`

- Same Yahoo Finance ticker format already in use (e.g. `VOLV-B.ST`, `ERIC-B.ST`)
- `range=5d` ensures the 7-day retention window is populated on restart
- `User-Agent: Mozilla/5.0` header avoids occasional 401 rejections
- `yahoo.fetch.delay.ms` (default 300ms) between ticker fetches keeps the rate well under ~2 req/sec

**Resilience:** The v8 endpoint is unofficial. All errors are caught and logged per ticker — a single ticker failure does not crash the pipeline. If Yahoo changes the format or adds authentication, the fix lives entirely in `YahooFinanceDirectClient` (e.g. crumb/cookie handling in `fetchQuoteSummaryModule`).

**Config:** `yahoo.finance.direct.base-url` (default: `https://query1.finance.yahoo.com`) — override for testing.
