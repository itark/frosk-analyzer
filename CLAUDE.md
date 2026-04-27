# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot application implementing a **hedge fund-style, layered trading framework** using ta4j 0.16. The system has three layers:

1. **Macro signal (HedgeIndex)** — a daily risk-on/risk-off score derived from 18 macro indicators (volatility, commodities, equities, FX, inflation, rates, credit). Gates all equity strategy entries.
2. **Equity selection** — rule-based stock screening (momentum, Beta, growth, technical levels) aligned with the current macro regime.
3. **Hedging** — options-based protection (protective puts, collars, spreads) sized to regime severity.

The project includes two Swedish stock portfolio strategies (Månadsportföljen and Dagstrategin — see **Roadmap** below), both gated by the HedgeIndex.

## Build and Run

```bash
./build-frosk.sh          # builds gdax-java dependency + frosk-analyzer, then starts app
mvn clean install -DskipTests
mvn spring-boot:run
```

## Testing

All integration tests extend `BaseIntegrationTest`, which bootstraps the full Spring context with `@ActiveProfiles("test")` and mocks `FroskStartupApplicationListener` to prevent startup side effects. The test database is configured in `src/test/resources/application-test.properties`.

```bash
mvn test                                             # run all tests
mvn test -Dtest=TestJStrategyAnalysis#runSTLT        # run a single test method
mvn test -Dtest=TestJStrategyAnalysis                # run all tests in one class
```

## Key Configuration (`application.properties`)

| Property | Purpose |
|---|---|
| `frosk.strategy.only` | Run only this strategy name (empty = all) |
| `frosk.runallstrategies` | true = run all strategies × all securities |
| `frosk.updatehedgeindex` | true = refresh the hedge index from DB before running |
| `frosk.buildportfolio` | true = call `portfolioService.build()` after `runInstall()` |
| `frosk.strategies.exclude` | Comma-separated strategy names to skip — **all hedge/FX/index strategies and the two Dagstrategin + OMXS30Swing strategies are excluded from the default all-strategies batch run** (they run via their own dedicated code paths) |
| `frosk.run.dagstrategin` | true = run Dagstrategin strategies on OMX30 after startup |
| `frosk.swedish.longterm.topN` | Max SLMS positions in portfolio (annotation fallback: 20; `application.properties` sets 25) |
| `frosk.swedish.longterm.maxVolatility` | Max annualized volatility for SLMS entry (default: 0.40) |
| `frosk.portfolio.min.sqn` | Min SQN for any position to appear in portfolio snapshot (default: 1.0) |
| `frosk.portfolio.min.win.rate` | Min profitable-trades ratio for any position (default: 0.35) |
| `frosk.portfolio.other.topN` | Max combined HighLander + ShortTermMomentum positions, ranked by SQN (default: 10) |
| `frosk.slms.stoploss.percent` | Hard stop-loss % below entry for ShortTermMomentumLongTermStrengthStrategy (default: 15.0) |
| `frosk.highlander.stoploss.percent` | Hard stop-loss % below entry for HighLanderStrategy (default: 20.0) |

## Architecture

```
HighLander (startup runner)
  ├─ StrategyAnalysis.run(strategy, securityId)
  │    ├─ Case 1: null/null  → all strategies × all securities (batch)
  │    │    └─ HedgeIndexService.warmCache() called first
  │    ├─ Case 2: strategy + null  → one strategy × all securities
  │    └─ Case 3: strategy + securityId → one strategy × one security
  │         └─ StrategyExecutor.run() [@Transactional(REQUIRES_NEW)]
  │              ├─ builds BarSeries via BarSeriesService
  │              ├─ builds ta4j Strategy via StrategiesMap.getStrategyToRun()
  │              ├─ runs backtest via BarSeriesService.runConfiguredStrategy()
  │              ├─ upserts FeaturedStrategy
  │              └─ replaces StrategyTrade + StrategyIndicatorValue (saveAll)
  ├─ StrategyAnalysis.runHedgeIndexStrategies()  → syncs + runs 14 macro strategies → hedgeIndexService.update()
  ├─ StrategyAnalysis.runDagstrateginStrategies() → runs DailyBreakout + DailyOversoldBounce on OMX30
  └─ PortfolioService.build() → snapshots open positions with sector cap + tiered HedgeIndex sizing
```

