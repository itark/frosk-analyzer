# frosk-analyzer

A Spring Boot application implementing a hedge fund-style, layered trading framework on [ta4j](https://github.com/ta4j/ta4j) 0.16. Sweden (OMXS30) is the primary equity market; a separate crypto process trades Coinbase spot on 15-minute bars.

## What it is

Three layers drive the equity side:

1. **Macro signal (HedgeIndex)** — a daily risk-on/risk-off score summed from up to 18 macro indicators (volatility, FX, rates, commodities, breadth). It gates all equity entries and sizes hedges. Sweden-first: US indicators are kept only where they drive global risk appetite that flows into OMXS30.
2. **Equity selection** — rule-based screening (momentum, relative strength, quality) aligned to the current regime.
3. **Hedging** — options-based protection sized to regime severity.

Two Swedish portfolio strategies (Månadsportföljen, Dagstrategin) run on top, both gated by HedgeIndex.

Alongside the equities framework, a **crypto process** runs a 15-minute intraday pipeline over ~37 Coinbase products: a VWAP mean-reversion core plus a set of research/brainstorm strategies (EMA+ADX trend, Bollinger squeeze, BTC/ETH stat-arb, volatility-regime, time-of-day seasonality, night reversion, cross-coin spillover, and a voting ensemble). The research strategies run in **paper mode** to accumulate forward data and are **not yet validated** — see the research doc before trusting any of their P&L.

## Architecture

Equity and crypto run as **two separate Spring Boot processes from the same JAR**, each with its own H2 database file and Spring profile:

| Process | Profile | Port | Data | Config |
|---|---|---|---|---|
| Equity | `equity` | 8080 | OMX / Yahoo Finance | `application-equity.properties` |
| Crypto | `crypto` | 8081 | Coinbase | `application-crypto.properties` |

The frosk-dashboard toggle switches all API calls between `:8080` and `:8081` with no restart. (A third, currently parked, `kraken-futures` profile exists for futures.)

## Build, run, test

```bash
./build-frosk.sh                 # build gdax-java + frosk-analyzer, then start
mvn clean install -DskipTests    # build only

# Run (separate terminals)
mvn spring-boot:run -Dspring-boot.run.profiles=equity    # :8080
mvn spring-boot:run -Dspring-boot.run.profiles=crypto    # :8081

# Test
mvn test
mvn test -Dtest=TestJStrategyAnalysis                    # one class
mvn test -Dtest=TestJStrategyAnalysis#runSTLT            # one method
```

Running without a profile uses `application.properties`, which matches the equity profile. Integration tests extend `BaseIntegrationTest` (`@ActiveProfiles("test")`); crypto ITs additionally need `-Dspring.profiles.include=crypto` because the Coinbase data service is `@Profile("crypto")`.

## Documentation

[`CLAUDE.md`](CLAUDE.md) is the operating manual and the full index (architecture, conventions, how to add a strategy). Detailed specs live under [`docs/`](docs/):

| Topic | File |
|---|---|
| System architecture, data model, operations | `docs/architecture.md`, `docs/data-model.md`, `docs/operations.md` |
| HedgeIndex scoring model (18 indicators, regimes) | `docs/strategies/hedgeindex.md` |
| Equity selection / screening | `docs/strategies/equity-selection.md` |
| Strategy inventory (one-line summaries) | `docs/strategies/README.md` |
| Intraday (Tier-0) pipeline | `docs/intraday-pipeline.md` |
| Crypto 15m intraday research + brainstorm | `docs/research/crypto-15m-intraday-strategies.md` |

Project history: [`CHANGELOG.md`](CHANGELOG.md).
