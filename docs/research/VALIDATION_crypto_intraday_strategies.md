# Validation & pre-registration — the 8 crypto 15m intraday strategies

Written 2026-11-03, **BEFORE** any full-universe / forward-data result has been read.
Locked thresholds. Any change after a first outcome read is a deviation, logged in §8, and makes
that strategy's test exploratory. Same discipline that rejected the VWAP breadth gate
([`PREREG_vwap_breadth_gate.md`](PREREG_vwap_breadth_gate.md)) — a promising 30-day signal that
did not survive out-of-sample. These eight are in exactly that position now.

## 0. Scope

The strategies from [`crypto-15m-intraday-strategies.md`](crypto-15m-intraday-strategies.md) that run
ON in PAPER mode and are **not yet validated**. No edge is proven for any of them.

| Strategy | Flag | Source |
|---|---|---|
| CryptoEmaAdxTrendIntradayStrategy | `crypto.emaadx.enabled` | §2/§4 S1 |
| CryptoTimeOfDayIntradayStrategy | `crypto.timeofday.enabled` | §2/§4 S2 |
| CryptoBollingerSqueezeIntradayStrategy | `crypto.bbsqueeze.enabled` | §2/§4 S3 |
| CryptoBtcEthStatArbIntradayStrategy | `crypto.statarb.enabled` | §2/§4 S4 |
| CryptoVolRegimeMomentumIntradayStrategy | `crypto.volregime.enabled` | §2/§4 S5 |
| CryptoNightReversionIntradayStrategy | `crypto.night.enabled` | §5 #2 |
| CryptoMomentumSpilloverIntradayStrategy | `crypto.spillover.enabled` | §5 #4 |
| CryptoEnsembleIntradayStrategy | `crypto.ensemble.enabled` | §5 #6 |

Baselines for reference (already live-tracked, not under test): `CryptoVWAPReversionIntradayStrategy`,
`CryptoLiquiditySweepIntradayStrategy`.