## FeaturedStrategy — Central Result Entity

`FeaturedStrategy` is one row per **(strategy name × security name)**. Key fields:

- `open` — true if the last trade in the backtest is a BUY without a matching SELL (position currently open)
- `entryDate`, `entryPrice` — from the last open BUY trade
- `sqn`, `expectency`, `profitableTradesRatio`, `totalGrossReturn`, `totalProfit`
- `@OneToMany StrategyTrade` — individual BUY/SELL pairs with `pnl`, `grossProfit`, `price`, `amount`
- `@OneToMany StrategyIndicatorValue` — latest indicator snapshots for charting

`StrategyExecutor` always deletes+re-inserts `StrategyTrade` and `StrategyIndicatorValue` on each run. A `FeaturedStrategy` row is created on first run and updated on subsequent runs.

## Adding a New Strategy

Every strategy must be registered in **five places** in `StrategiesMap`:

1. `@Autowired` field declaration
2. `buildStrategiesMap()` — adds class simple name to the string list
3. `getStrategies(BarSeries)` — calls `buildStrategy(series)` and adds to list
4. `getStrategyToRun(String, BarSeries)` — `else if` branch returning `buildStrategy(series)`
5. `getIndicatorValues(String, BarSeries)` — `else if` branch returning `getIndicatorValues()`

Strategy class must:
- Be annotated `@Component`
- Extend `AbstractStrategy`
- Implement `IIndicatorValue` (provides `getIndicatorValues()`)
- Call `super.setInherentExitRule()` and `indicatorValues.clear()` at the start of `buildStrategy()`

## Existing Strategies — Inventory

| Class | Type | Description |
|---|---|---|
| `HighLanderStrategy` | Long-term | Core equity strategy — gated by HedgeIndex; entries only in risk-on regime. Exit: composite sub-strategy exits OR 20% hard stop-loss. |
| `ShortTermMomentumLongTermStrengthStrategy` | Medium-term | Combines short-term momentum with long-term strength filter. Exit: price < SMA(50) OR SMA(10) < SMA(20) OR HedgeIndex risk-off OR 15% stop-loss. |
| `EngulfingStrategy` | Short-term | Candlestick pattern — uses `TradeOnCurrentCloseModel` |
| `GoldStrategy` | Commodity | Gold-specific rules — uses `TradeOnCurrentCloseModel` |
| `OMXS30SwingStrategy` | Swing (2–10 days) | **"Nordic Momentum Filter"** — Fredrik's own strategy. Entry: EMA(20) > EMA(50) AND RSI(14) crosses above 40 AND OBV > OBV SMA(5). Exit: RSI(14) > 65 OR ATR stop-loss (2× ATR) OR stop-gain (3× ATR × 2) OR max 10 bars held. Uses two custom inner rules: `AtrStopLossRule` and `MaxBarsHeldRule`. 137 lines. |
| `SwedishLongTermMomentumStrategy` | Quarterly factor | **Månadsportföljen** — 6M/12-1 momentum, low-vol filter, relative strength vs ^OMX, PEG/Beta gates, tiered HedgeIndex. 231 lines. |
| `DailyBreakoutStrategy` | Swing (2–10 days) | **Dagstrategin A** — 20-day breakout on elevated volume in uptrend. Uptrend filter, 0.5% breakout threshold, stop-loss at 5-bar low. 95 lines. |
| `DailyOversoldBounceStrategy` | Swing (2–10 days) | **Dagstrategin B** — RSI(14) < 30 + lower Bollinger Band bounce in uptrend. Volume confirmation, stop-loss at 3-bar low. 111 lines. |

> All strategies extend `AbstractStrategy` and implement `IIndicatorValue`. All must be registered in `StrategiesMap` at 5 points. `EngulfingStrategy` and `GoldStrategy` use `TradeOnCurrentCloseModel`; all others use `TradeOnNextOpenModel`.

---

## HedgeIndex — Full Scoring Model

`HedgeIndexService` computes a daily **risk-off score** by summing points from up to 18 macro indicators. Each triggered condition adds +1 to the score. Higher score = more risk-off = reduce equity exposure.

### Indicator Table

