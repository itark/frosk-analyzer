# ShortTermMomentumLongTermStrengthStrategy

**Type:** Medium-term

Combines short-term momentum with long-term strength filter. Exit: price < SMA(50) OR SMA(10) < SMA(20) OR HedgeIndex risk-off OR 15% stop-loss.

`frosk.slms.stoploss.percent=15.0` in `application.properties`.
