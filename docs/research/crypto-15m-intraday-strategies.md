# Fem crypto-strategier som slår VWAP mean-reversion

Fyra år av forskning och praktikertester 2022–2026 pekar ut fem lovande alternativ till VWAP long-only mean-reversion för spot-crypto på 15-minutersbars — strategier med dokumenterad edge, möjliga att bygga i Java/ta4j, och testbara mot de 37 mynt som finns i frosk-analyzers kryptoprocess på Coinbase/Kraken. Den starkaste slutsatsen är att inget enskilt recept är komplett utan ett regimfilter: utan gating på trendtillstånd eller volatilitetsnivå äts kanterna upp av 2022-typ-björnmarknader eller av transaktionskostnader i choppiga perioder. Strategierna nedan rankas efter balansen mellan evidensstyrka, implementerbarhet i frosk-plattformen och förväntad robusthet över hela 2022–2026-perioden (bull, bear och ranging).

---

## 1. Inledning och kontext

Kryptomarknaden 2022–2026 genomgick tre distinkta faser: en brutal björnmarknad 2022 (BTC -76 %), ett lateralt/ranging 2023, och en stark bullperiod 2024–2025. En 15-minutersstrategi måste överleva alla tre för att vara produktionsduglig. De 37 mynt som ingår i frosk-analyzers kryptouniversum (Coinbase/Kraken) handlas dygnet runt, sju dagar i veckan, utan börs-stängningar — detta skapar tidssäsongseffekter som inte finns på aktiemarknaden. Frosk-plattformen har redan en fullt funktionell `IntradayStrategy`-pipeline med 15-minutersbars, `TradeOnCurrentCloseModel` och 0,03 % avgift per handel — arkitekturen är redo, vad som saknas är välmotiverade strategier.

Sökningen har explicit exkluderat VWAP long-only mean-reversion. De kandidater som utvärderades inkluderar EMA-baserad trendföljning med regimfilter, tidssäsongsanomalier, Bollinger Band-squeeze-breakout, statistical arbitrage via kointegration, och volatilitetsregimväxling. Nedan följer de fem mest lovande, rangordnade från mest till minst prioriterade.

---

## 2. Prioriterad lista: Top 5 strategier

---

### Strategi 1: EMA-korsning med ADX-trendfilter och volatilitetsregimgating

**Beskrivning av strategin.** Strategin kombinerar tre lager: (1) ett EMA-korsningssignal (EMA 9 och EMA 21 på 15m) som ger long- eller short-riktning, (2) ett ADX > 20-filter som kräver att en mätbar trend existerar innan inträde, och (3) ett volatilitetsregimfilter baserat på ATR-percentil mot 100-bars rullande historik som sorterar in marknaden i "quiet", "normal" eller "stressed". Long-inträden aktiveras när EMA 9 korsar ovanför EMA 21, ADX > 20 bekräftar trend, och regimen är quiet eller normal (ATR-percentil < 80). Stop-loss sätts till 1,5–2 × ATR(14). Kortsignaler speglar reglerna. BTC > 200-periods EMA på daglig bar fungerar som makrogating, analogt med HedgeIndex i equity-sidan av frosk-analyzer.