Sweden is the primary market. US indicators are retained where they drive global risk appetite that flows directly into OMXS30. US-only indicators with no global read-through have been removed or replaced with European/Swedish equivalents.

| Category | Indicator | Description | Rule / Trigger | Points | Implemented |
|---|---|---|---|---|---|
| Volatility | **VIX** | CBOE Volatility Index — the primary global fear gauge; spikes in US implied vol propagate immediately to Swedish equities | VIX > 25 AND rising (5-day ROC positive) | +1 | ✅ |
| Volatility | **VSTOXX** | Euro Stoxx 50 Volatility Index (`^V2TX`) — European equivalent of VIX; more directly relevant to OMXS30 than US vol alone | VSTOXX > 25 AND rising (5-day ROC positive) | +1 | ✅ |
| Commodities | **Crude Oil (WTI)** | West Texas Intermediate crude price — sharp drops signal demand destruction or global recession fears | Drops > 8% in last 5 trading days | +1 | ✅ |
| Commodities | **Gold** | Safe-haven asset — breakouts to new highs signal capital rotating out of risk assets into protection | Closes above previous 20-day high (breakout) | +1 | ✅ |
| Equities | **S&P 500** | Broad US equity benchmark — trading below the 200-day SMA signals a global long-term downtrend; OMXS30 historically follows within days | Close below 200-day SMA | +1 | ✅ |
| Equities | **OMX vs STOXX50** | Relative performance of Swedish vs. European equities — when OMXS30 meaningfully underperforms Euro Stoxx 50, domestic risk-off is leading the broader market | OMXS30 30-day return < STOXX50 30-day return by more than 3 percentage points | +1 | ✅ |
| FX | **EUR/USD** | Sweden's largest trading partner is the Eurozone — EUR weakness directly compresses Swedish exporter margins and signals Eurozone fragility | EUR/USD drops > 3% in last 10 trading days OR closes below 1-year low | +1 | ✅ |
| FX | **USD/JPY** | Japanese yen is a safe-haven currency — sharp USD/JPY moves signal global carry unwind and broad risk-off positioning | USD/JPY 5-day ROC > +2% (rapid yen weakening = carry unwind risk) | +1 | ✅ |
| FX | **AUD/USD** | Australian dollar is a global growth/commodity proxy — sharp drops signal declining global risk appetite and falling commodity demand | Drops > 2% in last 5 trading days | +1 | ✅ |
| FX | **DXY (USD Index)** | US Dollar Index — a strong rising dollar tightens global financial conditions, pressures EM and export-driven economies including Sweden | DXY > 105 AND rising (5-day ROC positive) | +1 | ✅ |
| Inflation | **Swedish KPIF YoY** | Riksbank's primary inflation target — skipped; no Yahoo Finance ticker available | — | ⏭️ |
| Interest Rates | **10Y Treasury Yield** | US 10-year yield drives global discount rates — high and rising US rates lift Swedish long rates via global bond market linkage | 10Y > 4.5% AND rising (5-day ROC positive) | +1 | ✅ |
| Yield Curve | **2Y–10Y Spread** | US yield curve spread — deep inversion historically precedes global recessions by 6–18 months; reliable leading indicator even for non-US markets. Uses `^TNX` − `^IRX` as proxy for 10Y − 3M spread | 10Y − 13W < −50 bps (deep inversion) | +1 | ✅ |
| Market Breadth | **Advance/Decline** | NYSE Advance/Decline line — broad US market participation; a sustained decline signals the rally is narrowing globally, not just in the US | A/D line falling for 5+ consecutive days | +1 | ❌ |
| Credit Stress | **High-Yield OAS** | US High-Yield Option-Adjusted Spread — not available via Yahoo Finance15 API; would need a dedicated data source | HY OAS > 500 bps | +1 | ⏭️ |
| Liquidity | **TED Spread** | SOFR–T-bill spread — not available via Yahoo Finance15 API; would need a dedicated data source | SOFR–T-bill spread > 50 bps | +1 | ⏭️ |

