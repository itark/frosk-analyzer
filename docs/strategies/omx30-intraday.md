# OMX30 Intraday Strategies (Tier 0)

Three intraday strategies run on **15-minute bars** of all OMX30 dataset securities via the Tier-0 pipeline (see `docs/intraday-pipeline.md`). All three:

- implement `IntradayStrategy` (auto-discovered by `IntradayStrategyRunner`)
- use `TradeOnCurrentCloseModel` with 0.03% fee per trade (`exchange.transaction.intradayFeePerTradePercent`)
- gate entries on HedgeIndex score ≤ `frosk.intraday.hedge.max.score` (default **9**) via `HedgeIndexMaxScoreRule` — only strong risk-off blocks; a gate at 7 stopped all intraday trading in defensive weeks, and intraday setups are less regime-sensitive than multi-day holds
- use the day-floored HedgeIndex score lookup, so the gate works on 15m bars

The former `OMX30IntradayMomentumStrategy` (5-minute EMA/RSI momentum) has been removed.

---

## OpeningRangeBreakoutIntradayStrategy — "Frukosthandeln"

Measures the high/low of the first 30 minutes (09:00–09:30 = 2 bars on 15m charts), then enters on a breakout above the OR high.

**Entry (all must be true):**

- Close **crosses above** the OR high (`CrossedUpIndicatorRule`) — a fresh breakout event, so a stopped-out position is not re-entered on the next bar above the level (no re-entry chains)
- At least 2 bars into the day (OR period complete)
- OR width between 0.3% and 1.5% (volatility filter)
- Overnight gap within ±1.5% — a large gap invalidates the OR as a consolidation range
- Close > EMA(20) — trend context
- **Morning only**: no entries after bar 12 (~12:00) — late breakouts mostly hit the time stop near the close
- HedgeIndex score ≤ 9

**Exit (first satisfied wins):**

- Profit target: 1.5× the OR width above the OR high
- Stop: close below the OR low
- Max 16 bars held (~4 hours)
- Catastrophic stop: 0.8% hard stop

## VWAPMeanReversionIntradayStrategy — "Gummibandshandeln"

Mean reversion to a **true session-anchored VWAP** (`SessionVWAPIndicator`): Σ(typical price × volume) / Σ(volume) over the bars of the current Stockholm trading day, resetting at every session start; falls back to the close price when cumulative volume is zero. The previous SMA(20) proxy drifted across the day, breaking both the entry distance and the reversion target. Test: `TestJSessionVWAPIndicator`.

**Entry (all must be true):**

- Price stretched ≥ 0.15% below session VWAP (covers the 0.06% round-trip fee more than twice before the reversion target)
- RSI(5) < 30 — short-term oversold
- EMA(20) > EMA(40) — only buy pullbacks in intraday uptrends
- Bar count 4–26 (~10:00–15:30, avoids open/close volatility)
- HedgeIndex score ≤ 9 — in strong risk-off, stretched prices keep stretching

**Exit (first satisfied wins):**

- Profit target: price back above session VWAP
- RSI(5) > 65 — momentum exhaustion
- Stop: 0.6% below entry (≈2× the entry stretch)
- Max 12 bars held (~3 hours)

Requires volume data, so it runs on individual OMX30 stocks, not the `^OMX` index.

## GapReversalIntradayStrategy — "Morgonfällan"

Fades moderate overnight gap-downs that tend to fill the same day.

**Entry (all must be true):**

- Gap-down between **0.5% and 3.5%** vs the previous day's close (larger gaps often signal real news — don't fade those)
- After bar 1 (wait 15 min for the dust to settle)
- No new day-low on the current bar — support holds
- **Morning only**: no entries after bar 12 (~12:00)
- HedgeIndex score ≤ 9 — gap-downs during strong risk-off do not fill

**Exit (first satisfied wins):**

- Profit target: price reaches the previous day's close (gap fill complete)
- Stop: 1×ATR(14) below entry with a 0.6% floor (`AtrStopLossRule` AND-ed with `StopLossRule` — fires only when the loss exceeds both, i.e. effective stop distance = max(ATR, 0.6%)). A fixed 0.4% stop sat inside the noise of a stock that just gapped 1–3% and caused premature stop-outs.
- Max 20 bars held (~5 hours, close by end of day)

---

Net/trade after the 2026-06-11 redesign (7-day sample): ORB −0.14, VWAP-MR −0.10 (was −0.19), GapReversal +0.07 at 2.3× trade count. `intraday.retention.days` was raised 7→30 so future evaluation has a real sample.