Out of scope: the Kraken brainstorm items (#3 funding, #5 basis, #7 sell-vol) — Kraken is parked
indefinitely. The VWAP breadth gate — rejected, do not implement.

## 1. Principles (locked)

- **30-day in-sample is not evidence.** Every number seen so far (the comparison-report runs during
  implementation) was ≤ 30 days on ≤ 10 coins and is treated as noise.
- **Net of fees.** Decisions use net PnL/trade after the Coinbase taker round trip (0.6%×2 = 1.2 pp;
  the report's `netPnL/tr` column). Gross is reported but never decides.
- **Out-of-sample required.** A strategy is judged on a test period it was never tuned on
  (walk-forward, §3A), not on the full pooled window.
- **No tuning to rescue.** Thresholds here are not adjusted after an outcome. A failing strategy is
  retired (flag → false), recorded as rejected, not re-parameterised.
- **SQN is secondary.** `avgSQN` is unreliable at low trade counts (noted in the harness); it informs,
  never gates.

## 2. Data-maturity gate (when this may be run)

Do not read decision results before BOTH hold:

1. **Calendar time:** ≥ 90 days of forward paper running. The clock starts at the restart that first
   deployed all 8 strategies into the live crypto process — **2026-11-03**, not the implementation date
   2026-10-26 — so earliest read **~2027-02-01**.
2. **Sample:** ≥ 100 closed paper round trips for the strategy under test, OR an explicit waiver
   logged in §8 for a strategy that is structurally gated and cannot reach 100 (precedent: the
   regime-gated Kraken short strategies — see [[kraken-futures-profile]]). StatArb (BTC-only) and the
   time-/regime-gated strategies are the likely waiver candidates; decide per strategy, in writing,
   before looking at PnL.

Running the reports earlier is allowed for **plumbing checks only** (does it run, does it trade) —
never to read PnL into a decision.

## 3. Tracks

### A. Backtest / walk-forward — full universe (`StrategyComparisonReportIT`)

Longer history than the live retention window: temporarily raise `intraday.retention.days` (e.g. to
365) so the sync pulls the maximum Coinbase 15m history available, then split by date:

- **Train (tune-blind):** earliest available → 2026-08-01. Not used for decisions — exists only so a
  later period is genuinely out-of-sample.
- **Test (decides):** 2026-08-01 → run date.
- Report each strategy's net/trade, win rate, trade count, max single-ticker share, and net/trade in
  **both halves of the test period**.

### B. Forward paper ledger (live crypto process DB)

After the §2 gate, analyse the real accumulated paper record in the crypto process H2 DB (NOT the
test DB): `intraday_signal` (entries/exits, `market_regime`, `spread_percent`), the
`CryptoPaperTradingService` realized round-trip PnL, and the rolling `FeaturedStrategy` rows. This is
the only track that reflects real decision latency, spreads and fills over calendar time. Useful H2
queries live in `docs/queries.md`.

### C. Ensemble feasibility (`CryptoSignalCorrelationIT`)

Re-run the pairwise correlation gate (< 0.4) on the **full universe** with mature data, and add an
entry co-occurrence metric (Jaccard / odds-ratio on same ticker+bar entries) alongside the
held-state Pearson — the held-state metric is sparsity-biased toward 0 (seen in the first run).
Only then is the ensemble's go/no-go trustworthy.

## 4. Decision rule per strategy (locked)

A strategy **passes to extended paper** only if ALL hold on the §3A **test** period (primary) and are
not contradicted by §3B (forward paper):

1. Net/trade > 0 after the 1.2 pp taker round trip.
2. Net/trade > 0 in **both** halves of the test period (same sign).
3. ≥ 100 test-period trades, or a §2 waiver.
4. No single ticker > 25% of the strategy's trades.
5. Forward-paper (§3B) realized net PnL ≥ 0 over the gate window.

Pass → keep the flag ON, continue paper/SHADOW to ≥ 300 forward trades, then a separate per-strategy
**live** pre-registration (mirroring the breadth-gate discipline) before any real order.
Fail → set the flag to `false`, record the strategy as rejected here (§8), do not re-tune.
The ensemble (C) additionally requires the correlation gate to pass before it is kept.

## 5. What is NOT done

No parameter tuning of any strategy. No ticker selection after the outcome. No new breadth/regime
gates (the breadth gate is rejected; do not revive it under a new name). No Kraken work. No change to
the decision thresholds above once a result is read.

## 6. Run plan — exact commands (full universe)

Needs network (Coinbase sync) and the crypto profile (the Coinbase data service is `@Profile("crypto")`).
Omitting `-Dcrypto.intraday.products` uses the full `crypto.intraday.products` universe; the report IT
now seeds that whole list.

```bash
# A. Backtest / walk-forward — all 8 under test + the two baselines, full universe
mvn test -Dmaven.test.skip=false -Dtest=StrategyComparisonReportIT#cryptoIntradayReport \
  -Dspring.profiles.include=crypto \
  -Dintraday.retention.days=365 \
  -Dfrosk.compare.strategies=CryptoEmaAdxTrendIntradayStrategy,CryptoTimeOfDayIntradayStrategy,CryptoBollingerSqueezeIntradayStrategy,CryptoBtcEthStatArbIntradayStrategy,CryptoVolRegimeMomentumIntradayStrategy,CryptoNightReversionIntradayStrategy,CryptoMomentumSpilloverIntradayStrategy,CryptoEnsembleIntradayStrategy,CryptoVWAPReversionIntradayStrategy,CryptoLiquiditySweepIntradayStrategy

# C. Ensemble correlation — full universe
mvn test -Dmaven.test.skip=false -Dtest=CryptoSignalCorrelationIT#signalCorrelation \
  -Dspring.profiles.include=crypto -Dintraday.retention.days=365
```

For strategies with a macro gate (EmaAdx, TimeOfDay, VolRegime), the test DB has no BTC daily closes,
so `CryptoRegimeRule` fails closed and suppresses entries. Either seed BTC daily closes first, or run
those with `-Dcrypto.<name>.macro.filter.enabled=false` and note it — the macro gate itself is then
validated separately against the live crypto DB (§3B), not in this backtest.

Walk-forward split is read off the printed per-half columns; if finer control is needed, filter the
synced series by date in a throwaway copy of the IT (do not change production code).

## 7. Schedule / cadence

- **~2027-02-01** (gate): first full decision run — tracks A, B, C — then apply §4 per strategy.
- Until then: leave everything ON in paper, accumulating forward data. Do not read PnL into decisions.
- After the first run: re-run A + C **quarterly** on each surviving strategy; promote to a live
  pre-registration only after the ≥ 300-forward-trade paper/SHADOW period.

## 8. Deviation / outcome log

| Date | Strategy | Section | Change / outcome | Reason |
|---|---|---|---|---|
| — | — | — | (none yet) | — |