> **Note:** thresholds are starting points — tune to your universe and regime. Use smoothed/ROC versions to avoid single-day noise. Use `BarSeriesAligner` to align time series when comparing assets.
>
> **Removed indicators:** SKEW (CBOE tail-risk index — US options market only, no meaningful read for Swedish equities) and SDEX (S&P 500 constituent dispersion — purely US internal measure). NASDAQ vs S&P replaced by OMX vs STOXX50 — `NasdaqVsSPStrategy` removed from `HedgeIndexService.update()` (step 12); `OMXvsSTOXX50Strategy` is the sole equities-relative indicator. US CPI replaced by Swedish KPIF. VSTOXX added as a more relevant volatility gauge alongside VIX. EUR/USD added given Sweden's deep Eurozone trade dependency.

**Volatility cluster rule:** if both VIX and VSTOXX are risk-off simultaneously, increase hedge size by +1 notch regardless of total score.

### Regime Classification

| Total Score | Regime | Equity Exposure | Posture |
|---|---|---|---|
| 0–3 | **Strong Risk-On** | 80–100% | Long high-beta growth/momentum; minimal hedge |
| 4–7 | **Cautious / Transition** | 40–70% | Selective longs; collars or protective puts on core names |
| 8–11 | **Neutral / Defensive** | 10–40% | Defensive sectors, high-quality dividends; long Gold, long VIX |
| 12+ | **Strong Risk-Off** | 0–10% or net short | Long VIX/volatility, long Gold, short indices via bear puts or inverse ETFs |

### HedgeIndex Performance

`HedgeIndexService` maintains an in-memory cache (`Map<Long, Integer>` keyed on `Date.getTime()` millis) to avoid per-bar DB queries during strategy evaluation. Call `warmCache()` before running bulk strategies; the cache is automatically cleared after `update()`. Two accessors: `risk(ZonedDateTime)` returns boolean (score > threshold), `getScore(ZonedDateTime)` returns the raw integer score for tiered decisions.

**Remaining indicators backlog:** Only 3 indicators still missing an implementation: A/D line (NYSE Advance/Decline — no Yahoo Finance ticker), HY OAS (US High-Yield OAS — not available via Yahoo Finance15), TED Spread (SOFR–T-bill spread — not available via Yahoo Finance15). Swedish KPIF is permanently skipped (no Yahoo Finance ticker). All other indicators — VSTOXX, OMX vs STOXX50, EUR/USD, USD/JPY, AUD/USD, DXY, 10Y Treasury, 2Y–10Y spread — are fully implemented and their tickers are registered in `YAHOO-INDEX-World indexes.csv`.

---

## Equity Selection Model

When the HedgeIndex is risk-on (score 0–7), the equity selection layer screens for the best long candidates. In risk-off (8+), favour defensive sectors or move to cash/hedge.

### Stock Screening Criteria (from Yahoo Finance via `RapidApiManager`)

| Factor | Rule | Source |
|---|---|---|
| Beta (5Y Monthly) | > 1.3 (high-beta growth names) | `/stock/get-statistics` |
| Revenue Growth YoY | > 15% | `/stock/get-financials` |
| PEG Ratio | < 1.5 (if missing: fallback — see below) | `/stock/get-statistics` |
| Golden Cross | Price above both 50-day AND 200-day SMA | Computed from price series |
| Relative Strength | Outperforms SPY over 3 months | Computed: `ROCIndicator(stock, 63)` vs `ROCIndicator(SPY, 63)` |

**PEG fallback rule:** if PEG is unavailable, use Revenue Growth > 25% AND Forward P/E < sector average. Flag stock as "speculative" with lower position weight.

**Risk-off equity rules:** favour Utilities, Consumer Staples; screen for mean-reversion shorts or breakdown setups in high-beta names.

**Split-adjustment:** always use adjusted close prices. Back-adjust series before computing returns or indicators.

---

## Hedging Layer (Planned)

The options hedging layer sizes protection based on HedgeIndex regime. Not yet implemented — build after equity selection layer is validated.

| Strategy | Use Case | Description |
|---|---|---|
| Protective Put | Long in neutral regime (score 4–7) | Long stock + buy OTM put to cap downside |
| Collar | Long with elevated risk | Long stock + sell OTM call + buy OTM put |
| Call Spread | Directional long, risk-on | Buy call + sell higher-strike call |
| Bear Put Spread | Directional short, risk-off | Buy put + sell lower-strike put |

---

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

---

## BarSeriesService

