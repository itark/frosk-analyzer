# Coinbase-strategier — analys och återbesök av avstängningsbeslut

**Datum:** 2026-10-05
**Fråga:** Vilka Coinbase-strategier är avstängda och varför, håller besluten, och kan någon av de övriga ändras och aktiveras för Coinbase-kontot?
**Kort svar:** Ingen av de avstängda ska slås på. Ingen övrig strategi kan flyttas till Coinbase utan att ändras mer än datan motiverar. VWAP-reversion är enda kandidaten, och dess edge är ungefär lika stor som exekveringskostnaden paper-kontot inte betalar.

## Underlag och metod

- `crypto_paper_order` (paper-fills), `intraday_signal` (historiska signaler, parade BUY→SELL per ticker och strategi) och `intraday_bar` (15m, 2026-09-05 → 2026-10-05, 37 produkter). Analysen gjordes på en kopia av krypto-databasen; originalet är orört.
- Regim: BTC-EUR dagligt, SMA50 + ADX(14)≥25 (som `CryptoRegimeService`), föregående dag används för att undvika framtidsläckage.
- Kostnad: 0,10 % taker per ben (volymtier bekräftad 2026-08-03). Spread: medel 0,114 %, median 0,081 % vid VWAP-köp (n=1 704, `spread_percent`). PREREG_liquidity_sweep_15m.md mäter 0,119 % exekveringskostnad per round trip.
- Omspelning i Python av EMACross-Long, RangeBreakout och VWAP på de lagrade 15m-staplarna. Validering: omspelad VWAP gav brutto +0,365 % (n=1 345) mot +0,342 % (n=1 247) för de inspelade signalerna i samma fönster. Omspelningen saknar LSTM-filter (tjänsten ej körbar) och parametrarna är orörda.

## Resultat per strategi

| Strategi | Status | Utfall | Bedömning |
|---|---|---|---|
| EMACross-Long | Avstängd 2026-07-31 | n=1 112 (15–31 juli): brutto −0,447 %, vinst 20,6 %, t=−7,4. Endast TRENDING_UP-dagar (n=297): −0,28 % (t=−2,7). Omspelning senaste 30 dagarna (alla TRENDING_UP, dagens regim-grind): brutto +0,014 %, netto −0,19 %, t=+0,2 | Förblir av |
| RangeBreakout | Avstängd 2026-07-31 | n=463: brutto −0,429 %. TRENDING_UP-dagar (n=155): −0,29 %. Omspelning: brutto −0,126 %, t=−1,8 (−2,6 dagsklustrat) | Förblir av |
| CryptoShort | Påslagen, ej körbar på spot | n=59 på 10 dagar, brutto +0,87 %, vinst 52,5 %, t=2,8. Allt i TRENDING_DOWN | Hör hemma på Kraken Futures |
| EMACross-Short | Påslagen, ej körbar på spot | n=137, brutto +0,16 %, netto −0,04 % | Hör hemma på Kraken Futures |
| VWAP-reversion | Paper | 926 trades, +108,81 EUR på 2 000 EUR, netto +0,09 %/trade, t=1,33, vinst 58,6 %, profit factor 1,11, max realiserad drawdown 6,7 %. Positiv augusti, september, oktober | Enda kandidaten, se nedan |
| Liquidity Sweep | Pre-registrerad | Ej utvärderad (se "Avvikelse") | Rörs ej före datagaten |
| OFI (1m) | Pensionerad 2026-09-09 | — | Oförändrat |

### Återbesöket av EMACross-Long och RangeBreakout

Research-docens rekommendation 5 (testa med TRENDING_UP-grind) är delvis redan implementerad: båda strategierna har `CryptoMarketRegimeRule(TRENDING_UP)` i koden, men grinden har aldrig körts live eftersom strategierna stängdes av innan den lades till. Omspelningen på de senaste 30 dagarna (BTC TRENDING_UP alla dagar) är därför den rättvisaste prövning som går. Grinden lyfter EMACross-Long från −0,45 % till ≈0 brutto, men kostnaden är ≈0,3 % per trade, så netto förblir negativt. RangeBreakout är fortsatt negativ brutto.

Begränsningar: en månad i en enda regim, ett test per strategi utan parametertrimning, inget LSTM-filter. Parametertrimning mot denna månad vore datafiske. Vill man fortsätta: skriv en PREREG för en enda variant.

### VWAP-reversion — varför "aktivera för Coinbase" inte är självklart

- Paper fyller på 15m-stängningskurs utan spread. Med spread inräknad (halv spread per ben) blir netto ≈ +0,05 %/trade, t≈0,9, alltså inte skiljt från noll.
- Exit som maker-limit på VWAP (0,02 % avgift) testades som en enda variant: +0,025 % mot +0,051 % för taker-exit. Sämre, eftersom stängningsexit fångar överskjutet. Idén avfärdad.
- Beskrivande och i efterhand (ej pre-registrerat): netto efter avgift per regim på signalnivå var +0,29 % RANGING, +0,10 % TRENDING_UP, +0,03 % TRENDING_DOWN. Senaste månaden var TRENDING_UP genomgående.

## Rekommendationer

1. Behåll EMACross-Long och RangeBreakout avstängda.
2. Kör shortstrategierna på Kraken Futures (redan påslagna i den profilen); Coinbase spot saknar venue.
3. Gör paper realistiskt före livebeslut: fyll köp på `best_ask` och sälj på `best_bid`.
4. Lägg ett promotion-krav i `CoinbaseStrategyLifecycleService` på spread-justerad förväntan (utöver 30 trades / 30 dagar / drawdown <15 %). VWAP uppfyller redan de nuvarande kriterierna, som inte säger något om kostnadsjusterad edge.
5. SHADOW-läget skickar riktiga Coinbase-ordrar parallellt med paper. Rätt verktyg för att mäta verklig slippage på VWAP, men med små belopp.

## Avvikelse (för PREREG-logg)

En aggregatfråga mot `crypto_paper_order` inkluderade av misstag `CryptoLiquiditySweepIntradayStrategy` (antal trades, vinstandel, summa P&L, samt brutto per trade ur signalerna). Siffrorna har inte använts i någon bedömning. VWAP delades inte upp per timme, i linje med PREREG_vwap_seasonality_window.md.
