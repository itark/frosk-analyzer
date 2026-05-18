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

**5-min intraday is cost-prohibitive** at this tier — do not add intraday calls without upgrading the plan.

### Recommended Sync Schedule

**Total securities in DB: ~900.** Daily sync of all 900 = 900 × 22 trading days = **19,800 req/month** — nearly 2× the free quota. A tiered approach is required.

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

**Budget: ~8,002 / 10,000 req/month** — leaves ~2,000 headroom for manual runs and growth.
(Tier-0 adds ~1,122 req/month: 1 API call × ~51 ticks/day × 22 trading days.)

> ⚠️ **The numbers above are wrong for the actual Tier-0 load.** See `intraday-pipeline.md` Cost Analysis: Tier-0 alone is ~32,538 req/month (29 tickers × 51 ticks/day × 22 days), which is 3× the free quota. **See Pending Changes below for the cost fix.**

**Note:** `Scheduler.java` has `@EnableScheduling` enabled with four methods (`tier0IntradaySync`, `tier1DailySync`, `tier2WeeklySync`, `tier3MonthlyMetadata`) driven by cron properties. Skip logic is built in at each tier.

## Data Prerequisites

| Needed | Status | Notes |
|---|---|---|
| Swedish stock daily OHLCV | ✅ Exists | 977 securities in `YAHOO-SWEDISH` dataset; multi-year history via `RapidApiManager` |
| OMXS30 constituents (daily prices) | ✅ Exists | 29 tickers in `YAHOO-OMX30` dataset; used for Dagstrategin universe |
| OMXS30 benchmark (`^OMX`) | ✅ Exists | Present in `YAHOO-SWEDISH` dataset (line 973 of CSV); also `^OMXSPI` (all-share), `^NORDIC`, `^NORDICPI` are available |
| Beta, PEG, Revenue Growth, Dividend Yield | ⚠️ Partial | `yoyGrowth` fetched on security insert; `beta`, `pegRatio`, `dividendYield` fields exist — populated via `updateSecurityMetaData()`. Run Tier 3 sync to backfill. |
| HedgeIndex new indicators | ✅ CSV complete | All planned tickers added to `YAHOO-INDEX-World indexes.csv`; VSTOXX, FX pairs, DXY, rates, yield curve, OMX vs STOXX50 all implemented in `HedgeIndexService`. Remaining unimplemented: A/D line, HY OAS, TED Spread (no Yahoo Finance ticker) |

## Pending Changes

### Replace Tier-0 intraday fetching with direct Yahoo Finance v8 API (HIGH priority — cost saving)

**Problem:** RapidAPI's `yahoo-finance15` is a paid proxy around Yahoo Finance's own endpoints. Tier-0 alone consumes ~32,538 API calls/month — 3× the free quota — making an upgraded paid plan mandatory.

**Solution:** Call Yahoo Finance's native v8 chart API directly. This is the same underlying data, free of charge, no API key required. It is the same source used by the popular open-source `yfinance` library. Rate limit is approximately 2 req/sec, which is well within the 10-minute tick cadence for 29 tickers.

**Free endpoint (intraday 5-min bars):**
```
GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=5m&range=1d
```
- `{symbol}` uses the same Yahoo Finance ticker format already in use (e.g. `VOLV-B.ST`, `ERIC-B.ST`)
- `range=1d` returns today's 5-min bars; use `range=5d` for a rolling 5-day window
- No `x-rapidapi-key` or `x-rapidapi-host` headers needed
- Add a `User-Agent: Mozilla/5.0` header to avoid occasional 401 rejections

**Response shape (differs from RapidAPI wrapper):**
```json
{
  "chart": {
    "result": [{
      "timestamp": [1716800100, 1716800400, ...],
      "indicators": {
        "quote": [{
          "open":  [234.5, ...],
          "high":  [235.1, ...],
          "low":   [234.2, ...],
          "close": [234.9, ...],
          "volume":[12300, ...]
        }]
      }
    }]
  }
}
```
Timestamps are Unix epoch seconds in UTC. Convert to `ZonedDateTime` with `ZoneId.of("Europe/Stockholm")` to align with Swedish market hours.

**Implementation plan:**

1. **Create `YahooFinanceDirectClient`** — new `@Component` in `nu.itark.frosk.dataset`, using Spring `WebClient`. No API key fields. Add `User-Agent` header. Implement one method:
   ```java
   public YahooChartDTO getIntradayBars(String symbol, String interval, String range)
   ```
   Create matching DTO classes: `YahooChartDTO`, `YahooChartResult`, `YahooQuoteIndicators`.

2. **Create `YahooChartDTO` model classes** under `nu.itark.frosk.dataset.yhfinance.model` (or a new `direct` sub-package). Map the `chart.result[0]` structure above.

3. **Update `IntradayDataService.syncAndBuildSeries()`** — replace the call to `RapidApiManager.getHistory(ticker, 5m)` with `YahooFinanceDirectClient.getIntradayBars(ticker, "5m", "5d")`. The `range=5d` window ensures the retention window is always fully populated on restart.

4. **Add a 500ms sleep between ticker fetches** in `IntradayDataService` to stay well under the ~2 req/sec rate limit. The 10-minute tick window has plenty of slack for 29 sequential calls with a half-second delay.

5. **Keep `RapidApiManager` unchanged** — it continues to serve Tier 1/2/3 (daily prices, metadata). Those tiers use ~7,880 req/month, well within the free quota once Tier-0 is removed.

6. **Update `intraday-pipeline.md`** to reflect the new client.

7. **Update `application.properties`** — no new properties needed (no API key). Optionally add `yahoo.finance.direct.base-url=https://query1.finance.yahoo.com` for testability.

**Risk note:** The Yahoo Finance v8 endpoint is unofficial and undocumented. Yahoo can change the response format or add authentication without notice. If it breaks, the fallback is to re-enable `RapidApiManager.getHistory()` for intraday. Mitigate by wrapping the call in a try/catch in `IntradayDataService` and logging a warning (rather than crashing the pipeline) if the response is null or malformed.
