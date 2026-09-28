# Crypto Intraday Strategy Research — Literature Review & Recommendations

**Date:** 2026-09-17
**Scope:** Academic/practical findings (2023–2026) on profitable crypto intraday trading at 15m–1h bars, cross-referenced against frosk-analyzer's current crypto strategy stack and turned into concrete, prioritized add-ons.

## How to read this document

Sections 1–5 answer the five research questions with citations. Each finding is immediately followed by **Relevance to frosk-analyzer** — what it means given what this codebase has already tried, killed, or half-built. The **Recommendations** section at the end is the actionable output; everything above it is the evidence trail. Where a finding contradicts or duplicates something already tested here (notably the retired order-flow-imbalance strategy), that is called out explicitly rather than silently re-proposing it.

## Current state snapshot (grounding)

Established by reading `src/main/java/nu/itark/frosk/strategies/Crypto*.java` and `application-crypto.properties` directly, not from memory:

| Strategy | Status | Documented gross edge | Notes |
|---|---|---|---|
| `CryptoVWAPReversionIntradayStrategy` | **Live, only proven edge** | +0.285–0.408%/trade gross | UTC-day VWAP mean reversion, RSI(7) confirmation, GARCH regime hook present but disabled for crypto |
| `CryptoLiquiditySweepIntradayStrategy` | **Live, pre-registered, accumulating data** | Unknown by design | PDH/PDL sweep-and-reclaim; data gate not due until ~Dec 2026; do not inspect early |
| `CryptoShortIntradayStrategy` | Enabled (signal-only) | +0.869%/trade gross (best in dataset), 52.5% win, n=59 | Cannot execute — Coinbase spot has no shorting venue |
| `CryptoEMACrossShortIntradayStrategy` | Enabled (signal-only) | No documented edge study | Same execution constraint as above |
| `CryptoEMACrossLongIntradayStrategy` | **Disabled** | −0.461%/trade gross, 19.0% win, n=1040 | Killed 2026-07-31, negative before fees |
| `CryptoRangeBreakoutIntradayStrategy` | **Disabled** | −0.434%/trade gross, 25.7% win, n=444 | Killed same review |
| `CryptoBTCTrendStrategy` | Live (own scheduler, excluded from batch) | — | Daily EMA10/20+ADX signal for manual leveraged-ETP execution, not backtested for edge |
| Order-flow imbalance (1m OFI) | **Retired 2026-09-09** | t=0.219 (need >2.5), net −0.20% after cost | Gate A failed on its own pre-registration; L2 capture stays on for data continuity |
| GARCH+ADX volatility regime (`RegimeForecastService`) | Built, wired into VWAPReversion, **disabled for crypto** | — | Explicitly "equity pilot only for now" |
| LSTM signal filter | Enabled for crypto | — | Gates `EMACrossLong` and `RangeBreakout` — **both currently disabled strategies** |

This is a mature, fee-aware, pre-registration-disciplined system. The research below is filtered for what's genuinely additive to it, not a generic "here's how to trade crypto" primer.

## 1. Which technical strategies show consistent alpha in crypto intraday (15m–1h)?

**Momentum and reversal coexist at hourly/half-hourly horizons, unlike equities.** Wen, Bouri, Xu & Zhao, "Intraday return predictability in the cryptocurrency markets: Momentum, reversal, or both" (ScienceDirect/SSRN, BTC 2013–2020, robustness-checked on ETH/LTC/XRP) find both positive and negative predictive half-hour returns within the same trading day — a pattern absent in equities, attributed to late-informed investors (momentum) and overreaction/overconfidence in an immature, retail-dominated market (reversal). Predictability weakens around FOMC announcements, large price jumps, and high-liquidity periods.

*Relevance:* This is direct support for frosk's existing approach of running a mean-reversion strategy (VWAPReversion) and momentum-style strategies (EMACross, RangeBreakout) side by side rather than picking one family — the literature says both exist, just not always at the same time. It also suggests the EMACross/RangeBreakout failures may be a regime-conditioning problem rather than "momentum doesn't work in crypto" (see §3 and Recommendation 5).

