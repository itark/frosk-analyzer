# Configuration Reference (`application.properties`)

| Property | Purpose |
|---|---|
| `frosk.strategy.only` | Run only this strategy name (empty = all) |
| `frosk.runallstrategies` | true = run all strategies × all securities |
| `frosk.updatehedgeindex` | true = refresh the hedge index from DB before running |
| `frosk.buildportfolio` | true = call `portfolioService.build()` after `runInstall()` |
| `frosk.strategies.exclude` | Comma-separated strategy names to skip — **all hedge/FX/index strategies, the two Dagstrategin strategies, OMXS30Swing, SwedishLongTermMomentum, and the three intraday strategies are excluded from the default all-strategies batch run** (they run via their own dedicated code paths) |
| `frosk.run.dagstrategin` | true = run Dagstrategin strategies on OMX30 after startup |
| `frosk.run.omxs30swing` | true = run OMXS30SwingStrategy via its dedicated path |
| `frosk.run.manedsportfolj` | true = run Månadsportföljen (SwedishLongTermMomentumStrategy) via its dedicated path |
| `frosk.run.intraday` | true = run the Tier-0 intraday pipeline on every 10-min scheduler tick (default: true; test: false) |
| `frosk.hedge.criteria.risk.threshold` | HedgeIndex boolean risk threshold: `risk()` returns score > this value. **7** — recalibrated from 2 when scores became carried-forward levels (8+ = defensive) |
| `frosk.strategy.catastrophic.stop.pct` | Catastrophic stop % used by `AbstractStrategy.catastrophicStopRule()` (15.0) |
| **ShortTermMomentumLongTermStrength** | |
| `frosk.slms.stoploss.percent` | Hard stop-loss % below entry (10.0, was 15.0) |
| `frosk.slms.adx.threshold` | ADX(14) entry filter — only enter when trending (25.0) |
| `frosk.slms.atr.mult` | ATR trailing-stop multiplier (2.5) |
| `frosk.slms.hedge.max.score` | Max HedgeIndex score allowing entry (7) |
| `frosk.slms.hedge.exit.score` | Exit when HedgeIndex score exceeds this (9 — deliberately above the entry gate) |
| **HighLander** | |
| `frosk.highlander.stoploss.percent` | Hard stop-loss % below entry (12.0, was 20.0) |
| `frosk.highlander.atr.mult` | ATR trailing-stop multiplier (3.0) |
| **CANSLIM** | |
| `frosk.canslim.stoploss.percent` | Hard stop-loss % below entry (15.0) |
| `frosk.canslim.volume.multiplier` | Volume surge entry filter vs average volume (1.5) |
| `frosk.canslim.fiftytwo.week.threshold` | Near-52-week-high entry filter (0.95) |
| **Månadsportföljen (SwedishLongTermMomentum)** | |
| `frosk.swedish.longterm.topN` | Max SLMS positions in portfolio (annotation fallback: 20; `application.properties` sets 25) |
| `frosk.swedish.longterm.maxVolatility` | Max annualized volatility of **daily returns** for entry (0.60) |
| `frosk.swedish.longterm.pegratio.threshold` | PEG entry gate, own key — no longer shares HighLander's (2.5) |
| `frosk.swedish.longterm.hedge.exit.score` | Exit when HedgeIndex score exceeds this (9) |
| `frosk.swedish.longterm.atr.mult` | ATR trailing-stop multiplier (3.0) |
| **Portfolio gates** | |
| `frosk.portfolio.min.sqn` | Min SQN for any position to appear in the daily portfolio snapshot (1.0) |
| `frosk.portfolio.min.win.rate` | Min profitable-trades ratio for any position, in **percent scale** (40.0 = 40%) — matching how `FeaturedStrategy.profitableTradesRatio` is stored. The old 0.35 fraction value was a no-op against percent-stored values |
| `frosk.portfolio.other.topN` | Max combined non-SLMS positions, ranked by SQN (12, was 10) |
| `frosk.portfolio.force.rebuild` | true = bypass same-day idempotency check in `PortfolioService.build()`, always rebuild (default: false; test: true) |
| **Strategy run control** | |
| `frosk.strategy.force.rerun` | true = bypass same-day idempotency check in `StrategyExecutor`, always re-run (default: false; test: true) |
| **Intraday (Tier 0)** | |
| `frosk.intraday.hedge.max.score` | HedgeIndex entry gate for all three intraday strategies (9 — only strong risk-off blocks; a gate at 7 stopped all trading in defensive weeks) |
| `intraday.datasets` | Datasets whose securities are synced for 15m intraday bars (OMX30) |
| `intraday.retention.days` | Days of 15-minute bars to retain in `intraday_bar` (30, was 7) — older bars are pruned on every Tier-0 sync run |
| `exchange.transaction.intradayFeePerTradePercent` | Per-trade fee for intraday strategies and round-trip PnL netting (0.0003 = 0.03%) |
