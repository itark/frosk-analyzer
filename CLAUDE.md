# CLAUDE.md

Guidance for Claude Code when working in this repository. Detailed specs, architecture, and per-strategy designs live under `docs/`; this file is the operating manual and the index.

## Project Overview

A Spring Boot application implementing a hedge fund-style, layered trading framework using ta4j 0.16:

1. **Macro signal (HedgeIndex)** — daily risk-on/risk-off score; gates all equity entries
2. **Equity selection** — rule-based stock screening aligned with the current regime
3. **Hedging** — options-based protection sized to regime severity

Two Swedish stock portfolio strategies (Månadsportföljen, Dagstrategin), both gated by HedgeIndex. See `docs/strategies/README.md` for the full inventory.

## Build, Run, Test

```bash
./build-frosk.sh                                  # build gdax-java + frosk-analyzer, then start
mvn clean install -DskipTests
```

### Starting the application

Equity and crypto run as **two separate Spring Boot processes** from the same JAR, each with its own H2 database file and Spring profile. Start them in separate terminals:

```bash
# Terminal 1 — Equity (OMX/Yahoo data, port 8080)
mvn spring-boot:run -Dspring-boot.run.profiles=equity

# Terminal 2 — Crypto (Coinbase data, port 8081)
mvn spring-boot:run -Dspring-boot.run.profiles=crypto
```

Running without a profile (`mvn spring-boot:run`) uses the defaults in `application.properties`, which match the equity profile.

The frosk-dashboard toggle switch in the header seamlessly switches all API calls between `:8080` (equity) and `:8081` (crypto) — no restart needed.

Profile-specific configuration lives in:
- `application-equity.properties` — Yahoo sync, HedgeIndex, all schedulers (Tier 0–3)
- `application-crypto.properties` — Coinbase sync, Yahoo schedulers disabled, crypto cron at 00:30

### Tests

```bash
mvn test
mvn test -Dtest=TestJStrategyAnalysis             # one test class
mvn test -Dtest=TestJStrategyAnalysis#runSTLT     # one test method
```

Integration tests extend `BaseIntegrationTest`, which bootstraps the full Spring context with `@ActiveProfiles("test")` and mocks `FroskStartupApplicationListener` to prevent startup side effects. Test DB config: `src/test/resources/application-test.properties`.

## Adding a New Strategy

Every strategy must be registered in **five places** in `StrategiesMap`:

1. `@Autowired` field declaration
2. `buildStrategiesMap()` — adds class simple name to the string list
3. `getStrategies(BarSeries)` — calls `buildStrategy(series)` and adds to list
4. `getStrategyToRun(String, BarSeries)` — `else if` branch returning `buildStrategy(series)`
5. `getIndicatorValues(String, BarSeries)` — `else if` branch returning `getIndicatorValues()`

The strategy class must:

- Be annotated `@Component`
- Extend `AbstractStrategy`
- Implement `IIndicatorValue` (provides `getIndicatorValues()`)
- Call `super.setInherentExitRule()` and `indicatorValues.clear()` at the start of `buildStrategy()`

**Trade-on model convention:** `EngulfingStrategy` and `GoldStrategy` use `TradeOnCurrentCloseModel`; all others use `TradeOnNextOpenModel`. Intraday strategies use `TradeOnCurrentCloseModel` with 0.03% fee (`intradayFeePerTradePercent`).

**Adding an intraday strategy:** In addition to the five `StrategiesMap` registrations, implement `IntradayStrategy` on the class. The `IntradayStrategyRunner` auto-discovers all `IntradayStrategy` beans via Spring DI. Add the class name to `frosk.strategies.exclude` (intraday strategies run via Tier-0, not the batch run). See `docs/intraday-pipeline.md`.