**A specific, statistically significant intraday seasonality window: 21:00–23:00 UTC.** QuantPedia's "Are There Seasonal Intraday or Overnight Anomalies in Bitcoin?" and the follow-up backtest at paperswithbacktest.com both identify 21:00–23:00 UTC (after major equity exchanges close) as economically and statistically the strongest window, with a naive buy-21:00/sell-23:00 rule producing ~33% annualized return at materially lower volatility than buy-and-hold. 03:00–04:00 UTC is the weakest window, though not statistically significant.

*Relevance:* frosk already time-gates one strategy (`CryptoShortIntradayStrategy` blocks 17:30–19:00 UTC for US-evening volatility). This is evidence for the mirror-image idea on the long side — a session-based filter or signal-strength bonus around 21:00–23:00 UTC — and it's cheap to test since the infrastructure (`TimeGatingRule`) already exists.

**Trend-following (new-high following) has recently outperformed mean-reversion (new-low buying) on Bitcoin**, especially since 2022. QuantPedia's "Revisiting Trend-following and Mean-reversion Strategies in Bitcoin" (in-sample 2015–2024, out-of-sample stress test Feb 2022–Aug 2024) finds the MAX (new N-day-high) strategy "alive and well" through the 2022 decline while the MIN (new N-day-low) strategy underperformed. This is on daily bars, not 15m–1h, but it's a regime signal.

**Fixed-time bars may be the wrong sampling unit.** A 2025 Financial Innovation (Springer) paper combining information-driven bars (CUSUM filter, volume/dollar bars) with triple-barrier labeling and deep learning found that CUSUM-filtered/volume-bar sampling plus triple-barrier labeling outperformed fixed time bars with next-bar prediction, and survived transaction costs — while transformer architectures were *not* clearly better than simpler models (XGBoost, LSTM variants) on this problem.

*Relevance:* frosk's entire pipeline is fixed 15m Coinbase candles. This doesn't mean rebuilding the pipeline, but it's a reason to deprioritize "bigger model" investment in the LSTM microservice and instead consider whether the *labeling* used to train it follows triple-barrier logic (see §4 and Recommendation 6) — cheaper and, per this paper, more impactful than architecture changes.

## 2. Crypto-specific indicators vs. traditional equity

**Funding rate has weak single-asset predictive power but a real cross-sectional signal.** Presto Research's regression analysis of Binance perpetual funding rates found essentially zero next-period R² for single-asset price prediction ("price changes for a single asset cannot be reliably predicted using only funding rate changes"), but a statistical-arbitrage strategy using funding-rate *differentials* across ~50 liquid perps showed a favorable Sharpe, with the caveat of high turnover.

*Relevance:* frosk trades Coinbase **spot**, where funding rates don't exist at all — this only becomes applicable if the already-built but strategy-less Kraken Futures infrastructure (`KrakenFuturesTickerService`, `KrakenFuturesOrderClient`) gets a strategy. Worth flagging as a question rather than a recommendation (see Questions).

**Order-flow toxicity (VPIN) predicts price jumps, not just imbalance — and this is a different construction than what frosk already killed.** A 2025 ScienceDirect paper on Bitcoin order-flow toxicity uses VPIN (Volume-synchronized Probability of Informed Trading) over volume-synchronized 8-hour buckets and finds it significantly predicts future price *jumps* at roughly a two-bucket (~16h) horizon, asymmetrically stronger for positive jumps.

