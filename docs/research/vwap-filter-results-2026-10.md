# VWAP-filter: resultat av PREREG_vwap_signal_filters.md (2026-10-08)

Underlag: 400 dagar 15m-candles (Coinbase), den riktiga `CryptoVWAPReversionIntradayStrategy` via `VwapReplayIT`,
analys via `VwapFilterAnalysisIT`. Rådata och rapporter i `~/itark/backfill` (`validation_report.txt`, `vwap_filter_report.txt`).

## Validering
Replayen återskapar de inspelade signalerna (8 sep–5 okt) till 80,9 % totalt, men 95–100 % på tickers med kompletta barserier och
33–73 % på tickers med många saknade barer. Analysuniversum begränsat (beslut loggat i PREREG §9) till 14 tickers som validerar
(träff ≥ 90 %, precision ≥ 85 %): AERO, BTC, HBAR, HYPE, LTC, MON, NEAR, SOL, SUI, TAO, VVV, WLD, XRP, ZEC.
Tillsammans: recall 97,5 %, precision 93,0 %. Slutsatserna gäller bara dessa likvida par.

## Basstrategin (aktuell konfiguration, 14 tickers)
| Period | Trades | Brutto/trade | Netto @0,20 % | Netto @0,32 % | Dag-klustrat t (brutto) |
|---|---|---|---|---|---|
| P1 (före 2026-08-01, out-of-sample) | 2 553 | +0,141 % | −0,059 % | −0,179 % | +2,10 |
| P2 (från 2026-08-01, parametrarna trimmade här) | 564 | +0,336 % | +0,136 % | +0,016 % | +4,61 |
Månadsvis brutto är positivt i 13 av 14 månader (enda negativa: 2025-10, −0,045 %). (Beskrivande, ej förregistrerat.)
Slutsats: en verklig men liten bruttoedge (~0,14 %) som inte täcker taker-avgifter (0,20 % round trip) out-of-sample.

## De fem hypoteserna (P1)
| Hypotes | Kvar | Förbättring | Holm p | Utfall |
|---|---|---|---|---|
| H1 tid 21–22 UTC | 5 % | +0,070 pp (CI −0,28…+0,49) | 1,00 | Ej rekommenderad (för få trades, halvorna olika tecken) |
| H2 hög volatilitet exkluderas | 52 % | −0,064 pp | 1,00 | Ej rekommenderad (fel riktning i P1) |
| H3 stretch ≥ 2,0 % | 27 % | +0,074 pp (CI −0,06…+0,22) | 0,94 | Ej rekommenderad (starkaste, men inte signifikant; netto ≈ 0 vid 0,20 %) |
| H4 volymspik ≥ 2× | 44 % | −0,001 pp | 1,00 | Ej rekommenderad |
| H5 samtidighet ≥ 5 | 83 % | −0,033 pp | 1,00 | Ej rekommenderad (fel riktning i P1) |
Inget filter uppfyller beslutsregeln (§5). Inga trösklar justeras.

## Konsekvenser
- Enkla entry-filter på dessa features separerar inte vinnare från förlorare. Ett ML-filter på samma slags features har därmed lite att
  hämta; PREREG §6 (ML-grind) är inte uppfylld och ML-spåret bör vänta.
- Hävstången är kostnaden: brutto ~0,14 % mot 0,20 % taker. Maker-entry (0,02 %) skulle teoretiskt ge ~+0,02 % netto i P1 med taker-exit
  och ~+0,10 % med maker båda håll, före missade fills och adverse selection. Spår B (PREREG §7) är inte körd och måste mätas.
- Paper-resultatet (P2) är i sig inte ett out-of-sample-bevis: parametrarna trimmades på den perioden.

## Spår B: maker-entry (övre gräns, `VwapMakerEntryIT`, 14 validerade tickers)
Limit vid signalbarens close, gäller nästa bar. Fyllnadsgrad 89–93 %; fyllda trades har något lägre brutto än alla
(adverse selection −0,02…−0,04 pp), medan de missade signalerna hade +0,46…+0,66 % brutto (de studsar direkt).