**Adding a crypto intraday strategy:** Implement `CryptoIntradayStrategy` (NOT `IntradayStrategy` — the two runners are deliberately separate so crypto strategies never run on OMX series and vice versa). `CryptoIntradayStrategyRunner` auto-discovers all `CryptoIntradayStrategy` beans. Conventions specific to crypto intraday:
- Register in only **three** of the five `StrategiesMap` places — `@Autowired` field, `getStrategyToRun()`, `getIndicatorValues()` — and NOT in `buildStrategiesMap()`/`getStrategies()`, so the strategy stays out of the equity batch map entirely (same pattern as `CryptoLiquiditySweepIntradayStrategy` and all the research/brainstorm strategies). `StrategyExecutor` resolves it via `getStrategyToRun()`, so that branch is mandatory.
- Add a per-strategy `crypto.<name>.enabled` flag and wire it into `CryptoIntradayStrategyRunner.isExcluded()` and `isEnabledByConfig()`. New strategies default to PAPER mode (`CoinbaseStrategyModeService`), so they emit paper signals but place no real orders unless live trading is enabled.
- Fees come from `TransactionFeeService` (Coinbase taker tier), never the equity intraday 0.03%.
- A cross-series strategy (needs another product's bars, e.g. BTC/ETH stat-arb or the momentum-spillover master signal) loads them itself via `IntradayDataService.buildAllSeriesFromDb()` and floor-aligns by bar end time; it guards to its primary product and returns a never-trading strategy for others. See `CryptoBtcEthStatArbIntradayStrategy`.

## Conventions Claude Can't See From the Code

- **Two-process architecture**: equity (port 8080) and crypto (port 8081) run as separate Spring Boot processes from the same JAR, using `application-equity.properties` and `application-crypto.properties` respectively. Each process has its own H2 database file. The frosk-dashboard toggle switch changes which backend base URL is used (`config.baseApi` vs `config.cryptoBaseApi` in `config.js`).
- **Swedish tickers** use the `.ST` suffix (e.g. `VOLV-B.ST`, `ERIC-B.ST`). The OMXS30 index is `^OMX`.
- **`Security.active`** is auto-set to `enterpriseValue > 500_000_000` via `@PreUpdate`. `BarSeriesService.getDataSet(Database)` filters to `active=true`, so small caps are excluded by default.
- **HedgeIndex cache**: call `HedgeIndexService.warmCache()` before bulk strategy runs; the cache clears automatically after `update()`. Use `risk(ZonedDateTime)` for boolean checks and `getScore(ZonedDateTime)` for tiered decisions.
- **Same-day idempotency** is enforced in `StrategyExecutor.execute()` (skips if `lastRunDate == today`) and in `PortfolioService.build()`. Override with `frosk.strategy.force.rerun=true` and `frosk.portfolio.force.rebuild=true` (both default `false` in main, `true` in test).
- **Strategies excluded from batch all-strategies runs** (they run via dedicated code paths): all hedge/FX/index strategies, both Dagstrategin strategies, and `OMXS30SwingStrategy`. Configured via `frosk.strategies.exclude`.
- **New REST endpoints**: only add when there's a concrete consumer (frontend or external integration). Results are queryable directly via `/h2-console`.
- **Adding a HedgeIndex indicator** is a two-step operation: (1) add the ticker to `YAHOO-INDEX-World indexes.csv`, (2) add the scoring rule to `HedgeIndexService`. See `docs/strategies/hedgeindex.md`.
- **All external market data** flows through `YahooFinanceDirectClient` (direct calls to Yahoo's public v8/v10 endpoints — no API key, no cost, no quota). Any new Yahoo data source must be added there. See `docs/operations.md`.
- **Crypto intraday research strategies** (`CryptoTimeOfDay`, `CryptoEmaAdxTrend`, `CryptoBollingerSqueeze`, `CryptoBtcEthStatArb`, `CryptoVolRegimeMomentum`, `CryptoNightReversion`, `CryptoMomentumSpillover`, `CryptoEnsemble`) are from `docs/research/crypto-15m-intraday-strategies.md`. They run ON by default but in PAPER mode to accumulate forward data — **none is validated; do not read their paper P&L as an edge** before a walk-forward / pre-registered test. The Kraken-dependent brainstorm items (funding regime, basis arbitrage, selling volatility) are parked — the `kraken-futures` profile is on hold indefinitely.
- **Ensemble feasibility** is measured by `CryptoSignalCorrelationIT` (pairwise signal-correlation gate < 0.4) before building a voting meta-strategy; run it with `-Dspring.profiles.include=crypto`. Crypto ITs need that include because the Coinbase data service is `@Profile("crypto")` and absent under the plain `test` profile.

## Where Things Live

```
src/main/resources/codes/              CSV files registering securities by dataset
src/main/resources/static/             Frontend pages (CDN React, no build tooling)
src/main/resources/application.properties
src/test/resources/application-test.properties
```

## Documentation Map

When working on a specific area, read the relevant doc first.

| Topic | File |
|---|---|
| System architecture (call tree, runners, schedulers) | `docs/architecture.md` |
| Data model (`Security`, `SecurityPrice`, `IntradayBar/Signal`, `FeaturedStrategy`) | `docs/data-model.md` |
| Configuration reference (`application.properties` table) | `docs/configuration.md` |
| Operations (data layer, Tier 0/1/2/3 sync schedule) | `docs/operations.md` |
| Tier-0 intraday pipeline mechanics | `docs/intraday-pipeline.md` |
| Useful H2 queries | `docs/queries.md` |
| Strategy inventory (one-line summaries) | `docs/strategies/README.md` |
| HedgeIndex scoring model (18 indicators, regime classification) | `docs/strategies/hedgeindex.md` |
| Equity selection / screening criteria | `docs/strategies/equity-selection.md` |
| HighLander | `docs/strategies/highlander.md` |
| ShortTermMomentumLongTermStrength | `docs/strategies/short-term-momentum.md` |
| OMXS30Swing ("Nordic Momentum Filter") | `docs/strategies/omxs30-swing.md` |
| Månadsportföljen (long-term factor) | `docs/strategies/manadsportfoljen.md` |
| Dagstrategin (daily breakout + oversold bounce) | `docs/strategies/dagstrategin.md` |
| OMX30 Intraday Momentum | `docs/strategies/omx30-intraday.md` |
| Exit rules audit (stop-loss, HedgeIndex, time exits) | `docs/strategies/exit-rules.md` |
| Crypto 15m intraday research + brainstorm (strategies 1–5, brainstorm #1–#7) | `docs/research/crypto-15m-intraday-strategies.md` |

Project history and completed implementation steps: `CHANGELOG.md`.

## Path-Scoped Rules

Rules that only apply to certain parts of the codebase live in `.claude/rules/` and load only when Claude reads matching files:

- `strategies.md` — applies under `src/main/java/.../strategies/`
- `persistence.md` — applies to JPA entities and Spring Data repositories