**Vad forskningen säger om edge och robusthet.** En praktikerstudie (DEV Community, 2026) testade **302 000+ 15-minuterskandlar** för BTC, ETH och SOL mars 2023 – mars 2026 och fann att kombinationen av 50/200-periods SMA-korsning som trendbaslinje konsekvent förbättrade resultaten jämfört med enbart momentum ([DEV Community](https://dev.to/gunnarthorderson/market-regime-detection-vs-trend-following-when-to-use-each-5d52)). Att skala exponering efter regimkonfidens > 70 % förbättrade Sharpe-kvoten med **0,4 enheter** jämfört med binär regimklass i ett backtest täckande 2024–2026 ([DEV Community](https://dev.to/gunnarthorderson/crypto-regime-detection-explained-the-bullbear-indicator-your-bot-is-missing-38kp)). Shen, Urquhart & Wang (2022) bekräftar att Bitcoin intradagmomentum ger "substantial economic gains especially in periods of a market downturn" ([Financial Review 57(2), DOI: 10.1111/fire.12290](https://doi.org/10.1111/fire.12290)), vilket speglar att halvtimmesmomentum är starkast under björnmarknader. En SMA-50 trendstrategi på 7 kryptovalutor (2024–tidig 2025) rapporterade **Sharpe 2,51** och kumulativ avkastning +231,51 % ([journal.uii.ac.id](https://journal.uii.ac.id/ENTHUSIASTIC/article/view/39963)), om än kortare track record. ADX > 20 som chop-filter är välkänt i teknisk analys och nämnns konsekvent i flera oberoende källor som det enskilt viktigaste filtret för kryptosystem; DarkBot rapporterar att ett volatilitetsfilter höjer Sharpe till ~1,2 primärt genom att avlägsna låg-kvalitetssetups ([DarkBot](https://darkbot.io/blog/volatility-filter-crypto)). Caveat: de flesta backtest är gjorda på Binance-data, inte Coinbase/Kraken, och är självrapporterade utan walk-forward-testning.

**Lämplighet för frosk-plattformen.** Ta4j 0.16 innehåller bekräftat EMA (`EMAIndicator`), RSI (`RSIIndicator`) och ATR (`ATRIndicator`). ADX-indikatorn finns sannolikt i `org.ta4j.core.indicators.adx`-paketet och behöver verifieras i källkoden. ATR-percentilberäkning kräver en hjälpklass som rankar aktuell ATR mot ett rullande fönster — ungefär 20–30 rader Java utöver standardindikatorer. Strategin implementeras som `IntradayStrategy` + `AbstractStrategy` + `IIndicatorValue` utan arkitektoniska ändringar. Normalized ATR (ATRP = ATR/pris × 100) är nödvändig för att jämföra trösklar konsekvent över 37 mynt med vitt skilda prisnivåer. **Implementeringssvårighet: Medel.**

**Rekommenderat nästa steg.** Verifiera att `ADXIndicator` finns i ta4j 0.16 (`src`-koden). Implementera en `ATRPercentileIndicator`-hjälpklass. Kör backtest på BTC och ETH 15m-data för hela 2022–2026 med walk-forward-partitionering (träning jan–dec 2022, test 2023, träning igen 2023, test 2024) för att bekräfta robusthet.

---

### Strategi 2: Tidssäsongsbaserad BTC/ETH-strategi (21:00–23:00 UTC)

**Beskrivning av strategin.** Strategin köper BTC (och optionellt ETH) kl. 21:00 UTC och säljer kl. 23:00 UTC varje dag — det tvåtimmarsfönster när NYSE är stängd (öppen 14:30–21:00 UTC), Tokyo är stängd (öppen 00:00–06:00 UTC), Hong Kong är stängd och Australien håller på att stänga. Fredag 22:00–23:00 UTC är det historiskt starkaste delintervallet. Strategin behöver inget tekniskt inträdesvillkor utöver ett BTC-positivt makroläge; ett enkelt filter som BTC > 200-periods SMA på dagsbasis minskar risken under björnmarknader.

**Vad forskningen säger om edge och robusthet.** Padyšák & Vojtko (Quantpedia, 2022) analyserade Gemini-börsens timdata från 9 oktober 2015 till 3 februari 2022: returerna för **22:00 och 23:00 UTC är de mest ekonomiskt och statistiskt signifikanta** på 5 %-nivå, medan timmar 3:00–4:00 UTC är sämst ([Quantpedia, feb 2022](https://quantpedia.com/are-there-seasonal-intraday-or-overnight-anomalies-in-bitcoin/)). Den ursprungliga strategin gav **annualiserad avkastning 33 %, volatilitet 20,93 % och maxdrawdown -22,45 %**. En uppdatering till 30 juni 2023 visade uppdaterad annualiserad avkastning på **40,64 % och Calmar-kvot 1,79**, med maxdrawdown -22,7 % mot BTC:s > -70 % under samma period ([Quantpedia, The Seasonality of Bitcoin](https://quantpedia.com/the-seasonality-of-bitcoin/)). Effekten kopplas till frånvaron av institutionell aktivitet från stängda börsmarknader och är konsistent med kryptointradag-anomali-dokumentation av Petukhina, Reule & Härdle (2020) ([arXiv 2009.04200](https://arxiv.org/abs/2009.04200v1)). Viktiga caveats: backtestdata är från en enda börs (Gemini), och uppdaterade data för 2024–2026 saknas — det är oklart om edge håller i ett mer institutionaliserat marknadsklimat efter BTC ETF-lanseringar 2024.

**Lämplighet för frosk-plattformen.** Strategin kräver inga nya indikatorer — bara ett tidsfönstervillkor implementerat som en tidsstämpelikontroll i `buildStrategy()`, eller som en dedikerad `@Scheduled`-metod med cron-uttryck kl. 21:00 och 23:00 UTC. Frosk-analyzers schemaläggningssystem (Tier 0–3) och kryptoprocessens befintliga cron-arkitektur hanterar detta direkt. Ingen ny datainfrastruktur behövs utöver befintliga 15-minutersbars. Det starkaste datakravet är historiska 15m-bars för BTC och ETH sedan 2022, vilket redan finns i systemet. **Implementeringssvårighet: Enkel.**

**Rekommenderat nästa steg.** Bygg en `CryptoTimeOfDayStrategy` som aktiveras exklusivt i 21:00–23:00 UTC-fönstret med ett BTC > 200-periods daily EMA makrofilter. Backtesta mot 2022–2026 med Coinbase/Kraken-data (notera eventuella spread-skillnader mot Gemini). Fredag-subfilter bör testas separat.

---

### Strategi 3: Bollinger Band-squeeze breakout med volymbekräftelse

**Beskrivning av strategin.** Strategin letar efter perioder av komprimerad volatilitet — "squeeze" — definierat som Bollinger Band-bredd (BBW) under sin 10:e percentil mot de senaste 100 barsen. När ett nytt 15-minutersljus stänger utanför banden **och** volymen för den baren överskrider det rullande 20-barsgenomsnittet ges ett inträde i utbrottsriktningen. Stop-loss sätts inuti banden (mittlinjen eller 1 × ATR). Position stängs när priset vänder tillbaka till mittlinjen, eller vid ett tidsstopp (t.ex. 8 bars = 2 timmar). Strategin är fundamentalt annorlunda från mean-reversion: den utnyttjar *volatilitetsexpansion* efter kompression, inte överdrift i existerande trend.

**Vad forskningen säger om edge och robusthet.** CoinQuant backtestade en ETH BB-breakout-strategi (4 timmars bars, 6 månaders period) och fann **+94,84 % avkastning, Sharpe 1,95 och win rate 33,3 %** med 0,1 % avgift ([CoinQuant](https://www.coinquant.ai/blog/breakout-trading-strategy-does-it-work-on-crypto-backtested)). Win rate under 50 % är konsistent med hur breakout-system typiskt fungerar: ett fåtal stora vinnare bär resultatet. Gainium-testade en squeeze-bot på 5m-bars med kombinerat filter och rapporterade **win rate ~60,58 % och profit factor ~1,42** med maxdrawdown -24,2 % ([Gainium community](https://community.gainium.io/t/strategy-final-squeeze-bot-long-short/4693)). DarkBot bekräftar att volatilitetsfilter minskar antalet affärer men höjer Sharpe genom att ta bort lågkvalitetssetups ([DarkBot](https://darkbot.io/blog/volatility-filter-crypto)). Svagheten är tydlig: CoinQuant:s resultat baseras på **21 affärer** över 6 månader — statistiskt insignifikant. Inga oberoende walk-forward-testade backtester på 15m crypto hittades.

**Lämplighet för frosk-plattformen.** Bollinger Bands finns bekräftat i ta4j 0.16 (`BollingerBandsUpperIndicator`, `BollingerBandsLowerIndicator`, `BollingerBandWidthIndicator`). En BBW-percentilklass (liknande ATR-percentilklassen i Strategi 1) behöver implementeras som rullande rangordning mot historik — samma hjälpklass kan återanvändas. Volymbekräftelse via en volymindikatorregel är standard. Den viktigaste utmaningen är att kalibrera squeeze-tröskeln (10:e percentil) för altcoins som historiskt haft andra volatilitetsprofiler än BTC. Strategin är lämplig som `IntradayStrategy` och kräver inga arkitektoniska ändringar. **Implementeringssvårighet: Medel.**

**Rekommenderat nästa steg.** Implementera `BBWidthPercentileIndicator` som en återanvändbar hjälpklass. Backtest på BTC och ETH 15m för 2022–2026 för att verifiera om edge håller på kortare timeframe än 4h. Walk-forward-test med rullande kalibrering av squeeze-tröskeln per mynt (BTC, ETH, SOL) är avgörande för att bedöma robustheten.

---

### Strategi 4: Statistical arbitrage BTC/ETH-spread med OLS-hedgekvot

**Beskrivning av strategin.** Strategin handlar spridningen mellan BTC och ETH som en mean-reverterande process. Hedgekvoten β beräknas via OLS-regression på log-priser över ett rullande fönster på 500 bars (~5 dagar på 15m). Spridningen definieras som `log(BTC) - β × log(ETH)`, normaliseras till ett z-score (medelvärde och standardavvikelse beräknade på samma rullande fönster). Inträde vid |z| > 2,0 i riktning mot noll, exit vid |z| < 0,5 eller vid stopploss |z| > 4,0 (divergensskydd). Handel sker enbart på spotmarknaden; det korta benet i praktiken kräver antingen marginhandel på Kraken eller att man behandlar strategin som riktningshandel på spreaden via en syntetisk position.

**Vad forskningen säger om edge och robusthet.** En IJSRA-studie (2026) rapporterade **Sharpe 1,58 för kointegreringsbaserade strategier i krypto** och identifierade BTC/ETH-relationen som "väldigt stabil i många marknadsregimer" ([IJSRA-2026-0283](https://ijsra.net/sites/default/files/fulltext_pdf/IJSRA-2026-0283.pdf)). Ko et al. (2023) testade parval vid 1m, 5m och 1h och fann att **resultat var signifikant bättre vid hög frekvens** — intradagbaserat medelvärdesåtergång replikeras inte på daglig nivå ([ResearchGate](https://www.researchgate.net/publication/376180709_Pairs_trading_in_cryptocurrency_markets_A_comparative_study_of_statistical_methods)). En MDPI-studie (2022) fann att distansmetoden och en optimerad variant av pairs trading **gav positiva avkastningar under 2022 när ETH föll -61,77 %** ([MDPI IFJ 2022](https://www.mdpi.com/2227-7072/12/3/77)), vilket visar bearmarknadsrobusthet. En Springer-studie (2025) med GHE-baserad pairs-strategi under 2022–2023 rapporterade att ansatsen var "remarkably effective in identifying lucrative investment prospects, even amid high volatility" ([Springer, 2025](https://link.springer.com/article/10.1007/s10614-025-11149-y)). Jämfört med Kalman-filter är statisk OLS mer robust i praktiken: en SSRN-studie visade att Kalman-filtrets omsättning var ~54 % högre och netto-Sharpe-kvoten sämre efter transaktionskostnader ([SSRN Baskaran](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=6727238)), och en GitHub-implementering fann att Kalman aldrig genererade affärer pga felaktig kalibrering ([GitHub Kalman Pairs](https://github.com/kaikiikeda/Kalman-Filter-Pairs-Trading)).

**Lämplighet för frosk-plattformen.** Ta4j har ingen inbyggd OLS-regression — denna måste implementeras som en hjälpklass (~50–100 rader ren Java, inga externa bibliotek behövs). Z-score-normalisering, rullande medelvärde och standardavvikelse är standardoperationer. Den viktigaste praktiska begränsningen är det korta benet: spotblankning på Coinbase är begränsat, och Kraken Futures har 1-timmes funding-cykler för EEA-klienter (en 15m position håller i genomsnitt en kvart av en fundingperiod, kostnaden per affär är liten men ackumuleras). Rullande Engle-Granger-test (var 4:e vecka på de senaste 500 barsen) kan implementeras som ett periodiskt schemalagt jobb för att övervaka kointegrationsstabilitet. **Implementeringssvårighet: Komplex.**

**Rekommenderat nästa steg.** Implementera en `OLSRegression`-hjälpklass och en `ZScoreIndicator` i frosk-analyzer. Kör Engle-Granger-test på BTC/ETH 15m-data för hela 2022–2026 för att bekräfta kointegrationsstabilitet (inklusive FTX-kollaps november 2022). Starta med enbart long-sidan (BTC long, ETH short implicit via spread) för att undvika blankningsproblematiken i ett första steg.

---

### Strategi 5: Volatilitetsregimväxling med ATR-percentil (momentum i lågvol, defensiv i högvol)

**Beskrivning av strategin.** Strategin delar marknaden i tre regimer baserade på ATR(14) rankad mot 100-bars rullande historik: "Quiet" (percentil < 20), "Normal" (20–80), och "Stressed" (> 80) med 10-procent hysteresis-buffert och 2-bars bekräftelse för att undvika regimflimmer. I "Quiet"-regim aktiveras en momentumstrategi (EMA-korsning eller tidseriemomentum). I "Normal"-regim handlas halvstorlek. I "Stressed"-regim stängs alla positioner och inget nytt inträde tillåts. Strategin är ett meta-ramverk som omsluter Strategi 1 och gör regimgating explicit.

**Vad forskningen säger om edge och robusthet.** Banerjee (SSRN 2025) klassificerar BTC i tre regimer (Expansion/Neutral/Contraction) med kvoten kortsiktig/långsiktig realized volatility kombinerad med rörelsestyrka på högfrekvent 2024–2025-data ([SSRN 5920642](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5920642)). GJR-GARCH identifierades som mest träffsäkert vid volatilitetsprognoser på intradag inklusive 15-minutersnivå i en MDPI-jämförelse (2024) av sex modeller ([MDPI IJFS 2025](https://www.mdpi.com/2227-7072/14/4/90)). LuxAlgo Volatility Regime Switches specificerar exakta trösklar: Stressed vid 80:e percentil, Quiet vid 20:e, 10-procent hysteresis, 2-bars bekräftelse, lookback 100 bars ([LuxAlgo](https://www.luxalgo.com/library/indicator/volatility-regime-switches/)). Dobrynskaya (SSRN/Journal of Alternative Investments, 2023) fann positiv momentum på kort sikt (upp till 2–4 veckor) och signifikant reversal på längre horisont över 2 000 kryptovalutor 2014–2020 ([SSRN 3913263](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=3913263)), konsistent med att olika regimer kräver olika strategier. Det saknas dock peer-reviewed jämförelser av regimväxling mot alltid-long momentum specifikt på 15m crypto.

**Lämplighet för frosk-plattformen.** ATR-percentilberäkning (samma hjälpklass som i Strategi 1), regimklassificering och en wrapper-struktur runt befintlig momentumlogik kräver ingen arkitektonisk förändring i frosk-analyzer. Normalized ATR (ATRP = ATR/pris × 100) är kritisk för jämförbarhet mellan 37 mynt. Hysteresis-logiken innebär att regimen inte ändras förrän en tydlig gräns passerats — detta minskar signalflimmer men kräver noggrann implementering för att undvika lookahead-bias. Den primära utmaningen är kalibrering: samma percentilgränser (20/80) kanske inte fungerar lika bra för altcoins med mer extrema volatilitetsprofiler som BTC. **Implementeringssvårighet: Komplex.**

**Rekommenderat nästa steg.** Bygg `VolatilityRegimeIndicator` som beräknar ATRP-percentil med hysteresis. Integrera som en gating-komponent runt Strategi 1 och mät Sharpe och MaxDD med och utan regimfilter på 2022–2026-data för att kvantifiera värdet.

---

## 3. Strategier som valdes bort och varför

**Cross-sectional momentum (long topp-kvintil, short botten-kvintil).** Edge dokumenterades på daglig frekvens för 2020–2022 men statistisk signifikans försvinner helt i 2023–2024 (GitHub backtest: alpha sjunker från 36,2 % till 13,5 %, t-värde faller till 1,08 ([GitHub crypto-momentum-backtest](https://github.com/prams2104/crypto-momentum-backtest))). Kryptokorrelationerna är för höga för meningsfull cross-sectional spridning i universum av 37 mynt.

**Short-only-strategier.** Inga peer-reviewed 15m-backtest med realistiska kostnader (funding rates, slippage, spread) hittades. Blankning av spotcrypto på Coinbase är begränsat och Kraken Futures funding för EEA-klienter sker varje timme. Short-only som isolerad strategi saknar tillräckligt evidensunderlag.

**Kalman-filter pairs trading.** Omsättningen var ~54 % högre än OLS-baserad approach och transaktionskostnader eliminerade fördelen i den enda relevanta jämförande studien ([SSRN Baskaran](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=6727238)). Kalibreringssvårigheten gör det till ett onödigt komplex andra steg.

**Turn-of-the-candle (0, 15, 30, 45-minuterseffekten).** Shanaev et al. (2023) dokumenterade **0,58 bps positiv avkastning per minut** vid kanten av 15-minuterskandlar ([Heliyon 9(3), 2023](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC10015199/)), men effekten kräver mikrosekundsprecision vid orderläggning och Kitron & Wengrowicz (2026) fann att råa reversal-kanter på ~1,3 bps per handel inte täcker typiska transaktionskostnader om ~5 bps round-trip ([arXiv 2608.21888](https://arxiv.org/pdf/2608.21888)).

---

## 4. Slutsats och rekommenderad prioritetsordning

De fem strategierna är inte ömsesidigt uteslutande — Strategi 5 (volatilitetsregimväxling) är ett meta-lager som omsluter Strategi 1 och gör dem komplementära snarare än konkurrerande. Den rekommenderade implementeringsordningen är: börja med **Strategi 2** (tidssäsongsbaserad 21:00–23:00 UTC) som en snabb vinst med minimal kodbas och verifierbar edge, bygg sedan **Strategi 1** (EMA + ADX + volatilitetsregim) som kärn-momentumstrategi, och lägg till **Strategi 3** (BB-squeeze breakout) som komplement för att fånga volatilitetsexpansionsmöjligheter. Strategi 4 (stat arb BTC/ETH) är det enda alternativet med stark akademisk backing för björnmarknadsrobusthet men kräver antingen blankningsmöjligheter eller omformulering till en nettolongspreadstrategi. Strategi 5 bör byggas när Strategi 1 är validerad — den tillför primärt drawdown-kontroll snarare än ny alpha.

Det enskilt viktigaste att ta med sig: **inget av dessa recept är testat på exakt Coinbase/Kraken-data för 2022–2026 i peer-reviewed miljö.** Survivorship bias i altcoin-data är ett allvarligt problem som frosk-analyzers `Security.active`-filter inte hanterar retroaktivt. Walk-forward-testning med tydlig tränings- och testperioduppdelning är inte valfritt — det är ett krav för att avgöra om edge är robust eller en backtest-artefakt.

---

## 5. Bilaga: Brainstorm ovanliga intraday-strategier (2026-10-26)

**Kontext:** Coinbase spot (37 coins, 15 min) + Kraken Futures (kan shorta). VWAP long-only mean-reversion fungerar inte i nedgångsfaser. Mål: robust, konsistent drift — inte maximal avkastning.

### Prioriteringsordning

#### 1. Kelly-baserad position sizing ★★★★★
Rullande win rate (W) och R-kvot per strategi → fractional Kelly: `f* = W − (1−W)/R`, använd 25–50% av f*. Löser nedgångsfas-problemet automatiskt: när win rate sjunker minskar Kelly-storleken automatiskt. Gör detta oavsett allt annat.

- **Robusthet:** Extremt hög — matematiskt bevisad
- **Komplexitet:** Låg-medel (rullande statistik-tracker)
- **Overfit-risk:** Nästan noll — adaptivt, inte parameteranpassat

#### 2. Microstructure / natt-mönster ★★★★
Coins som rör sig ovanligt mycket på låg volym (01–07 UTC) tenderar att återgå när europeisk/amerikansk volym återkommer. Filtrera 15-min-bars på UTC-timme + volym relativ 7d-snitt, kör befintlig mean-reversion *bara* under dessa timmar. Kompletterar och utvidgar rec #3 (21:00–23:00 UTC-fönstret) från kapitel 4.

- **Robusthet:** Medel-hög — stöds av microstructure-forskning (Admati & Pfleiderer 1988, uppdaterad för crypto)
- **Komplexitet:** Medel
- **Overfit-risk:** Låg om parametrarna hålls enkla

#### 3. Funding rate som marknadsfasindikator ★★★★
Hög positiv funding = bull-sentiment → aktivera mean-reversion. Låg/negativ funding = bear/neutral → minska eller stäng av. Kraken API ger funding rate per timme. Löser direkt bristen på marknadsfasfilter i `CryptoVWAPReversionIntradayStrategy` och är ett alternativ/komplement till GARCH+ADX-regimhooken.

- **Robusthet:** Hög — strukturellt driven
- **Komplexitet:** Medel (ny feed, enkel logik)
- **Overfit-risk:** Låg

#### 4. Cross-coin momentum decay ★★★★
BTC/ETH bryter ut starkt (>1,5 % på 15 min, hög volym) → identifiera laggande altcoins som *inte* rört sig ännu → ta position i BTC:s riktning → stäng efter 2–4 candles. Kräver en parallell "master signal"-feed i arkitekturen. Bör köas efter VPIN/OFI-hypotesen pga. konkurrens om samma historiska data.

- **Robusthet:** Medel-hög — "spillover effects" i crypto stöds av akademisk forskning
- **Komplexitet:** Medel-hög (arkitekturellt mer än algoritmiskt)
- **Overfit-risk:** Medel — håll fördröjningsfönstret enkelt (1–4 candles)

#### 5. Basis / cash-and-carry arbitrage ★★★
Köp spot (Coinbase) + shorta futures (Kraken) på samma coin → fånga basis-mean-reversion. Trend-oberoende, strukturell drift.

- **Robusthet:** Hög
- **Komplexitet:** Hög — kräver synkroniserad orderläggning på två börser, hanterar margin-krav
- **Overfit-risk:** Låg

#### 6. Ensemble / röstning ★★★
Kör bara när 2 av 3 strategier (VWAP + LiquiditySweep + ett tredje genuint oberoende filter) är eniga om riktning. Nyckeln är dekorrelation — mät korrelation mellan befintliga signal-outputs; <0,4 krävs för att ensemble ska ge mervärde.

- **Robusthet:** Medel-hög *om* signalerna är dekorrelerade
- **Komplexitet:** Medel
- **Overfit-risk:** Medel-hög — risk att man tunar vilket filter som "vinner" på historisk data

#### 7. Sälja volatilitet ★★
Kräver Kraken options eller komplex delta-hantering med futures. Avvakta tills Kraken options-API är stabilt och integrerat.

- **Robusthet:** Medel (fungerar i sideways, katastrofalt vid trend-breakouts)
- **Komplexitet:** Mycket hög
- **Overfit-risk:** Medel
