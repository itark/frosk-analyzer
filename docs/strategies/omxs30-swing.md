# OMXS30SwingStrategy — "Nordic Momentum Filter"

**Type:** Swing (2–10 days)

Fredrik's own strategy. 137 lines.

**Entry:** EMA(20) > EMA(50) AND RSI(14) crosses above 40 AND OBV > OBV SMA(5).

**Exit:** RSI(14) > 65 OR ATR stop-loss (2× ATR) OR stop-gain (3× ATR × 2) OR max 10 bars held.

Uses two custom inner rules: `AtrStopLossRule` and `MaxBarsHeldRule`.