- `getDataSet(Database)` — batch-loads all prices in one query (`findBySecurityIdInOrderBySecurityIdAscTimestampAsc`), groups in memory; avoids N+1.
- `getDataSet(String, boolean, boolean)` — loads by security name; `api=true` fetches live from Coinbase.
- Trade execution model: `EngulfingStrategy` and `GoldStrategy` use `TradeOnCurrentCloseModel`; all others use `TradeOnNextOpenModel`.

## H2 Console

Available at `/h2-console` when the app is running. Connection URL is in `application.properties`.

---

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

**Budget: ~6,880 / 10,000 req/month** — leaves ~3,100 headroom for manual runs and growth.

**Note:** `Scheduler.java` has `@EnableScheduling` enabled with three methods (`tier1DailySync`, `tier2WeeklySync`, `tier3MonthlyMetadata`) driven by `scheduler.tier1.cron`, `scheduler.tier2.cron`, `scheduler.tier3.cron` properties. Skip logic is built in: `YAHOODataManager.syncronize()` checks the latest stored date per security and skips if already current.

---

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

---

## Roadmap — Swedish Stock Portfolios

The system already holds years of Swedish stock price history. The goal is to use that data to backtest and run two distinct strategies, both gated by the existing HedgeIndex.

### Strategy 1: Månadsportföljen (Long-Term Factor Portfolio)

**Goal:** Hold 15–25 quality Swedish large/mid-cap stocks, rebalanced quarterly. Beat OMXS30 over 3–5 years.

**Universe:** Active Swedish stocks (`active=true AND name LIKE '%.ST'`). The `active` flag filters to `enterpriseValue > 500M` — this is appropriate for a large/mid-cap strategy and requires no override.

**Selection factors — Swedish adaptation of the equity model:**

| Factor | ta4j Implementation | Weight | Notes |
|---|---|---|---|
| 6-month momentum | `ROCIndicator(closePriceSeries, 126)` | 30% | 126 trading days ≈ 6 months |
| 12-month momentum (12-1) | `(close[t-21] - close[t-252]) / close[t-252]` | 30% | Skips most recent month to avoid short-term reversal — **not** `ROCIndicator(series, 252)` which includes the reversal month |
| Low volatility | `StandardDeviationIndicator(closePriceSeries, 252)` | 20% | Inverted in composite score — lower vol = higher score |
| Relative strength vs OMXS30 | `ROCIndicator(stock, 63)` minus `ROCIndicator(^OMX, 63)` | 20% | 3-month outperformance vs benchmark |
| Golden Cross | Price > SMA(50) AND Price > SMA(200) | Hard filter | Binary — stock excluded from ranking if not met |
| Dividend yield | From `Security.dividendYield` | Soft tilt | Populated from statistics; used for portfolio ranking, not as hard entry filter |

**Composite score:** Each factor is rank-normalised across the candidate universe (rank 1 = worst, rank N = best), then weighted and summed. Stocks failing the Golden Cross hard filter are excluded before ranking.

**Entry/exit rules:**
- Entry: stock enters top-N ranked list at quarterly rebalance date (first trading day of Jan, Apr, Jul, Oct) AND Golden Cross condition met
- Exit: stock drops below rank threshold OR composite score falls below bottom quartile OR death cross (close < SMA200)
- Max weight per stock: 10%; max per sector: 30% (requires `sector` field — see below)
- HedgeIndex gate: score 0–3 → full allocation (top-N positions); score 4–7 → reduce to top-N/2 positions, no new entries into lowest-ranked half; score 8+ → no new entries, trim bottom 25% of positions

**Quarterly rebalance implementation note:** ta4j runs bar-by-bar so quarterly date detection requires a helper — check if `bar.getEndTime().getMonth()` is in {Jan, Apr, Jul, Oct} AND `bar.getEndTime().getDayOfMonth() <= 5` (first week of quarter) to approximate the rebalance trigger.

**Class:** `SwedishLongTermMomentumStrategy extends AbstractStrategy implements IIndicatorValue` — ✅ implemented (231 lines). Wired into `StrategiesMap` (all 5 points) and `PortfolioService`.

**Portfolio filter:** `PortfolioService.build()` includes `SwedishLongTermMomentumStrategy` with sector cap, tiered HedgeIndex sizing, and topN limit.

