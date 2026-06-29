# Equity Selection Model

When the HedgeIndex is risk-on (score 0–7), the equity selection layer screens for the best long candidates. In risk-off (8+), favour defensive sectors or move to cash/hedge.

## Stock Screening Criteria (from Yahoo Finance via `YahooFinanceDirectClient`)

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
