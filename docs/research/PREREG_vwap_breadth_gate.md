# Pre-registration — marknadsbreddsgrind för CryptoVWAPReversionIntradayStrategy

Skriven 2026-10-16, FÖRE att breddgrinden utvärderats på 400-dagarsdatan (`~/itark/backfill`).
Ändring efter första utfallsanalys = avvikelse som loggas i §9 och gör testet explorativt.

## 0. Bakgrund och vad som redan är sett
Hypotesen kommer från en explorativ analys 2026-10-16 på lagrade 15m-barer i krypto-DB:n
(`intraday_bar`, **2026-09-16 – 2026-10-16**, 37 produkter, 1 473 replayade trades i Python):
- Breda nedgångsdagar (≥ 80 % av produkterna faller) gav −0,53 % brutto/trade, uppdagar +1,04 %.
  Stopp-, exit- och bekräftelsevarianter ändrade inte nedgångsdagarnas tecken.
- En realtidsgrind "köp bara om ≤ 50 % av universumet stänger under sin UTC-dags-VWAP" gav +0,40 % netto/trade
  mot +0,06 % utan grind (n = 269 vs 1 473), med samma tecken i båda halvorna av fönstret. Cirka åtta varianter
  prövades i den analysen (tre bredd-trösklar, två median-trösklar, BTC-vs-VWAP), så resultatet är in-sample och
  urvalsbelastat.

Det explorativa fönstret överlappar **P2** nedan (2026-09-16 – 2026-10-08). P2 är därför inte oberoende för
denna hypotes. **P1 (före 2026-08-01) har inte setts för breddgrinden och är det enda rena testet.**

Varningsflagga, noterad i förväg: H5 i `~/itark/PREREG_vwap_signal_filters.md` (exkludera bar med ≥ 5 samtidiga
VWAP-signaler) fick fel tecken i P1 (−0,033 pe). H5 mäter samtidiga *signaler*, inte andel av universumet under
VWAP, men båda är proxys för "marknadsbred nedgång". Ett liknande utfall här är ett rimligt utfall.

## 1. Hypotes
VWAP-reversion fungerar när en produkt avviker från VWAP på egen hand, men inte när hela marknaden ligger under
sin VWAP (då är avvikelsen beta/momentum, inte en dislokation). Att bara ta entries när en minoritet av
universumet ligger under VWAP ger **högre** netto per trade än att ta alla entries.

Riktning låst: filtrerade trades > alla trades.

## 2. Parametrar (låsta)
**Breddmått** `B(t)` vid signalbaren `t` (= `entryTs` i replayen; strategin fyller på signalbarens close via
`TradeOnCurrentCloseModel`, så `B(t)` är känt vid entry — ingen framtidsinformation):
- `B(t)` = andelen produkter i breddsuniversumet med en bar på exakt tidsstämpel `t` vars close < produktens egen
  UTC-dags-VWAP vid samma bar. VWAP beräknas som `SessionVWAPIndicator(series, UTC)`: session = UTC-datum för
  barens sluttid, typiskt pris (H+L+C)/3 viktat med exakt volym, kumulativt från sessionens första bar.
- Breddsuniversum: alla produkter med CSV i `~/itark/backfill` **utom** `ETH-EUR`, `USDC-EUR`, `USDT-EUR`,
  `EURC-USDC` (samma uteslutningar som i den explorativa analysen; stablecoins mäter inte marknadsriktning).
- Om färre än 15 produkter har en bar vid `t` är `B(t)` odefinierat och traden **behålls** (grinden inaktiv).
  Antalet sådana trades rapporteras.

**Varianter** (exakt tre, inga andra):
- G40: behåll trade om `B(t)` ≤ 0,40
- **G50: behåll trade om `B(t)` ≤ 0,50 — primär**
- G60: behåll trade om `B(t)` ≤ 0,60

## 3. Data och replay
- Trades: `VwapReplayIT#replay` med exakt volym (nuvarande live-beteende), den riktiga strategiklassen, parametrar
  låsta mot `application-crypto.properties` (stretch 1,2 / RSI7 < 35 / SMA96-trendfilter / cooldown 2 / stop 2,7 /
  24 barer). Samma `replay_trades_exact.csv`-format som PREREG_vwap_signal_filters.
- Analysuniversum för trades: de 14 validerade tickers i `validated_tickers.txt` (samma begränsning och skäl som
  PREREG_vwap_signal_filters §9). Sekundärt rapporteras alla replayade tickers, utan beslutsvikt.
- Perioder: P1 = före 2026-08-01 (primär, ren out-of-sample), P2 = 2026-08-01 – slutet av backfillen (bekräftelse,
  delvis kontaminerad, se §0).

## 4. Mått
- Primärt: förbättring = medel netto/trade för behållna trades − medel netto/trade för alla trades, i procentenheter.
  Netto = brutto − 0,20 % (taker round trip); känslighet −0,32 %.
- Day-block bootstrap (B = 4 000, hela UTC-dagar för entry), ensidigt p för förbättring > 0, 90 % CI.
  Holm-justering över de tre varianterna.
- Rapporteras per variant: n, andel behållna, vinstandel, brutto och netto/trade, max andel från en ticker,
  förbättring i P1:s två datumhalvor och i hela 400-dagarsperiodens två halvor.