**Properties:** `frosk.swedish.longterm.topN` (default: 25) — max positions. `frosk.swedish.longterm.maxVolatility` (default: 0.40) — max annualized vol for entry.

**Sector field:** `sector` (`String`) column on `Security`. Populated via `RapidApiManager.getModuleAssetProfile()` (`asset-profile` module) called inside `YAHOODataManager.updateSecurityMetaData()`. Run `updateSecurityMetaData()` once to backfill all existing stocks before the 30%-per-sector cap in `PortfolioService` takes effect.

---

### Strategy 2: Dagstrategin (Daily Short-Term Swing)

**Execution model:** Runs entirely on **daily OHLCV bars** — the same data already fetched by `RapidApiManager`. No intraday data required, no new entity or ingestion pipeline needed. After each daily close, `StrategyExecutor` evaluates signals against the updated daily `BarSeries`. A ranked **next-morning watchlist** is produced for the human trader to act on at the open.

**Holding period:** 2–10 trading days. Not day trading — short-term swing trading on daily bars.

**Universe:** OMXS30 constituents (~30 tickers). No additional API cost beyond the existing Tier 1 daily sync.

**Data flow (runs after Tier 1 daily price sync, `0 0 18 * * MON-FRI`):**
1. Tier 1 sync fetches today's daily close for OMXS30 tickers via `RapidApiManager`
2. `StrategyExecutor` runs both Dagstrategin strategies across the OMXS30 universe
3. Signals stored as `StrategyTrade`; metrics updated in `FeaturedStrategy`
4. REST endpoint `GET /dagstrategin/watchlist` returns next-morning ranked candidates

**No new data infrastructure required** — uses existing `BarSeriesService.getDataSet()` and `SecurityPrice` table.

---

**Sub-strategy A: Daily Breakout Momentum**

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

**Sub-strategy B: Daily Oversold Bounce**

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

### Data Prerequisites

| Needed | Status | Notes |
|---|---|---|
| Swedish stock daily OHLCV | ✅ Exists | 977 securities in `YAHOO-SWEDISH` dataset; multi-year history via `RapidApiManager` |
| OMXS30 constituents (daily prices) | ✅ Exists | 29 tickers in `YAHOO-OMX30` dataset; used for Dagstrategin universe |
| OMXS30 benchmark (`^OMX`) | ✅ Exists | Present in `YAHOO-SWEDISH` dataset (line 973 of CSV); also `^OMXSPI` (all-share), `^NORDIC`, `^NORDICPI` are available |
| Beta, PEG, Revenue Growth, Dividend Yield | ⚠️ Partial | `yoyGrowth` fetched on security insert; `beta`, `pegRatio`, `dividendYield` fields exist — populated via `updateSecurityMetaData()`. Run Tier 3 sync to backfill. |
| HedgeIndex new indicators | ✅ CSV complete | All planned tickers added to `YAHOO-INDEX-World indexes.csv`; VSTOXX, FX pairs, DXY, rates, yield curve, OMX vs STOXX50 all implemented in `HedgeIndexService`. Remaining unimplemented: A/D line, HY OAS, TED Spread (no Yahoo Finance ticker) |

---

### Implementation Order

---

#### ✅ Completed — Steps 0–14