*Relevance — important caveat:* frosk already tested and retired a raw order-flow-imbalance signal at 1-minute granularity (`PREREG_ofi_1m.md`, Gate A failed 2026-09-09: t=0.219, net −0.20% after cost, 93.5% of the gross edge eaten by cost). VPIN's construction is meaningfully different — volume-bucketed rather than time-bucketed, and the target is jump *probability* over a much longer horizon rather than next-bar return — so this isn't simply re-litigating the same failed idea. But given the discipline this project applies to avoid multiple-testing (the sweep pre-registration explicitly warns against checking early and calls a second look "no longer pre-registered"), this should be treated as a *new*, narrowly-scoped hypothesis with its own pre-registration, not folded into reviving the old one. The L2 order-book capture for BTC-EUR/ETH-EUR is already running, so the data exists to build this without new infrastructure.

**On-chain data (exchange netflow, whale transactions) is real but mostly a lower-frequency signal.** Search turned up substantial industry material (Nansen, CoinMarketCap flow data) but no peer-reviewed work establishing predictive power at 15m–1h horizons specifically — on-chain settlement and reporting lag makes it naturally suited to multi-day rather than intraday signals.

*Relevance:* Deprioritized for the current 15m pipeline. Worth a second look only if frosk moves toward swing/multi-day crypto strategies.

**Volatility-regime conditioning (GARCH) is well-supported and frosk has already built it — for equities only.** Multiple 2024–2026 papers on HMM/GARCH regime detection in crypto (MDPI, Springer *Computational Economics*, and others) report that gating direction or position size on a volatility/trend regime improves risk-adjusted returns versus static rules.

*Relevance:* This is the highest-leverage finding in the whole review, because frosk doesn't need to build anything — `RegimeForecastService` and `GarchRegimeRule` already exist, `CryptoVWAPReversionIntradayStrategy` already has the SIDEWAYS-regime hook wired into its entry rule, and it's disabled for crypto by a single flag (`regime.garch.enabled=false`, explicitly commented "equity pilot only for now"). See Recommendation 1.

## 3. Mean-reversion vs. momentum — which wins at which horizon?

At sub-hourly to hourly horizons (15m–1h, frosk's own bar size), the Wen et al. paper says **both exist simultaneously** and which one dominates varies by micro-conditions (liquidity, proximity to news). At daily-and-longer horizons, the QuantPedia revisit says **momentum (new-high following) has recently dominated mean-reversion**, particularly through stress periods.

*Relevance:* The practical read for frosk is that VWAPReversion's continued edge and EMACrossLong/RangeBreakout's failure aren't necessarily evidence that "mean reversion works, momentum doesn't" in this market — it's consistent with momentum only working in a genuine trend regime and getting chopped up in the sideways/choppy conditions that dominated the July 2026 backtest window (their own regime service would have flagged BTC below SMA20 in 28 of 30 days during the crypto breakout baseline period, per the P12 changelog entry). A momentum strategy tested without regime conditioning, in a period that was mostly not trending, is expected to lose — that's not the same finding as "momentum has no edge in crypto." See Recommendation 5.

## 4. Optimal holding periods, stop-loss placement, position sizing

No peer-reviewed source gives a universal "optimal" holding period — it's asset- and strategy-specific, but the general finding across the intraday-predictability literature (predictability decaying within hours, VPIN's ~16h jump horizon, the 21:00–23:00 UTC two-hour seasonality window) is consistent with frosk's own short max-bars-held windows (8–24h across strategies) rather than arguing for longer holds.

**ATR-based stops and chandelier trailing stops are standard, well-supported practice** and are already exactly what frosk implements (`AtrStopLossRule`, `AtrTrailingStopRule`, `ProfitLockTrailingRule`). Nothing in the current literature suggests a materially better alternative for stop placement specifically; the marginal gains in recent work come from *when to enter/exit*, not *how far away to place the stop*.

**Triple-barrier labeling (profit target + stop + time limit, evaluated jointly) is the standard the 2025 Financial Innovation crypto-DL paper credits for its improvement over next-bar prediction.** This is structurally identical to what every frosk crypto strategy's exit rule already does (`profitTarget.or(stopLoss).or(timeExit)`) — the strategies themselves already use triple-barrier logic. Where it's *not* yet applied is training data for the LSTM signal-filter microservice, which is a separate system frosk doesn't control the internals of from this repo. Worth a question rather than a firm recommendation (see Questions).