## 5. Beslutsregel — krav för att gå vidare till paper (alla villkor, primärt G50)
1. Förbättring ≥ +0,05 pe i P1 och ≥ +0,02 pe i P2 (samma tecken).
2. Holm-justerat p < 0,10 i P1.
3. Behåller ≥ 15 % av P1-trades och ≥ 100 trades i P1.
   *Avviker medvetet från 40 % i PREREG_vwap_signal_filters: en marknadsgrind förväntas per konstruktion stänga
   majoriteten av signalerna (explorativt 18 %). Satt före utfallet.*
4. Ingen enskild ticker > 25 % av behållna P1-trades.
5. Förbättring > 0 i båda datumhalvorna av P1.
6. Behållna P1-trades har netto > 0 vid 0,20 % (en förbättring som bara gör en förlust mindre räcker inte för paper).

Om G50 klarar alla villkor: grinden implementeras bakom en flagga (default AV), med paritet paper/live enligt
gällande regel, och körs i paper/SHADOW i ≥ 300 trades innan live-beslut. G40/G60 är känslighetsanalys: om G50
klarar men G40 och G60 har olika tecken på förbättringen noteras resultatet som skört.
Om G50 inte klarar: grinden rapporteras som underkänd. Trösklar justeras inte för att rädda den, och G40/G60 kan
inte ersätta G50 i efterhand.

## 6. Det som INTE görs
Inga andra breddmått (median-avkastning, BTC vs VWAP, andra trösklar, kombinationer med H1–H5). Ingen justering av
strategins parametrar. Ingen tickerselektion efter utfall. Inga ändringar i produktionskod som del av detta test.

## 7. Förväntat utfall
Explorativt (in-sample, 30 dagar): förbättring cirka +0,3 pe. Rimlig förväntan efter regression mot noll och
med H5 i åtanke: 0 till +0,15 pe i P1. Ett utfall nära noll eller negativt i P1 är fullt möjligt och ska läsas som
att 30-dagarsfyndet var regim- eller urvalsdrivet.

## 8. Genomförande
Breddmåttet beräknas från backfill-CSV:erna och kopplas på `entryTs` i replay-filen. Statistiken är en
rad-för-rad-port av `VwapFilterAnalysisIT` (bootstrap, Holm, halvor), körd separat från produktionskoden.

## 9. Avvikelselogg
| Datum | Avsnitt | Ändring | Skäl |
|---|---|---|---|
| 2026-10-16 | §10 | Beskrivande tilläggsanalys (dagtyp i efterhand över P1/P2) körd efter utfallet | Förklara utfallet; ingen beslutsvikt, inga trösklar ändrade |

## 10. Resultat (2026-10-16)
Replay: `VwapReplayIT` (2/2 tester gröna, inkl. `productionParametersAreMirrored`) körd på en ren kopia av HEAD;
utdata byte-identisk med `~/itark/backfill/replay_trades_exact.csv` (6 715 trades). Rapport och skript:
`~/itark/backfill/breadth_gate_report.txt`, `~/itark/backfill/breadth_gate.py`. B(t) definierat för alla trades.

**Primärt (14 validerade tickers), P1 = 2 553 trades / 310 dagar, baslinje netto −0,059 %, vinst 53,8 %:**

| Variant | Behållna P1 | Netto/trade P1 | Vinst P1 | Förbättring P1 (90 % CI) | Holm p | P1-halvor | P2 förbättring | Utfall |
|---|---|---|---|---|---|---|---|---|
| G40 | 213 (8 %) | −0,262 % | 52,1 % | −0,203 pe (−0,466…+0,050) | 1,00 | −0,218 / −0,211 | +0,362 | Underkänd |
| **G50** | 304 (12 %) | **−0,317 %** | 50,7 % | **−0,259 pe (−0,471…−0,041)** | 1,00 | −0,383 / −0,187 | +0,429 | **Underkänd** |
| G60 | 390 (15 %) | −0,250 % | 51,3 % | −0,191 pe (−0,374…−0,006) | 1,00 | −0,319 / −0,125 | +0,186 | Underkänd |

Alla tre varianter har **fel tecken i P1, i båda P1-halvorna**. För G50 och G60 utesluter 90 %-intervallet noll
på den negativa sidan. P2 (kontaminerad, §0) är positiv för alla tre — samma mönster som H5 (P1 −0,033, P2 +0,057).
Sekundärt (alla 32 replayade tickers) ger samma bild: G50 P1 −0,198 pe, P2 +0,301 pe.

**Beslut enligt §5: hypotesen är underkänd. Ingen implementation, ingen paper-körning. Trösklarna justeras inte.**

Beskrivande (ej beslutsgrundande): i efterhand klassade breda nedgångsdagar (≥ 80 % faller) är dåliga även i P1
(1 117 trades / 122 dagar, netto −0,62 %, mot +0,66 % på uppdagar). Diagnosen "strategin förlorar på breda
nedgångsdagar" håller alltså över 400 dagar, men andelen produkter under VWAP *vid entry* identifierar inte de
dagarna: i P1 hade trades med B ≥ 0,8 (69 % av alla) netto −0,02 %, de med B < 0,5 −0,32 %.
