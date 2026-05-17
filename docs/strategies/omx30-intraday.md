# OMX30IntradayMomentumStrategy

**Type:** Intraday (1-hour max hold)

Runs on **5-minute bars** of all OMX30 dataset securities. The 10-minute cron evaluates the strategy on each security; intraday signals are generated at 5-minute granularity.

| Parameter | Value | Rationale |
|---|---|---|
| EMA fast | 5 bars (25 min) | Short-term trend on 5m bars |
| EMA slow | 13 bars (65 min / ~1 h) | Medium-term intraday trend |
| RSI period | 5 | Fast RSI for intraday momentum |
| Entry RSI level | 50 (crosses above) | Momentum turning up |
| Exit RSI level | 75 (over) | Overbought — take profit |
| Max bars held | 12 (1 hour) | Intraday time-based exit |

**Entry (all must be true):** EMA(5) > EMA(13) AND RSI(5) crosses above 50

**Exit (first satisfied):** RSI(5) > 75 OR EMA(5) crosses below EMA(13) OR 12 bars elapsed

> **No volume filter** — `^OMX` is an index; volume data is often zero or unavailable at the intraday level from Yahoo Finance.

Uses `TradeOnCurrentCloseModel` with 0.03% fee (`intradayFeePerTradePercent`).