**Position sizing: the literature's caution about naive Kelly sizing on noisy, small-sample win-rate estimates lines up with frosk's own documented decision.** The `crypto.signal.strength.*` multiplier comments explicitly note Kelly was rejected "given a calibrated win-probability isn't trustworthy yet on this project's trade counts," referencing an internal research doc. That's the same conclusion the broader quant literature reaches for small-sample strategies — no change recommended here; it's already the right call for the data volume this project has.

## 5. Machine learning / regime-detection improvements over pure technical strategies

**HMM/GARCH regime-switching shows consistent (if modest) risk-adjusted improvement over static technical rules** in multiple 2024–2026 papers — again, the strongest actionable point is that frosk already built this and hasn't turned it on for crypto (§2, Recommendation 1).

**Meta-labeling (a secondary classifier deciding whether/how much to size an existing rule-based signal, per Lopez de Prado's triple-barrier framework) is well-established in the literature** (Hudson & Thames' applied research, the broader "Advances in Financial Machine Learning" literature it's built on) as a way to add value on top of a primary technical signal without replacing it. frosk's `SignalStrength` (BASE/ELEVATED/STRONG) tiers are effectively a hand-built, rule-based version of meta-labeling already. The backtests that killed automatic scoring for VWAPReversion (561 trades) and RangeBreakout (506 trades) found the hand-built bonus conditions were *inverted* predictors, not just weak ones. That's a useful negative result in its own right, but it doesn't necessarily indict meta-labeling as an approach — a properly trained classifier (rather than hand-picked "wider range = better" heuristics) is a different hypothesis. Meta-labeling literature generally wants thousands of labeled trades to train reliably; frosk is in the low hundreds per strategy, so this is a "watch and revisit once trade count grows" item, not immediate.

**Deep learning/transformer architectures did not show a clear edge over simpler models in the most relevant recent paper found.** This argues against investing further in the LSTM microservice's architecture and toward cheaper wins: better labeling (triple-barrier) and — most importantly, given what was found reading the code — **pointing it at a strategy that actually has an edge**. Right now `forecast.lstm.enabled=true` for crypto, but the only two strategies gated by `LstmSignalRule` (`CryptoEMACrossLongIntradayStrategy`, `CryptoRangeBreakoutIntradayStrategy`) are both disabled for negative gross edge — the LSTM service is running and being called for nothing. See Recommendation 2.

## Recommendations, prioritized by effort vs. evidence strength

**1. Enable and evaluate GARCH+ADX regime gating for crypto (near-zero code, config + measurement only).** `regime.garch.enabled` is currently `false` under the crypto profile and the hook is already wired into `CryptoVWAPReversionIntradayStrategy`'s entry rule (requiring `Regime.SIDEWAYS`). This is the single highest evidence-to-effort recommendation in this review: the literature broadly supports volatility-regime conditioning, and frosk has already paid the implementation cost. The main open question is whether the Python GARCH microservice has been validated against 15m crypto bars (`regime.garch.window.bars=1000`, `min.observations=200` — check these are sane for 15m bars vs. whatever equity bar size they were tuned against) — that's a measurement task, not a build task.

**2. Re-point the LSTM signal filter at `CryptoVWAPReversionIntradayStrategy` instead of the two dead strategies it currently (uselessly) gates.** `forecast.lstm.enabled=true` for crypto today calls a live service that gates only disabled strategies. Wiring `LstmSignalRule` into VWAPReversion's entry rule — the one strategy with a real, measured edge — is the only place in the current architecture where this service's cost (an HTTP call plus a running microservice) can actually pay for itself. This should go through the same before/after edge measurement discipline already used elsewhere in this codebase (compare gross edge with the filter on vs. off on a holdout window) rather than being flipped on blind.

**3. Pre-register a time-of-day filter or signal-strength bonus around the 21:00–23:00 UTC window.** Two independent sources (QuantPedia, paperswithbacktest) identify this as the statistically strongest window in Bitcoin's day, right after major equity markets close. `TimeGatingRule` already exists and is already used for the mirror case (blocking, not favoring, a window) in `CryptoShortIntradayStrategy`. Cheapest way to test: either restrict VWAPReversion/EMACross entries to this window as a filter, or — more in keeping with the `SignalStrength` pattern already in place — add it as a bonus condition and re-run the same kind of backtest that discovered the VWAP/breakout scoring was inverted. Should be pre-registered before looking at results, matching the sweep strategy's own methodology.

**4. Pre-register a VPIN/jump-probability signal on the existing L2 capture data (BTC-EUR, ETH-EUR) — as a new, distinct hypothesis, not a revival of the retired OFI strategy.** The data collection (`coinbase.l2.capture.enabled=true`) is already running with 35-day retention, so this needs no new infrastructure. The construction (volume-synchronized buckets, jump-probability target, ~16h horizon) is different enough from the failed 1-minute time-bucketed imbalance signal that it's a legitimate new hypothesis — but given how carefully this project avoids multiple-testing on the same underlying data (the L2 capture note explicitly separates "stop trading it" from "keep the data record intact"), this should get its own `PREREG_vpin_jump_*.md` with pre-committed gates, most likely queued *after* the liquidity-sweep pre-registration's own data gate resolves (~Dec 2026) rather than run concurrently against the same limited BTC-EUR/ETH-EUR history.

**5. Re-test `CryptoEMACrossLongIntradayStrategy` and `CryptoRangeBreakoutIntradayStrategy` gated on `Regime.TRENDING` rather than treating their July 2026 failure as final.** Both were killed for negative gross edge measured over a period the changelog itself notes was ~93% non-trending (BTC below SMA20 in 28/30 days). The literature (§3) suggests momentum strategies are expected to fail in a mostly-sideways window regardless of underlying quality. Once Recommendation 1 is live, gating these two specifically on `Regime.TRENDING` (not `SIDEWAYS`, the opposite of VWAPReversion's gate) and re-measuring on a period that actually contains trend would be a fair test — re-enabling with the same parameters in the same regime mix would not be.

**6. Ask (don't yet act) about the LSTM microservice's label construction.** If it's not already using triple-barrier-style labels (profit target / stop / time-limit evaluated jointly, matching what every frosk strategy's exit rule already does), that's a plausible lever — but this repo doesn't contain that service's source, so it's a question for Fredrik, not a code change proposal.

**7. Deprioritized: funding-rate and on-chain signals.** Funding rate only has a documented cross-sectional edge (many perps at once, high turnover) and frosk's crypto pipeline is spot-only on Coinbase — the Kraken Futures infrastructure exists but has no strategies built on it. On-chain data lacks peer-reviewed support at 15m–1h horizons. Neither is worth building against today's architecture; both are worth revisiting only if the futures infrastructure gets used.

## Questions for Fredrik

- The Coinbase taker round-trip fee is stated inconsistently across the code I read: `CryptoVWAPReversionIntradayStrategy`'s javadoc and `application-crypto.properties` both say "0.20% round trip" (`trading.fee.roundtrip.pct=0.002`), but `CryptoRangeBreakoutIntradayStrategy`'s javadoc says "0.6%/trade" (1.2% round trip), and the P11 changelog entry sets `cryptoTakerFeePerTradePercent=0.006`. If the fee tier genuinely improved since RangeBreakout was written, its javadoc is stale; if not, the cost hurdle used in any new strategy's viability check needs the right number. Worth a quick confirmation before pre-registering anything above.
- Is there any near-term intent to build a strategy on the existing Kraken Futures client (`KrakenFuturesTickerService`/`OrderClient`)? That's the only scenario where funding-rate research (§2) becomes directly applicable rather than a "revisit later" item, and where `CryptoShortIntradayStrategy`'s unexecutable +0.869%/trade edge could actually be traded via a shortable venue.
- For Recommendation 4 (VPIN/jump signal): OK to draft the pre-registration now and queue it, or better to wait until the liquidity-sweep's own December data gate resolves so the two aren't running as concurrent tests against overlapping BTC-EUR/ETH-EUR history?
- Do you have visibility into the LSTM microservice's training/labeling code (it's outside this repo), or is that a separate project I don't have access to from here?

## Sources

- [Intraday return predictability in the cryptocurrency markets: Momentum, reversal, or both (ScienceDirect)](https://www.sciencedirect.com/science/article/abs/pii/S1062940822000833)
- [Same paper, SSRN working copy](https://papers.ssrn.com/sol3/Delivery.cfm/SSRN_ID4135239_code2537556.pdf?abstractid=4080253&mirid=1)
- [Same paper, ResearchGate](https://www.researchgate.net/publication/361202793_Intraday_return_predictability_in_the_cryptocurrency_markets_momentum_reversal_or_both)
- [Can Funding Rate Predict Price Change? — Presto Research](https://www.prestolabs.io/research/can-funding-rate-predict-price-change)
- [Predictability of Funding Rates — SSRN](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5576424)
- [Exploring risk and return profiles of funding rate arbitrage on CEX and DEX — ScienceDirect](https://www.sciencedirect.com/science/article/pii/S2096720925000818)
- [Bitcoin wild moves: Evidence from order flow toxicity and price jumps — ScienceDirect](https://www.sciencedirect.com/science/article/pii/S0275531925004192)
- [VPIN — The Volume Synchronized Probability of Informed Trading (original methodology paper)](https://www.quantresearch.org/VPIN.pdf)
- [Order Flow Toxicity in the Bitcoin Spot Market — Medium](https://medium.com/@lucasastorian/empirical-market-microstructure-f67eff3517e0)
- [Cryptocurrency Trading Research hub — QuantPedia](https://quantpedia.com/cryptocurrency-trading-research/)
- [Revisiting Trend-following and Mean-reversion Strategies in Bitcoin — QuantPedia](https://quantpedia.com/revisiting-trend-following-and-mean-reversion-strategies-in-bitcoin/)
- [Are There Seasonal Intraday or Overnight Anomalies in Bitcoin? — QuantPedia](https://quantpedia.com/are-there-seasonal-intraday-or-overnight-anomalies-in-bitcoin/)
- [Bitcoin Never Sleeps: Exploiting Seasonality, Momentum, and Mean Reversion in a 24/7 Market — Paperswithbacktest](https://paperswithbacktest.com/blog/bitcoin-never-sleeps-exploiting-seasonality)
- [Algorithmic crypto trading using information-driven bars, triple barrier labeling and deep learning — Financial Innovation (Springer)](https://link.springer.com/article/10.1186/s40854-025-00866-w)
- [Does Meta Labeling Add to Signal Efficacy? — Hudson & Thames](https://hudsonthames.org/does-meta-labeling-add-to-signal-efficacy-triple-barrier-method/)
- [Markov and Hidden Markov Models for Regime Detection in Cryptocurrency Markets: Evidence from Bitcoin (2024–2026)](https://www.academia.edu/165182244/Markov_and_Hidden_Markov_Models_for_Regime_Detection_in_Cryptocurrency_Markets_Evidence_from_Bitcoin_2024_2026_)
- [Regime-Aware Adaptive Forecasting Framework for Bitcoin Prices Using Probabilistic Generative Models — Computational Economics (Springer)](https://link.springer.com/article/10.1007/s10614-026-11338-3)
- [A hidden Markov model to detect regime changes in cryptoasset markets — ResearchGate](https://www.researchgate.net/publication/341915689_A_hidden_Markov_model_to_detect_regime_changes_in_cryptoasset_markets)
