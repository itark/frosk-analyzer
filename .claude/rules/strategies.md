---
globs: src/main/java/**/strategies/**
---

# Strategy Rules

- Every strategy must extend `AbstractStrategy`, implement `IIndicatorValue`, and be annotated `@Component`.
- Call `super.setInherentExitRule()` and `indicatorValues.clear()` at the start of `buildStrategy()`.
- Register in **all five places** in `StrategiesMap`: (1) `@Autowired` field, (2) `buildStrategiesMap()`, (3) `getStrategies()`, (4) `getStrategyToRun()`, (5) `getIndicatorValues()`.
- Trade execution model: `EngulfingStrategy` and `GoldStrategy` use `TradeOnCurrentCloseModel`; intraday strategies use `TradeOnCurrentCloseModel` with 0.03% fee; all others use `TradeOnNextOpenModel`.
- Strategies excluded from the batch all-strategies run must be added to `frosk.strategies.exclude` in both `application.properties` and `application-test.properties`.
- If the strategy should appear in the portfolio, add its class name to `PortfolioService.PORTFOLIO_STRATEGIES`.
- Use ta4j 0.16 API (`DoubleNum`, `Rule`, `BarSeries`). Do not import ta4j 0.17+ types.
- Custom rules (e.g. `StopLossRule`, `HedgeIndexTieredRule`, `MaxBarsHeldRule`) live under `strategies/rules/`.