| Step | What was done |
|---|---|
| 0 | Initial exploration — `sector`, `dividendYield` on `Security`; HedgeIndex cache understood |
| 1 | FX indicators: `EURUSDStrategy`, `USDJPYStrategy`, `AUDUSDStrategy` + CSV tickers |
| 2 | DXY + European vol + OMX vs STOXX50: `DXYStrategy`, `VSTOXXStrategy`, `OMXvsSTOXX50Strategy`; volatility cluster rule |
| 3 | Rates + yield curve: `TreasuryYieldStrategy`, `YieldCurveSpreadStrategy`; HY OAS + TED Spread skipped (no ticker) |
| 4 | Swedish KPIF skipped (no Yahoo Finance ticker) |
| 5 | `SwedishLongTermMomentumStrategy` (231 lines): quarterly rebalance, 6M/12-1 momentum, golden cross, relative strength vs `^OMX`, PEG/Beta gates, wired at all 5 `StrategiesMap` points |
| 6 | SLMS sector cap (`applySectorCap`), tiered HedgeIndex gate (`HedgeIndexTieredRule`), tiered portfolio sizing (`computeTieredTopN`), low-vol filter, dividend yield stored |
| 7 | Scheduler activated: Tier 1/2/3 `@Scheduled` crons live in `Scheduler.java` |
| 8 | `DailyBreakoutStrategy` (95 lines) + `DailyOversoldBounceStrategy` (111 lines); `GET /dagstrategin/watchlist`; called from `syncTier1()` every weekday |
| 9 | Portfolio quality gate (`passesQualityGate` in `PortfolioService`): SQN ≥ 1.0, win rate ≥ 35%; HighLander + ShortTermMomentum capped at top-10 by SQN; `ShortTermMomentumLongTermStrengthStrategy` now exits on HedgeIndex risk-off OR 15% stop-loss |
| 10 | Validated step 9: build compiles cleanly, all strategies wired at 5 points in `StrategiesMap`, `TestJStrategiesMap` passes, all rule classes (`HedgeIndexTieredRule`, `StopLossRule`, `HedgeIndexRiskOffRule`) verified |
| 11 | Added 20% hard stop-loss to `HighLanderStrategy` via `StopLossRule`; extracts composite entry/exit rules and appends `.or(stopLoss)` to exit; `frosk.highlander.stoploss.percent=20.0` in `application.properties` |
| 12 | Removed `NasdaqVsSPStrategy` from `HedgeIndexService.update()` and its converter method; removed from `frosk.strategies.hedge.strategy` in all properties files (main, test, prod). Strategy remains in `StrategiesMap`. HedgeIndex now has 13 contributing strategies. |
| 13 | Created `BreakoutProfitTargetRule` — 2:1 R:R exit based on entry price vs 5-bar low stop level at entry. Added to `DailyBreakoutStrategy` exit rule: `stopLoss.or(profitTarget).or(hedgeRiskOff)`. |
| 14 | Already implemented — `PortfolioService.PORTFOLIO_STRATEGIES` allowlist (3 equity strategies only) already excludes all macro/hedge strategies; H2 query already has `fs.name != 'HedgeIndexStrategy'` filter. No code change needed. |

> **Results are queryable directly in the H2 console** (`/h2-console`). No additional REST endpoints are planned — the existing portfolio, watchlist, and strategy endpoints are sufficient. New endpoints should only be added when there is a concrete consumer (e.g. a frontend or external integration).

#### Useful H2 queries

```sql
-- Current portfolio: open positions with quality metrics
SELECT fs.security_name, fs.name strategy, fs.sqn, fs.prof_trade_ratio win_rate,
       fs.total_gross_return, t.price entry_price, t.date entry_date
FROM featured_strategy fs
JOIN strategy_trade t ON t.featured_strategy_id = fs.id
WHERE fs.open = true
  AND fs.name != 'HedgeIndexStrategy'
  AND t.type = 'BUY'
  AND t.date = (SELECT MAX(t2.date) FROM strategy_trade t2
                WHERE t2.featured_strategy_id = fs.id AND t2.type = 'BUY')
ORDER BY fs.sqn DESC;

-- Today's new BUY signals
SELECT fs.security_name, fs.name strategy, fs.sqn, t.price, t.date
FROM featured_strategy fs
JOIN strategy_trade t ON t.featured_strategy_id = fs.id
WHERE fs.open = true AND t.type = 'BUY'
  AND CAST(t.date AS DATE) = CURRENT_DATE
ORDER BY fs.sqn DESC;

-- Current HedgeIndex score (most recent date)
-- Note: HEDGE_INDEX stores one row per indicator per date (columns: id, category, date, indicator, price, risk, rule_desc)
-- Score = count of RISK=TRUE rows for the latest date
SELECT date, SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) AS score, COUNT(*) AS total_indicators
FROM hedge_index
WHERE date = (SELECT MAX(date) FROM hedge_index)
GROUP BY date;

-- HedgeIndex history (last 30 days)
SELECT date, SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) AS score, COUNT(*) AS total_indicators
FROM hedge_index
WHERE date >= DATEADD('DAY', -30, CURRENT_DATE)
GROUP BY date
ORDER BY date DESC;
```

---