| Period | Baslinje netto/trade (taker 0,20 %) | Maker-entry + taker-exit (0,12 %), netto per signal | Skillnad (90 % CI) |
|---|---|---|---|
| P1 (konservativ fyllnad) | −0,059 % | −0,014 % | +0,044 pp (+0,021…+0,068) |
| P2 (konservativ fyllnad) | +0,136 % | +0,176 % | +0,040 pp (+0,004…+0,072) |
Maker på båda ben (0,04 %, kräver vilande exit-order som inte modelleras, endast gräns): P1 +0,057 %, P2 +0,249 %.

Slutsats: maker-entry sparar ca 0,08 pp i avgift men förlorar ca 0,04 pp i missade/sämre fyllnader. Nettovinsten är ~+0,04 pp per signal
och gör inte P1 positiv (−0,014 %). Det är en övre gräns: omentré efter missad fyllnad, köposition och exit-timing är inte modellerade.
Sparad spread/slippage på entry-sidan (uppmätt exekveringskostnad ~0,12 % round trip) ingår inte och kan öka vinsten, men är inte verifierad.
Rimligt nästa mätsteg är verklig fyllnad i SHADOW med liten storlek, inte mer simulering.

## Faktiskt paper-utfall (CRYPTO_PAPER_ORDER, 2026-08-03 – 2026-10-08, hämtat 2026-10-08)
990 stängda VWAP-trades, netto efter 0,20 % avgift (REALIZED_PNL_EUR / ENTRY_EUR_AMOUNT), fyllnad vid bar-close utan spread.

| Urval | Trades | Netto/trade | SE | Positiva dagar |
|---|---|---|---|---|
| Alla tickers | 990 | +0,029 % | 0,066 | 35/64 (55 %) |
| 14 validerade (likvida) | 347 | −0,044 % | 0,102 | 31/61 |
| Övriga | 643 | +0,068 % | 0,085 | 36/64 |
Per månad (alla): aug +0,082 % (+34,6 EUR), sep +0,066 % (+41,2 EUR), okt −0,197 % (−45,6 EUR, 162 trades).
Senaste tre dagarna: 6 okt −16,8 EUR, 7 okt −36,4 EUR, 8 okt −13,2 EUR (blandar gammal och ny konfiguration; trendfilter/cooldown
infördes ~2026-10-07; urvalet är litet).
Replayen med samma (gamla) konfiguration ger för de 14 tickers +0,072 % netto/trade mot paper +0,010 %: samma nivå inom bruset.
Replayen med nuvarande konfiguration (+0,136 % netto, P2) överskattar alltså paper-utfallet; paper är statistiskt ~0 efter avgifter.
Slutsats: ingen påvisbar nettoedge i paper ännu; behov av ≥ 300 trades med nuvarande konfiguration innan beslut om SHADOW/LIVE.

## UI:ts signalbaserade Daily P&L vs paper (2026-10-08)

`/intraday/pnl` parar BUY/SELL ur `intraday_signal` per ticker och drar 0,20 % avgift. Alla signaler räknas, oavsett om paper-motorn handlade dem.

- Alla VWAP-signaler jun–okt: 3 490 round-trips, snitt +0,139 % netto (SE 0,052), 66 % positiva dagar. Per månad: jun −0,14, jul +0,10, aug +0,27, sep +0,16, **okt −0,16 %**.
- Från 3 aug (paper-perioden): 2 544 round-trips, +0,165 %. Av dem handlades 990 av paper-motorn (+0,029 %, identiskt med paper-resultatet) och 1 554 handlades inte (+0,252 %, SE 0,098).
- Skillnaden 0,22 pp (t ≈ 1,9) kommer av urval, inte av fyllnad: paper-motorn tar ett undergrupp av signalerna och just den gruppen presterar sämre. Orsak inte utredd.
- Daily P&L-panelen summerar procenttal över ~50 trades/dag med olika kapital; en +18 %-dag är inte 18 % avkastning på något konto.
