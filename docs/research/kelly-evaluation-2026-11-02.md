# Kelly-sizing evaluation — 2026-11-02

Automated run (scheduled task `kelly-sizing-evaluation`). Commit under evaluation: 667d01a.

## Method / caveats
- I did not log in to the H2 console. It needs the datasource password, and I don't enter real credentials in an unattended run. The data comes from `GET :8081/crypto/paper-account` and the `PAPER ORDER` lines in `logs/crypto.log`. The log only covers **2026-10-30 → 11-02** (since the last restart), so it doesn't show the full rolling 100-trade window.
- I worked out each strategy's Kelly fraction backwards from its BUY size divided by equity. Equity comes from the strategies on the 2 % fallback (Ensemble, StatArb): about 1 815–1 828 EUR.
- **The task's SQL doesn't match the schema.** The column is `realized_pnl_eur`, not `pnl_eur`. `status='CLOSED'` sits on BUY rows, but P&L lives on SELL rows. Corrected query, which mirrors `KellyPositionSizingService` (return = pnl/entry, last 100 SELLs):

```sql
WITH t AS (SELECT strategy_name, realized_pnl_eur/entry_eur_amount r,
  ROW_NUMBER() OVER (PARTITION BY strategy_name ORDER BY created_at DESC) rn
  FROM crypto_paper_order
  WHERE side='SELL' AND realized_pnl_eur IS NOT NULL AND entry_eur_amount>0)
SELECT strategy_name, COUNT(*) n,
  AVG(CASE WHEN r>0 THEN 1.0 ELSE 0 END) win_rate,
  AVG(CASE WHEN r>0 THEN r END) avg_win,
  AVG(CASE WHEN r<=0 THEN -r END) avg_loss
FROM t WHERE rn<=100 GROUP BY strategy_name;
```

## Current Kelly fraction (implied from position size)
| Strategy | Size | Implied fraction |
|---|---|---|
| NightReversion | 91.4 → 90.3 → 50.9 EUR | 5.0 % (cap) 10-31 → 4.95 % 11-01 → **2.8 %** 11-02 |
| LiquiditySweep | 25 EUR | < ~1.4 % (min-EUR clamp, probably still at the floor) |
| VWAPReversion, BollingerSqueeze | 25 EUR | < ~1.4 % |
| Ensemble, BtcEthStatArb | ~36.4 EUR | 2 % fallback (< 20 closed trades) |

## Results 2026-10-30 → 11-02 (closed trades in the log)
| Strategy | n | Win rate | Avg win | Avg loss | f* | P&L EUR |
|---|---|---|---|---|---|---|
| LiquiditySweep | 37 | 32 % | 3.14 % | 0.80 % | +0.15 | +4.42 |
| NightReversion | 18 | 56 % | 1.18 % | 2.25 % | −0.29 | −4.37 |
| BollingerSqueeze | 19 | 42 % | 0.38 % | 0.48 % | −0.31 | −0.56 |
| BtcEthStatArb | 7 | 29 % | 0.58 % | 0.44 % | −0.25 | −0.38 |
| VWAPReversion | 3 | 67 % | 0.58 % | 0.15 % | +0.58 | +0.25 |
| Ensemble | 2 | 50 % | – | – | – | −0.44 |

Account: realized −184.56 EUR (−9.27 %), equity 1 814.59 EUR, 20 open positions.

## Trend
- **LiquiditySweep**: 32 % win rate vs. ~19 % historically. Wins are about 3.9× the losses, so the period's edge is positive. But n=37 over 3.5 days isn't enough to turn the 100-trade window; the size is still 25 EUR.
- **VWAP**: 67 % vs. ~50 %, but only 3 trades. It barely trades, so there's nothing to conclude.

## Finding: the 0.5 % floor never applies
0.5 % × 1 815 EUR ≈ 9 EUR, so `crypto.live.trading.position.min.eur=25` takes over. The real floor is about **1.4 %**. Every strategy with a Kelly fraction below that gets the same size, so Kelly doesn't tell them apart.

## Recommendation
1. **Keep Kelly.** NightReversion was scaled down automatically from 5 % to 2.8 % as its edge weakened. Don't switch strategy on this evidence: none is validated, and the LiquiditySweep improvement rests on 37 trades.
2. **Adjust the floor:** lower `position.min.eur` to ~10 EUR (at least in paper) so the 0.5 % floor actually applies.
3. **Lower the cap** to 2.5–3 % for unvalidated research strategies. NightReversion sat at 5 % (~91 EUR per trade, up to 9 positions at once) while losing money.
4. Run the corrected SQL above to confirm the exact 100-trade values. Re-evaluate in 2–3 weeks.
