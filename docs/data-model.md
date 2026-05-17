# Data Model

## Data Model for Swedish Stocks

Swedish daily prices use the **same two tables** as all other securities — no separate entity needed.

**`security` table** (`Security.java`):

| Column | Java field | Notes |
|---|---|---|
| `id` | `long id` | PK; used as `BarSeries.name` |
| `name` | `String name` | Ticker, e.g. `VOLV-B.ST`, `ERIC-B.ST`, `^OMX` (OMXS30 index — present in `YAHOO-SWEDISH` dataset) |
| `description` | `String description` | Company name / label |
| `database` | `String database` | `"YAHOO"` for all Swedish stocks |
| `quote_currency` | `String quoteCurrency` | e.g. `"SEK"` |
| `beta` | `Double beta` | 5Y monthly beta — already stored |
| `peg_ratio` | `Double pegRatio` | Already stored |
| `yoy_growth` | `Double yoyGrowth` | Revenue growth YoY — already stored |
| `trailing_pe` / `forward_pe` | `Double` | Already stored |
| `trailing_eps` / `forward_eps` | `Double` | Already stored |
| `enterprise_value` | `Long enterpriseValue` | Controls `active` flag |
| `dividend_yield` | `Double dividendYield` | Computed from `lastDividendValue / price` via `updateSecurityMetaData()` |
| `sector` | `String sector` | e.g. `"Technology"`, `"Industrials"` — populated via `updateSecurityMetaData()` |
| `active` | `boolean active` | Auto-set: `enterpriseValue > 500_000_000` via `@PreUpdate` |

**`security_price` table** (`SecurityPrice.java`):

| Column | Java field | Type |
|---|---|---|
| `security_id` | `Long securityId` | FK to `security.id` |
| `timestamp` | `Date timestamp` | Bar date |
| `open` / `high` / `low` / `close` | `BigDecimal` | OHLC, precision 12/6 |
| `volume` | `Long volume` | |

**Filter to Swedish stocks:** `security.name.endsWith(".ST")` — all Nasdaq Stockholm tickers use the `.ST` suffix.

**`active` flag caveat:** `BarSeriesService.getDataSet(Database)` filters to `active=true`. Stocks with `enterpriseValue ≤ 500M` are excluded. May filter out some mid/small-caps in Månadsportföljen — check count with `SELECT COUNT(*) FROM security WHERE active = true AND name LIKE '%.ST'`.

**Fundamental data:** `beta`, `pegRatio`, `yoyGrowth`, `trailingPe`, `forwardPe`, `dividendYield` stored on `Security`. All populated by `RapidApiManager` via `updateSecurityMetaData()`.

## Intraday Entities

| Entity | Table | Purpose |
|---|---|---|
| `IntradayBar` | `intraday_bar` | Rolling 5-minute OHLCV window (`retention.days` deep); unique on `(security_id, bar_timestamp, interval_code)` |
| `IntradaySignal` | `intraday_signal` | Persistent signal log — never deleted; one row per BUY/SELL event with close, EMA fast, EMA slow, RSI snapshots (DB columns still named `ema9`, `ema21`, `rsi7` for backwards compat) |
