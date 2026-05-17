# Configuration Reference (`application.properties`)

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
| `frosk.strategy.force.rerun` | true = bypass same-day idempotency check in `StrategyExecutor`, always re-run (default: false; test: true) |
| `frosk.portfolio.force.rebuild` | true = bypass same-day idempotency check in `PortfolioService.build()`, always rebuild (default: false; test: true) |
| `frosk.run.intraday` | true = run the Tier-0 intraday pipeline on every 10-min scheduler tick (default: true; test: false) |
| `intraday.retention.days` | Days of 5-minute bars to retain in `intraday_bar` table (default: 7) — older bars are pruned on every Tier-0 sync run |
