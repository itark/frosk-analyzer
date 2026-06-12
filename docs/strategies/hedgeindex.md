# HedgeIndex — Full Scoring Model

`HedgeIndexService` computes a daily **risk-off score** by summing points from up to 18 macro indicators. Each triggered condition adds +1 to the score. Higher score = more risk-off = reduce equity exposure.

## Indicator Table

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

**Volatility cluster rule:** if both VIX and VSTOXX are risk-off simultaneously, +1 extra point is added to the score (implemented in `HedgeIndexService.scoreOf()`).

## Regime Classification

| Total Score | Regime | Equity Exposure | Posture |
|---|---|---|---|
| 0–3 | **Strong Risk-On** | 80–100% | Long high-beta growth/momentum; minimal hedge |
| 4–7 | **Cautious / Transition** | 40–70% | Selective longs; collars or protective puts on core names |
| 8–11 | **Neutral / Defensive** | 10–40% | Defensive sectors, high-quality dividends; long Gold, long VIX |
| 12+ | **Strong Risk-Off** | 0–10% or net short | Long VIX/volatility, long Gold, short indices via bear puts or inverse ETFs |

## Score Semantics and Cache

`HedgeIndex` rows are state-change **events** (one per hedge strategy trade: BUY = indicator back to risk-on, SELL = indicator flipped to risk-off), not daily snapshots. The score for any given day is therefore a carried-forward **level**: the number of indicators whose most recent event on or before that day left them in the risk-off state.

`HedgeIndexService` builds an in-memory cache by replaying all events chronologically and carrying each indicator's state forward, keyed by Stockholm start-of-day in a `TreeMap<Long, Integer>`. Lookups use `floorEntry`, so any timestamp — a daily bar end time, a 15-minute intraday bar, or `ZonedDateTime.now()` — resolves to the score in effect on that calendar day. (The previous cache counted events on exact-millisecond dates, so `getScore(now())` and any intraday timestamp returned 0, making the tiered sizing and regime gates dead code.)

Call `warmCache()` before running bulk strategies; the cache is automatically cleared after `update()`. Accessors:

- `risk(ZonedDateTime)` — boolean, score > `frosk.hedge.criteria.risk.threshold` (**7**, recalibrated from 2 for level semantics: 8+ = defensive)
- `getScore(ZonedDateTime)` / `getScoreForDay(ZonedDateTime)` — raw integer level for tiered decisions

Rule classes in `strategies/rules/`: `HedgeIndexMaxScoreRule` (entry gate, satisfied while score ≤ max; `.negation()` turns it into an exit; works on daily and 15m bars alike), `HedgeIndexTieredRule`, `HedgeIndexRiskOffRule` (wraps `risk()`, fires at score ≥ 8).

Test: `TestJHedgeIndexDayScore`.

**Remaining indicators backlog:** Only 3 indicators still missing an implementation: A/D line (NYSE Advance/Decline — no Yahoo Finance ticker), HY OAS (US High-Yield OAS — not available via Yahoo Finance15), TED Spread (SOFR–T-bill spread — not available via Yahoo Finance15). Swedish KPIF is permanently skipped (no Yahoo Finance ticker). All other indicators — VSTOXX, OMX vs STOXX50, EUR/USD, USD/JPY, AUD/USD, DXY, 10Y Treasury, 2Y–10Y spread — are fully implemented and their tickers are registered in `YAHOO-INDEX-World indexes.csv`.
