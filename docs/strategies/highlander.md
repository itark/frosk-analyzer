# HighLanderStrategy

**Type:** Long-term

Core equity strategy — gated by HedgeIndex; entries only in risk-on regime. Exit: composite sub-strategy exits OR 20% hard stop-loss.

Added 20% hard stop-loss via `StopLossRule`; extracts composite entry/exit rules and appends `.or(stopLoss)` to exit; `frosk.highlander.stoploss.percent=20.0` in `application.properties`.
