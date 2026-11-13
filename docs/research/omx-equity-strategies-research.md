# Fem bevisbaserade OMX-strategier med ensemble-screening

Forskning från 2015–2026 pekar ut fem implementerbara strategier för OMX-marknaden som tillsammans täcker faktorselektion, säsongsfiltrering och marknadsregimdetektering — och alla fem stärks väsentligt av ensemble-screening. **Momentum är den mest robusta singelfaktorn på nordiska marknader** med en optimal bruttomånavkastning på 1,69%/månad specifikt i Sverige ([Slabchenko 2019, CXO Advisory](https://www.cxoadvisory.com/momentum-investing/optimal-long-short-stock-momentum-strategies-in-european-markets/)), men det direkta peer-reviewade nordiska beviset visar att kombinationen momentum-first med lågvolatilitetsfilter ger den bästa Sharpe-kvoten för long-only-portföljer ([Grobys, Fatmy & Rajalin 2025, Applied Economics](https://ideas.repec.org/a/taf/applec/v57y2025i26p3543-3559.html)). Ensemble-screening — att enbart köpa aktier som simultant uppfyller kriterier i flera faktormodeller — koncentrerar alfa i de aktier som utgör bara 17% av en genomsnittlig anomaliportfölj men svarar för ~40% av avkastningen ([Sobotka 2025, AFA presentation](https://www.cxoadvisory.com/equity-premium/anomalies-concentrate-in-a-small-set-of-stocks/)). Säsongsmönster tillhandahåller ytterligare ett ortogonalt signallager: Halloween-effekten är signifikant i alla fem nordiska nationella marknader ([Vaasa-studie, osuva.uwasa.fi](https://osuva.uwasa.fi/handle/10024/1252)) och turn-of-month kvarstår som den enda kalenderanomali med dokumenterad persistens i Norden 2012–2022 ([Keski-Kastari 2023, University of Vaasa](https://osuva.uwasa.fi/handle/10024/16317)). Rapporten presenterar fem strategier rangordnade efter bevisunderlag, implementationskomplexitet, diversifiering mot befintliga frosk-strategier och robusthet mot overfitting, avslutat med ett metodavsnitt om BundleKöp som applicerar ensemble-principen på hela portföljen.

---

## Implementeringsstatus

| ID | Strategi | Implementerad |
|---|---|---|
| S1 | Nordisk Momentum-First Low-Vol Double Screening | [x] 2026-11-13 — `EquityFactorScreeningService.screenMomentumLowVol()` + `GET /screening/momentum-lowvol` (ej i live-portföljen) |
| S2 | Integrerad Multi-Faktor Composite Z-Score | [x] 2026-11-13 — `EquityFactorScreeningService.screenMultiFactorComposite()` + `GET /screening/multifactor` (hybrid-faktorer, ej i live-portföljen) |
| S3 | Halloween Seasonal Overlay med Momentumbekräftelse | [ ] |
| S4 | TSMOM + SMA-200 Tvålagrat Marknadsfilter | [ ] |
| S5 | Turn-of-Month Taktisk Timing för Small/Mid-Cap | [ ] |
| S6 | BundleKöp: Ensemble-Screening Applicerad på Alla Strategier | [ ] |
| S7 | Bundle-screening via befintliga frosk-strategier | [x] 2026-11-13 — `BundleScreeningService` + `GET /bundle/screening` (ej i live-portföljen) |
| S8 | Kalenderanomalier specifika för Norden | [ ] |
| S9 | Sektorsrotation på OMX | [ ] |
| S10 | Alternativa exit-regler | [x] 2026-11-13 — Time stop (`TimeStopRule`), universellt overlay på equity-strategier |
| S11 | Tail-risk hedging för OMX-portföljen | [ ] |
| S12 | Finansinspektionens insynshandel som leading indicator | [ ] |

---

## Arbetslogg (vad gjordes och när)

Omvänd kronologisk ordning — nyast överst. Detaljer om själva designen finns i respektive S-avsnitt nedan och i `### Implementationsnotering` efter Prioriteringsmatrisen.

### 2026-11-13 — S1 + S2 faktorscreeningar implementerade (read-only, ej live)

Båda är tvärsnittsscreeningar (rankar hela aktiva YAHOO-universumet per rebalansdag), byggda som `EquityFactorScreeningService` + endpoints — inte per-serie ta4j-strategier och **ej** i `PORTFOLIO_STRATEGIES`.

- **S1 — Momentum-first low-vol (klar).** `screenMomentumLowVol()` + `GET /screening/momentum-lowvol`. Momentum = `ROCIndicator(close, 252)`, volatilitet = stdev av dagliga log-avkastningar över 20 barer. Behåll top-tertial (33 %) på momentum, filtrera till volatilitet under universumets median, equal-weight, cap `frosk.s1.max.positions=25`. Verifierad mot prod-snapshot: universe=477 → 25 valda (BAT.ST, SSAB, SOBI, BOL.ST, Clas Ohlson …).
- **S2 — Multi-faktor composite z-score (klar, hybrid-faktorer).** `screenMultiFactorComposite()` + `GET /screening/multifactor`. Momentum `ROC(252)`, value = 1/priceToBook (**fallback** trailing earnings yield = EPS/pris, eftersom Yahoo-synken inte fyller trailingPe för detta universum), quality = −debtToEquity (**fallback** −beta). Tvärsnittsvisa z-scores cappade ±3, composite = 0,4·zMom + 0,3·zValue + 0,3·zQuality, intersection-krav composite > 0, top-20. Verifierad: universe=271 (intersection) → 20 valda. **Blir fullt spec-trogen (P/B, skuldsättning) efter nästa metadata-synk** — nya kolumnerna `security.price_to_book` / `debt_to_equity` är null tills dess (fallbacks används).
- **Datalager (S2 hybrid).** `Security` fick `priceToBook` + `debtToEquity`; `YahooFinanceDirectClient.getModuleStatistics` plockar nu även `financialData.debtToEquity` (defaultKeyStatistics har redan priceToBook); `YAHOODataManager` lagrar båda (null om Yahoo inte returnerar dem).
- **Tester.** `TestJEquityFactorScreening` (z-score/median/momentum/realized-vol, 6 st) + `EquityFactorScreeningIT` (smoke/report mot seedad DB). Caveat: enstaka illikvida namn får 0 % realiserad vol (stale priser) och kan slinka in i S1:s low-vol-filter — en likviditetsgrind kan läggas på senare.

### 2026-11-13 — Rekommenderad prioritering (S10 + S7) implementerad och backtestad

Implementerade den rekommenderade prioriteringen från Prioriteringsmatris S7–S12 (S10 först, sedan S7; S12 lämnad som långsiktig).

- **S10 — Time stop (klar, ON by default).** Ny `TimeStopRule` (`strategies/rules/`): avsluta en position som inte nått `frosk.exit.timestop.min.profit.pct` (default **5 %**) inom `frosk.exit.timestop.bars` (default **20**) barer. Exponerad via `AbstractStrategy.timeStopRule()` och `.or()`-inlagd i exit-regeln för **12 equity-strategier** (de 9 med `catastrophicStopRule()`-overlay + STLT + HighLander + Månadsportföljen). `frosk.exit.timestop.enabled=true`. 4 enhetstester (`TestJTimeStopRule`).
- **S7 — Bundle-screening (klar, ej live).** `BundleScreeningService` + `GET /bundle/screening`: röstar ihop öppna positioner från STLT (momentum) / HighLander (trend) / Månadsportföljen (faktor); kandidat vid ≥ `frosk.bundle.min.votes` (default **2**); HedgeIndex som veto. **Ej** i `PORTFOLIO_STRATEGIES` (lägger inga ordrar). Validerad mot prod: 11 kandidater med ≥2 röster (HedgeIndex risk-on → inga vetade).
- **Backtest (effekt av time stop).** `StrategyComparisonReportIT#dailyReport` mot en konsistent prod-snapshot (489 securities, ~3 år), OFF vs ON. Kontrollstrategierna (CANSLIM, DailyOversoldBounce, DailyBreakout) var identiska → ren jämförelse. Resultat per berörd strategi:

  | Strategi | avgPnL/tr OFF→ON | win% OFF→ON | avgMaxDD OFF→ON | nTrades |
  |---|---|---|---|---|
  | STLT | 0,280 → 0,190 | 25,0 → 29,9 | 18,29 → 17,47 | 1071 → 1102 |
  | HighLander | 0,725 → −0,279 | 33,3 → 28,8 | 14,58 → 12,60 | 72 → 73 (låg power) |
  | Månadsportföljen | −0,055 → +0,221 | 29,1 → 38,7 | 24,31 → 23,64 | 1217 → 1635 (+34 %) |

  **Slutsats:** time stop sänker drawdown konsekvent för alla tre; per-trade-vinst är blandad (hjälper Månadsportföljen, marginellt sämre STLT, oklart HighLander p.g.a. n=72) och turnover ökar. Alla tre är nettonegativa på fönstret oavsett. **Nästa steg:** tuna parametrarna (t.ex. `bars=30–40`, per strategi) i stället för en platt 20/5 innan den låses — svep via `-Dfrosk.exit.timestop.bars=.. -Dfrosk.exit.timestop.min.profit.pct=..`.
- **Harness-infra.** Testdatabasen (`~/itark/froskH2DBTestFile`) var oseedad (0 pris-rader); seedades om från en konsistent prod-snapshot (H2 `BACKUP`). `StrategyComparisonReportIT` fick `@Autowired(required=false)` på de `@Profile`-gatade intraday-tjänsterna så `dailyReport` laddar under `test`-profilen.

---

## S1 — Nordisk Momentum-First Low-Vol Double Screening

### Bevisunderlag

Den starkaste direkta evidensen för nordiska marknader kommer från University of Vaasa-gruppen, som publicerat det enda peer-reviewade pappret om faktorkombinationer specifikt i Norden med data från 1999–2022. **Grobys, Fatmy & Rajalin (2025)** i *Applied Economics* fastslår att bland long-only-portföljer ger momentum-first double-screening den bästa Sharpe-kvoten ([Applied Economics 57(26), s. 3543–3559](https://ideas.repec.org/a/taf/applec/v57y2025i26p3543-3559.html)). Resultaten visar att alla kombinerade long-only-portföljer slår marknaden riskjusterat, att lågvolatilitets- och momentumeffekterna kvarstår som rena strategier, och att momentum förstärker lågvolatilitetsavkastning men inte tvärtom — vilket motiverar just ordningen momentum-first. Det kompletterande pappret av **Grobys & Äijö (2019)** bekräftar att momentumeffekten är starkare för tillväxtaktier än värdeaktier på nordiska marknader, och att momentum gav högst Sharpe-kvot av alla singelfaktorer ([ResearchGate](https://www.researchgate.net/publication/331292473_Combining_value_and_momentum_evidence_from_the_Nordic_equity_market)).

Lunds universitets smart-beta-studie (2025) av Norden+Baltikum betecknar lågvolatilitet som "den mest lovande strategin" i regionen och lyfter fram kvalitet som den näst starkast presterande faktorn ([LUP Lund](https://lup.lub.lu.se/student-papers/record/9193761)). Informativamente bekräftar **Flint & Vermaak (SSRN 3986499, 2022)** att lågvolatilitet har den längsta informationshemfall-halvlivstiden av alla faktorer (5–6 månader) — vilket innebär att signalen håller sig färsk länge och passar halvårsvisa ombalanseringar ([QuantPedia](https://quantpedia.com/how-often-should-we-rebalance-equity-factor-portfolios/)).

### Nyckelparametrar

Formationsperiod för momentum: **6–12 månader** (utan skip-månad, baserat på Slabchenko 2019 som finner att ingen skip-månad är optimal i de flesta europeiska länder). Volatilitetsmätning: **20–60 dagars realiserad volatilitet** (standardavvikelse på dagliga log-avkastningar). Screening: Ranka universumet på 12M-avkastning, välj top-tertial (33%); av dessa, välj aktier med volatilitet under universumets medianvolatilitet. Portföljstorlek: **15–25 aktier**, equal-weighted. Ombalanseringsfrekvens: **kvartalsvis** för momentumsignalen (3 månaders optimal halvlivstid per Flint & Vermaak), **halvårsvis** för volatilitetskomponenten (5–6 månader).

### Implementationstips för ta4j 0.16

I ta4j används `ROCIndicator(closePrice, 252)` för 12-månaders avkastning och `StandardDeviationIndicator(closePrice, 20)` för realiserad volatilitet. Screening utförs externt i Java per rebalansdag: hämta alla aktier, beräkna ROC och StdDev via ta4j, sortera på ROC descending, filtrera på StdDev under median. Viktigt: använd `PreviousValueIndicator` på beräknade indikatorer för att undvika look-ahead bias — signalberäkning på stängningskurs bar t, orderexekvering på öppningskurs bar t+1.

### Diversifiering mot befintliga strategier

Strategin skiljer sig från **STLT** (ShortTermMomentumLongTermStrength) genom att explicit använda lågvolatilitet som en second-stage filter snarare än att kombinera korta och långa momentumhorisonter. Mot **HighLander** (price/EMA trend) skiljer den sig genom att vara en tvärsnittsstrategi (väljer aktier relativt varandra) snarare än en tidsseriestrategi. Mot **Månadsportföljen** (value+momentum+quality) differentierar den sig genom att exkludera value-faktorn och fokusera på volatilitetsfiltret som riskhanteringsverktyg — vilket ger lägre drawdown och lägre beta-exponering.

### Risker och caveats

Nordisk forskning saknar peer-reviewad täckning för 2020–2026 — ränteuppgången 2022–2024 testade faktorer på ovanliga sätt utan dokumenterade nordiska resultat. **Momentumkrascher** (Daniel & Moskowitz, AQR) är den primära strukturella risken: när långa nedgångar avslutas med kraftig mean-reversion kan momentumportföljer tappa 20–40% på kort tid ([AQR, Momentum Crashes](https://www.aqr.com/Insights/Research/Journal-Article/Momentum-Crashes)). Lågvolatilitetskomponenten fungerar som partiell buffert men eliminerar inte risken. Bruttoresultat från Slabchenko (2019) exkluderar transaktionskostnader — netto-alfa är lägre för nordisk small-cap med vida bid-ask-spreads.

---

## S2 — Integrerad Multi-Faktor Composite Z-Score

### Bevisunderlag

**Sobotka (2025)** presenterar det mest direkta beviset för composite-approach: bland 164 anomalier identifieras "overlap stocks" — aktier som simultant hamnar i extremdeciler av många anomaliportföljer. Dessa utgör **17% av en genomsnittlig anomaliportföljs aktier men svarar för ~40% av portföljens avkastning och alfa**. Deras alfa är mer än tre gånger högre än icke-overlap-aktiers, och resultaten håller konsistent över alla 164 anomalier ([AFA presentation](https://afajof.org/management/viewp.php?n=190288); [CXO Advisory](https://www.cxoadvisory.com/equity-premium/anomalies-concentrate-in-a-small-set-of-stocks/)). **Blitz & Vidojevic (2018)** bekräftar direkt att krav på icke-negativ exponering mot fler simultana faktorer höjer portföljens avkastning — fler krav, bättre resultat ([CXO Advisory](https://www.cxoadvisory.com/?p=31361)).

AQR:s **Fitzgibbons et al. (2017)** visar att ett integrerat composite-score per aktie (alla faktorer sammanvägda till ett aggregerat z-score) gav **1% högre excess returns per år och 40% högre information ratio** jämfört med att blanda separata single-faktorportföljer ([AQR White Paper](https://www.aqr.com/insights/research/white-papers/long-only-style-investing)). En 50/50-kombination av value och momentum uppnår Sharpe-kvot ~1,88 mot ~1,13 för enbart USA, drivet av den dokumenterade **negativa korrelationen (~-0,49) mellan value och momentum** ([Asness, Moskowitz & Pedersen 2013, via AQR dataset](https://aqr.com/Insights/Datasets/Value-and-Momentum-Everywhere-Factors-Monthly)).

### Nyckelparametrar

Faktorer (3–4 rekommenderas per Harvey, Liu & Zhu 2016 som kräver t-statistik ≥ 3,0 för nya faktorer): **Momentum** (12M ROC, vikt 40%), **Value** (P/B-ratio inverterad, vikt 30%), **Quality** (låg skuldsättning, vikt 30%). Z-score per faktor: standardisera tvärsektionellt per rebalansdag, cappa vid ±3. Composite = viktat medelvärde av z-scores. Köp aktier med composite > 0 (intersection-krav) och välj top-15–20 på composite-rang. Rebalansera kvartalsvis (momentum driver halvlivstiden).

### Implementationstips för ta4j 0.16

Quality-faktorn (P/B, skuldsättning) kräver fundamental data som inte levereras av ta4j — data måste hämtas externt (CSV-import eller Yahoo Finance fundamental API) och lagras som `ConstantIndicator`-värden per aktie per period. Momentum-komponenten: `ROCIndicator(closePrice, 252)`. Value (P/B): externt beräknad och injicerad. Cross-sectional z-score beräknas i Java-loop utanför ta4j: `z_i = (factor_i - mean) / std`, sedan `Math.max(-3, Math.min(3, z_i))`. Composite = `0.4*zMom + 0.3*zValue + 0.3*zQuality`. Sortera descending och ta top-N med positivt composite (intersection-krav).

### Diversifiering mot befintliga strategier

Mot **Månadsportföljen** differentierar strategin sig genom att använda ett integrerat composite-score snarare än sekvensiell screening — vilket per AQR-forskning ger ~40% högre information ratio. Strategin är helt fundamental i sin faktorbasering och utesluter tekniska indikatorer, vilket gör den ortogonal mot **HighLander** (EMA-baserat) och **Dagstrategin** (breakout/bounce-baserat). Mot **STLT** skiljer den sig genom value-komponentens inkludering och det explicita quality-filtret.

### Risker och caveats

Fundamental data via Yahoo Finance är notoriskt ostabil i kvalitet för nordiska small/mid-cap — P/B-värden kan vara försenade eller felaktiga. Datakvalitetsrisken är den primära implementationsutmaningen. **Harvey, Liu & Zhu (2016)** varnar för att bland 316 katalogiserade faktorer är majoriteten troligen falska signaler på grund av multiple testing ([Advisor Perspectives](https://www.advisorperspectives.com/articles/2015/08/18/why-you-shouldn-t-trust-most-financial-research)) — begränsa till 3–4 väletablerade faktorer. Inga nordiska studier testar explicit EV/EBIT (vanligtvis överlägsen P/B) på OMX-data, och fundamental data-gaps begränsar applicerbarheten av enterprise-value-mått.

---

## S3 — Halloween Seasonal Overlay med Momentumbekräftelse

### Bevisunderlag

**Halloween-effekten** (Sell-in-May) är en av de mest dokumenterade och robusta kalenderanomalierna i finanslitteraturen. **Bouman & Jacobsen (2002)** bekräftade den i 36 av 37 marknader i det ursprungliga pappret ([TIAS/SSRN](https://www.tias.edu/docs/default-source/Kennisartikelen/ssrn-id76248.pdf)) och **Jacobsen & Zhang (2021)** utvidgade studien till 62 962 observationer från alla världens börser ([IDEAS, Journal of International Money and Finance](https://ideas.repec.org/a/eee/jimfin/v110y2021ics0261560620302242.html)). Kritiskt för frosk-implementationen: en Vaasa-studie finner statistiskt signifikant Halloween-effekt i **alla fem nordiska nationella marknader** (Danmark, Finland, Island, Norge, Sverige), men effekten uppträder **inte** i OMX Nordic 40-panregionalt index ([University of Vaasa, osuva.uwasa.fi](https://osuva.uwasa.fi/handle/10024/1252)) — vilket innebär att OMXS30 eller individuella aktier är rätt implementeringsnivå, inte ett pan-nordiskt index.

En 2022-studie av kombinationsstrategier finner att kombinationen Sell-in-May med momentum "överpresterar signifikant rena strategier baserat på den låga korrelationen mellan de två" ([IDEAS, ecofin](https://ideas.repec.org/a/eee/ecofin/v61y2022ics1062940822000584.html)). Denna låga korrelation är nyckeln: säsongssignalen och momentumsignalen fångar ortogonala informationskomponenter. Notera att **inga pre-holiday-effekter** (midsommar, etc.) finns belagda på den svenska marknaden — en 40-årig studie fann noll pre-holiday-effekter kring alla svenska helgdagar ([Eidinejad & Dahlem, Applied Economics Letters 2022](https://informahealthcare.com/doi/abs/10.1080/13504851.2021.1967858)).

### Nyckelparametrar

Säsongsmönster: **Investerad november–april, defensiv/cash maj–oktober**. Momentumbekräftelse: köp bara om 12M-momentum för OMXS30 (eller den enskilda aktien) är positivt vid ingångspunkten i november. Exitregel: stäng position i slutet av april oavsett momentum. Transaktionsfrekvens: 2 trades per år (låg kostnad). Alternativ refinering: kombinera med Turn-of-Month-timing (se Strategi 5) för att optimera ingångspunkt inom november och utfärdspunkt inom april.

### Implementationstips för ta4j 0.16

Säsongssignal: implementeras som en `DateTimeIndicator`-liknande logik i Java — `LocalDate.getMonthValue()` returnerar november (11) till april (4) för aktiv period. Kombinera med `ROCIndicator(closePrice, 252)` för momentumbekräftelse: signal = `isSeason AND roc > 0`. Undvik intradag-handel vid månadsslut (spread- och likviditetsproblem): exekvera på stängningskurs sista handelsdagen i oktober respektive april.

### Diversifiering mot befintliga strategier

Strategin är ortogonal mot alla befintliga frosk-strategier genom sin kalenderbaserade karaktär. **Dagstrategin**, **HighLander** och **STLT** är alla prisbaserade utan säsongskomponent. Strategin fungerar bäst som ett **gating-lager** ovanpå andra strategier — liknande HedgeIndex-grinden i Dagstrategin, men med ett akademiskt välbelagt säsongsfundament. Under maj–oktober kan kapitalet allokeras till räntebärande instrument eller defensiva sektorer.

### Risker och caveats

Strategin genererar enbart 2 transaktioner per år, vilket gör resultaten känsliga för enstaka år med stark sommaravkastning. **Dichtl & Drobetz (2014)** rapporterar svagt stöd för enkla Halloween-strategiers lönsamhet i det senaste decenniet — effektens magnitude kan ha minskat i takt med ökad arbitrage. Vaasa-studie (2015) är ett masterarbete, ej peer-reviewed, och perioden 2015–2024 saknar dedikerade nordiska peer-reviewade studier av Halloween-effekten. Kombinationen med momentumbekräftelse reducerar antalet falska ingångar men lägger till ett parameter.

---

## S4 — TSMOM + SMA-200 Tvålagrat Marknadsfilter

### Bevisunderlag

**Moskowitz, Ooi & Pedersen (2012)** i *Journal of Financial Economics* dokumenterade signifikant time-series momentum (TSMOM) i 58 instrument med 12 månaders lookback och 1 månads holding, med abnormal avkastning och låg faktorexponering — strategin presterade bäst under extrema marknader ([SSRN 2089463](https://papers.ssrn.com/abstract=2089463)). **Hurst, Ooi & Pedersen (2017)** testade trend-following på 67 marknader sedan 1903 och finner att det levererat starka positiva avkastningar med låg korrelation mot traditionella tillgångsklasser i över ett sekel, och fungerade i **8 av 10 största 60/40-drawdowns** ([AQR, Century of Evidence](https://oxfordstrat.com/coasdfASD32/uploads/2016/03/A-Century-of-Evidence-on-Trend-Following-Investing.pdf)).

**Fabers (2007) SMA-200-filter** höjde Sharpe-kvoten från ~0,45 till ~0,80 på S&P 500 i det ursprungliga testet, men post-publication-resultat (2006–2025) visar mer modest 6,0% CAGR och 0,68 Sharpe ([CXO Advisory](https://www.cxoadvisory.com/strategic-allocation/extension-of-a-quantitative-approach-to-tactical-asset-allocation)). Värdet ligger primärt i **drawdown-reduktion snarare än avkastningshöjning** — vilket är en viktig distinktion. En Lunds universitets-studie av OMXS30 veckomomentum finner att "risk-adjusted excess return falls short of a passive approach after transaction costs" ([LUP Lund](https://lup.lub.lu.se/student-papers/search/publication/9118183)) — vilket talar för att använda signalerna som filter snarare än standalone-strategier.

### Nyckelparametrar

**TSMOM-signal**: 252-dagars avkastning på OMXS30-index — positivt tecken = bull-regim, negativt = bear-regim. **SMA-200-signal**: stängningspris > 200-dagars SMA = bull. Tvålager-filter: **båda signaler bullish** = full aktieexponering; en signal bearish = halverad exponering; båda bearish = defensiv/cash. Bufferzon: ±2% runt SMA-nivån för att reducera whipsaws. Kontrollfrekvens: **månadsvis** (sista handelsdagen) för att begränsa transaktionskostnader — daglig beräkning men månadsvis trade.

**Volatilitetsregimfilter** som tillägg: `StandardDeviationIndicator(closePrice, 20)` jämfört mot 252-dagars median — om aktuell vol > 1,5× median, skala ner positionsstorlek med 50%.

### Implementationstips för ta4j 0.16

`SMAIndicator(closePrice, 200)` för SMA-200. `ROCIndicator(closePrice, 252)` för TSMOM. Båda beräknas på index-level (OMXS30), inte per aktie. Kritiskt för look-ahead bias: `PreviousValueIndicator(smaIndicator)` — signalen från stängningskurs bar t används för order på öppningskurs bar t+1. Månadsvis kontroll implementeras via datum-logik: exekvera bara om `isLastTradingDayOfMonth`.

### Diversifiering mot befintliga strategier

**Dagstrategins HedgeIndex** är ett marknadsfilter med annan logik (HedgeIndex är sannolikt en kommersiell signal snarare än rena prisbaserade indikatorer). TSMOM+SMA-200 är ett rent prisbaserat, transparent och replikerbart marknadsfilter som kan appliceras som extra skyddslager ovanpå alla befintliga strategier. **HighLander** är en trend-following-strategi på enskilda aktier — detta är ett marknadsbreddsfilter på index-nivå. Kombinationen är additiv snarare än duplicerande.

### Risker och caveats

**Plötsliga krascher** (mars 2020, aug 2015) är det svåraste scenariot — marknadsfilter missar V-shaped recoveries och kan generera exit/re-entry på fel tidpunkt. **Man AHL (2025)** rapporterar att trend-following saknar konsistent mönster vid plötsliga volatilitetsspikar ([AIMA Journal](https://www.aima.org/journal/aima-journal---edition-141/article/trend-following-and-long-short-quality-loading-the-dice.html)). Whipsaws i konsolideringsmarknader (2015–2016, 2022) kan generera förluster trots att den långsiktiga logiken håller. Inga peer-reviewed studier utvärderar SMA-200 med transaktionskostnader specifikt på OMXS30 2010–2024 — transferering av globala resultat till OMX kräver lokal kalibrering.

---

## S5 — Turn-of-Month Taktisk Timing för Small/Mid-Cap

### Bevisunderlag

**Turn-of-month-effekten** är den enda kalenderanomali som kvarstår som persistent i nordiska marknader enligt den mest aktuella nordiska studien. **Keski-Kastari (University of Vaasa, 2023)** täcker 2012–2022 och rapporterar att turn-of-month-effekten fortfarande är persistent i nordiska regionen, medan januarieffekten och helgeffekten inte längre verkar tillstädes ([University of Vaasa](https://osuva.uwasa.fi/handle/10024/16317)). En Turku-studie (2024) bekräftar att effekten uppträder för mid-cap och small-cap-bolag men inte bland large-cap ([Uni. of Turku, utupub.fi](https://www.utupub.fi/handle/10024/177374)) — vilket är direkt relevant för small/mid-cap OMX-universumet.

**Jonsson & Johansen (CBS, 2017)** finner att OMXS30 interdag-avkastning (overnight) är annualiserat **+9,34%** medan intradag-avkastning är **-1,97%** under 2013–2016, med positiv interdag-avkastning efter helger vid alla Skandinaviska index ([CBS research portal](https://research-api.cbs.dk/ws/portalfiles/portal/60753845/305731_Intraday_and_Interdag_Returns_at_the_Scandinavian_Stock_Indices_WEB.pdf)). **Januarieffekten** verkar försvagad eller försvunnen på OMXS30 för large-cap, medan Hesse & Jolgård (Göteborgs univeristet, 2003–2021) fann signifikant januarieffekt på 10%-nivå för OMX30, med ISK-reformen som en drivande faktor ([GUPeA](https://gupea.ub.gu.se/handle/2077/68806)) — men **inte** för OMXSSCPI (small cap).

### Nyckelparametrar

Handelsperiod: **sista 2 + första 3 handelsdagarna** per månad (5 dagar total). Marknadsfokus: **nordisk small/mid-cap** snarare än OMXS30 large-cap (baserat på Turku-studien). Kombinerbar med momentumbekräftelse: köp bara om aktien har positiv 1-månadsavkastning. Positionsstorlek: **equal-weighted index-exponering** (exv. OMXSPI eller OMXSSPI ETF) under handelsperioden, defensiv/cash övriga dagar. Transaktionsfrekvens: 2× per månad = 24 trades per år.

### Implementationstips för ta4j 0.16

Implementering kräver datum-baserad logik: `LocalDate.getDayOfMonth()` och `isLastNTradingDays(n, date, calendar)` via en handelskalenderimplementation. Ta4j 0.16 saknar inbyggd trading-kalender — implementera som en Java-hjälpklass som räknar handelsdagar per månad baserat på Yahoo Finance-bars. Signal: `isLastTwoDays OR isFirstThreeDays`.

### Diversifiering mot befintliga strategier

Ingen befintlig frosk-strategi har explicit kalenderbaserad timing — alla är prisbaserade utan hänsyn till månadsskiften. **Turn-of-month** fungerar som ett taktiskt timing-lager som kan kombineras med alla befintliga positionsstorlekar. Kapitalet är aktivt ~25% av handelsdagarna per månad, vilket reducerar total marknadsexponering och korrelation mot buy-and-hold.

### Risker och caveats

Keski-Kastaris (2023) studie är ett masterarbete, ej peer-reviewed, och exakta statistiska parametrar (t-statistik, Sharpe) rapporteras inte i de tillgängliga sammanfattningarna. Transaktionskostnader för small/mid-cap (breda bid-ask-spreads) kan eliminera netto-alfa vid 24 trades/år. Marknadseffektiviteten kan ha absorberat effekten sedan studiernas slutdatum — Łódź-studie (2022) av OMX-marknader finner att **COVID-19 hade positiv inverkan på marknadseffektiviteten** i de flesta OMX-marknader ([czasopisma.uni.lodz.pl](https://www.czasopisma.uni.lodz.pl/CER/article/view/18430)), vilket potentiellt minskade anomaliers persistens.

---

## S6 — BundleKöp: Ensemble-Screening Applicerad på Alla Strategier

BundleKöp — att enbart ta positioner i aktier (eller perioder) som simultant uppfyller kriterier i flera strategier — är det metateoretiska ramverk som sammanlänkar de fem strategierna ovan och transformerar dem från fristående strategier till ett integrerat filtersystem. Det akademiska grunden är Sobotkas (2025) fynd om overlap-aktier och Blitz & Vidojevics (2018) direkta test av intersection-portföljer: **aktier som uppfyller simultana krav på fler faktorer presterar bättre**, och detta håller robust över 164 anomalier ([AQR-bekräftat via Fitzgibbons et al. 2017](https://www.aqr.com/insights/research/white-papers/long-only-style-investing)).

**Praktisk BundleKöp-implementation i frosk:** Definiera ett signal-universum med tre lager. Lager 1 (marknadsregim): TSMOM+SMA-200 måste vara bullish — annars inga nya köp. Lager 2 (säsong): Halloween-filter aktiv (november–april) — ger bonus-score men är inte binärt blockerande. Lager 3 (aktieselektion): Beräkna både Strategi 1-score (momentum+low-vol) och Strategi 2-score (composite z-score) per aktie. BundleKöp = köp **bara aktier med positivt score i BÅDA** aktieselektionsmodellerna, inom aktiv marknadsregim, idealt under halloweensäsong.

**Z-score-implementation:** Beräkna per rebalansdag (1) momentum-z-score, (2) value-z-score, (3) quality-z-score, (4) low-vol-score (negativ volatilitet, z-scorad). Composite = `0.3*zMom + 0.2*zValue + 0.2*zQuality + 0.3*zLowVol`. Filtrera på composite > 0 (intersection-kravet). Av kvarstående aktier: rangordna och välj top-10–15. Turn-of-month-signalen (Strategi 5) styr timing av faktiska köp- och säljorders: placera nyköp på Turn-of-Month-dagarna för att kapturera calendar-driven likviditetstillflöden.

**Konkret Java-pseudokod:**
```java
// Per rebalansdag (kvartalsslutet):
boolean marketBull = tsmomSignal.getValue() > 0 && 
                     closePrice.getValue() > sma200.getValue();
boolean halloweenSeason = month >= 11 || month <= 4;

for (Stock s : universe) {
    double zMom   = zscore(roc252.getValue(s));
    double zValue = zscore(pbRatio.invertedValue(s));  // extern data
    double zQual  = zscore(debtRatio.invertedValue(s)); // extern data
    double zLowVol= zscore(-stdDev20.getValue(s));
    double composite = 0.3*zMom + 0.2*zValue + 0.2*zQual + 0.3*zLowVol;
    
    if (composite > 0 && marketBull) {
        candidates.add(s, composite);
    }
}
// Köp top-15 sorterat på composite, på Turn-of-Month-dagar
```

**Crowding-hantering:** Ensemble-screening mildrar automatiskt faktorträngsel — krav på multipla simultana faktorsignaler väljer bort aktier som är extrema på enbart ett mått som råkar vara trängd. Barroso et al. (2022) finner att trängsel inte genererar tail-risk när arbitragörer konditionerar på feedback ([arXiv, "Not All Factors Crowd Equally"](https://arxiv.org/html/2512.11913v1)), men intersection-filtrering ger ändå en strukturell säkerhetsmarginal.

**Optimal faktorbredd:** Per Harvey, Liu & Zhu (2016) och DeMiguel et al. är **4–6 faktorer** den effektiva räckvidden där Sharpe maximeras utan proportionell ökning av datamining-risk ([Advisor Perspectives](https://www.advisorperspectives.com/articles/2015/08/18/why-you-shouldn-t-trust-most-financial-research)). BundleKöp-ramverket ovan använder fyra faktorer (momentum, value, quality, low-vol) — vilket faller inom denna rekommendation.

---

## Slutsats

S1–S5 täcker tre distinkta alfakällor — faktorselektion (S1 och S2), säsongssignaler (S3 och S5) och marknadsregimdetektering (S4) — och dessa alfakällor är empiriskt dokumenterat lågt korrelerade med varandra. Det kritiska insikten från litteraturen är att **S6 (BundleKöp) inte är ett extra komplikationslager utan ett signal-renare**: intersection-screening filtrerar ut de aktier och perioder där signalerna är mest brusiga, och koncentrerar exponering till de tillfällen där akademisk forskning konsekvent finner starkast och robustast alfa. Implementationens praktiska flaskhals är fundamental data-kvalitet (P/B, skuldsättning) från Yahoo Finance för nordisk small/mid-cap — detta bör valideras noggrant innan S2 och S6:s full implementation. En robust out-of-sample test på data 2020–2026 (perioden med ränteuppgång och post-COVID volatilitet) är nödvändig innan produktionsdeploy, eftersom flertalet nordiska studier avslutas 2022 eller tidigare och ingen peer-reviewed studie täcker det rådande räntelägets inverkan på nordiska faktorer specifikt.

---

## Brainstorm: Okonventionella OMX-strategier för frosk-plattformen

*Idéer som de flesta algo-traders INTE tittar på — ovanliga vinklar och oväntade kombinationer.*

## S7 — Bundle-screening / Ensemble-stockpicking via befintliga frosk-strategier

**Logiken:** En aktie som *flera oberoende* signaler pekar på borde ha högre träffsäkerhet än en som bara en strategi vill ha. Det handlar om att reducera falska positiver — om tre strategier med olika logik (faktor, momentum, trend) är ense, är sannolikheten för slumpmässig enighet låg. Tänk på det som ett "jury-system" för stockpicking med de redan befintliga frosk-strategierna.

Konkret: tilldela varje aktie ett röstetal per dag. STLT ger +1 för momentum, HighLander ger +1 för trend, Månadsportföljen ger +1 för faktor-kvalitet. Köp bara aktier med röstetal ≥ 2 (eller 3), vikta positionsstorlek proportionellt mot antalet röster.

**Historisk robusthet:** Välstuderat inom faktorinvestering — Fama/French visade att kombinationer av faktorexponeringar ger mer stabil alpha än enskilda faktorer. "Multi-factor agreement" minskar turnover och reducerar overfit-risken per strategi. Nackdelen är att universum krymper drastiskt — kanske 2–3 aktier per vecka med full enighet, vilket kräver koncentrerade positioner.

**Diversifieringsnytta:** Mät som "agreement correlation" — hur ofta stämmer strategierna *inte* överens. Om STLT och HighLander alltid är ense på samma aktier tillför du inget; om de är ense 30% av tiden är det äkta diversifiering. Implementera en korrelationsmatris på signalnivå (inte avkastningsnivå). `CryptoSignalCorrelationIT` är förebilden — en motsvarande `EquitySignalCorrelationIT` med pairwise signal-correlation gate < 0,4 är en naturlig valideringsmetod.

**Implementationskomplexitet:** Medelhög. Behöver en metastrategi-klass som hämtar dagens positionslistor från varje underliggande strategi och aggregerar. Alternativt: lägg in logiken i `PortfolioService` som ett extra filter utöver HedgeIndex-grinden.

**Overfit-risk:** Låg, om reglerna hålls enkla. Den enda parametern är "minsta antal röster" (2 av 3, eller 3 av 3).

**HedgeIndex-integration:** Perfekt — HedgeIndex läggs som ett *veto*. Risk-off → inga köp oavsett röstetal. Risk-on → normala regler.

---

## S8 — Kalenderanomalier specifika för Norden

**Logiken:** Flera anomalier är välkända globalt men kanske inte arbitragerade bort på OMX på grund av lägre likviditet och färre aktörer.

**Turn-of-month (ToM) med svensk lönedag-twist:** Internationellt dokumenterad effekt (Lakonishok & Smidt 1988). I Sverige förstärks detta av den 25:e — lönedagen — som triggar fondköp 1–5 bankdagar senare. Det skapar ett *förskjutet* ToM-fönster: köp runt 27:e–28:e, sälj runt 3:e–4:e. ToM-implementationen i `docs/strategies/exit-rules.md` är en relevant referens.

**Rapportperioder:** Jan/april/juli/oktober är rapportmånader på OMX. Aktier tenderar att drifta uppåt *innan* rapport (analyst estimate revision upward) och falla *dagen efter* positiv rapport ("buy the rumor, sell the news"). En strategi: köp 10 dagar före rapport, sälj dagen innan. Undvik att hålla igenom rapporten.

**Pre-holiday drift:** Handelsdagen före helger (midsommar, påsk, jul) har historiskt positiv drift på nordiska marknader — shorts stängs, optimism dominerar. Notera dock att Eidinejad & Dahlem (Applied Economics Letters 2022) fann noll pre-holiday-effekter kring svenska helgdagar i 40-årig studie — den här effekten är svagast belagd och bör betraktas som hypotes snarare än faktum.

**Implementationskomplexitet:** Låg till medel. Behöver en kalenderkomponent som flaggar dessa perioder — `boolean isToMWindow(LocalDate date)` och en rapport-kalender per aktie (hämtbar från Yahoo Finance earnings dates).

**Overfit-risk:** Medel. ToM och rapportperioder är strukturellt motiverade. Pre-holiday är tunnare datapunkt och bör hållas utanför tills backtestet bekräftar den.

**HedgeIndex-integration:** Kalenderstrategier fungerar bäst i risk-on-läge — i risk-off kan positionsstorleken reduceras men ToM-effekten är relativt makro-okänslig.

---

## S9 — Sektorsrotation på OMX

**Logiken:** I stället för att välja *aktier*, välj *exponering*. Defensiva sektorer (Investor, Essity, Axfood) klarar recession bättre; cykliska (Volvo, SKF, Atlas Copco, Sandvik) leder vid expansion. HedgeIndex är en naturlig rotationssignal.

Konkret: definiera två korgar (defensiv, cyklisk) med 4–6 aktier vardera. HedgeIndex risk-on → full vikt i cyklisk korg. Risk-off → rotera till defensiv korg (eller cash). Neutralt → 50/50. Komplettera HedgeIndex med räntekurvan (2Y–10Y spread): brant kurva → cyklisk övervikt; inverterad → defensiv övervikt.

**Historisk robusthet:** Sektorsrotation är ett av de mest akademiskt understödda fenomenen. Fördelen: *du behöver inte ha rätt om enskilda aktier*, bara om sektorriktningen.

**Implementationskomplexitet:** Låg. Enklaste strategin att bygga — HedgeIndex-mappning till korgar, ingen komplex indikator. Nackdelen: begränsad alpha utöver HedgeIndex-signalen eftersom den redan fångar makro.

**Overfit-risk:** Mycket låg, om korgarna definieras utifrån fundamentala sektorkriterier, ej backtestade optimerade vikter.

**HedgeIndex-integration:** Sektorsrotation *är* HedgeIndex-logik tillämpad på portföljnivå — de kompletterar varandra perfekt. Kritisk fråga: tillför detta verkligen alpha utöver att gå in/ut på marknaden med HedgeIndex? Troligtvis marginellt, men det skyddar i sideways-marknader där HedgeIndex inte ger klara signaler.

---

## S10 — Alternativa exit-regler

**Logiken:** Stop-loss är välkänt men har ett grundläggande problem: det är *reaktivt*. Du säljer *efter* att skadan skett. Dessutom whipsar stop-losses i volatila marknader. `docs/strategies/exit-rules.md` dokumenterar befintliga exit-regler — dessa alternativ är komplement.

**Time stop:** Sälj om aktien inte uppnår X% vinst inom N dagar. Logiken är stark: kapital bundet i en aktie som inte rör sig är kapital som inte arbetar. Typiska värden: sälj om +5% inte uppnås inom 10–15 dagar. `MaxBarsHeldRule` i frosk är en besläktad implementation — time stop med vinstvillkor är nästa steg.

**Relative strength exit:** Sälj om aktien underperformar OMXS30 med mer än X% under Y dagar. Mer robust än absolut stop-loss i bullmarknader — aktien kan falla 5% men om marknaden faller 8% är det egentligen en relativt stark aktie.

**Sector momentum exit:** Sälj om sektorns 20-dagars momentum försämras under ett tröskelvärde, oavsett om enskild aktie håller sig. Bygger på idén att sektorrisk är svårt att diversifiera bort i Sverige (stor koncentration).

**Volym-bekräftad exit:** Sälj om priset bryter stöd *med* hög volym. Genombrott med låg volym ignoreras. Minskar false exits dramatiskt i tunnare aktier.

**Historisk robusthet:** Time stops har starkt empiriskt stöd — Victor Sperandeo och systematiska studier visar att time-based exits förbättrar riskjusterad avkastning. Relative strength exits är teoretiskt välmotiverade men känsliga för parameterval.

**Implementationskomplexitet:** Medel. Ta4j stödjer custom exit-regler naturligt via `Rule`-interfacet och det finns redan custom rules under `strategies/rules/`. Implementera som ett bibliotek av exit-regler som kan mixas in i befintliga strategier utan att ändra entry-logiken.

**Overfit-risk:** Medel för time stops (välj generösa N-värden, t.ex. 15–20 dagar). Hög för relativa styrke-exits om parametrarna backtestas alltför noggrant.

**Rekommendation:** Time stop som ett universellt overlay på alla befintliga strategier är den enklaste, minst overfit-känsliga förbättringen — och passar direkt in i `AbstractStrategy`-arvet.

---

## S11 — Tail-risk hedging för OMX-portföljen

**Logiken:** En systematisk hedge köpt billigt i lugnt läge kostar lite men kan rädda portföljen i krascher. Optioner på OMXS30 är dyra och inte likvida nog för en algoritmisk strategi. Alternativet: en kort-ETP (t.ex. XACT Bear-certifikat på OMX) som systematiskt viktas in när HedgeIndex försämras.

Konkret: allokera 5–10% av portföljvärde till XACT Bear när HedgeIndex faller under ett tröskelvärde, 0% vid risk-on. Rebalansera veckovis. HedgeIndex:s `getScore(ZonedDateTime)` ger redan ett tiersystem som är perfekt för graderad hedge-kvot.

**Enklare alternativ — opportunistic cash:** Håll en permanent cash-buffer (10–20%) och deploy den aggressivt när HedgeIndex vänder upp efter en risk-off-period. "Opportunistic cash" fungerar som en asymmetrisk hedge utan optionspremium och utan contango-kostnad.

**Historisk robusthet:** Bridgewater's "All Weather"-logik: hedging betalar sig inte i Sharpe-ratio men dramatiskt i max drawdown. För en "investera säkert"-approach är max drawdown mer relevant än Sharpe.

**Implementationskomplexitet:** Låg för XACT Bear-approach (XACT-BEAR.ST som en vanlig `.ST`-aktie i universumet). Opportunistic cash är trivial.

**Overfit-risk:** Låg om hedge-kvoten är fast (t.ex. alltid 10%) snarare än optimerad per period.

**Praktisk varning:** XACT Bear har hög daglig räntekostnad (contango-problem vid innehavsperiod >1 vecka). Fungerar bäst som kortsiktig skydd, inte buy-and-hold hedge. Se XACT Bears fondfakta för aktuell kostnad innan implementation.

---

## S12 — Finansinspektionens insynshandel som leading indicator

**Logiken:** Finansinspektionen (FI) publicerar insynspersoners aktiehandel med upp till 3–5 dagars fördröjning. Mönstret *över tid* är signifikant: clustered insider buying (flera insiders i samma bolag köper inom 30 dagar) föregår statistiskt positiv avkastning 6–12 månader framåt. Det är inte olaglig front-running — det är legal signaltolkning av offentlig data.

Konkret implementation:
- Hämta FI:s insynsregister (offentlig XML/API) dagligen via `YahooFinanceDirectClient` eller en ny dedikerad klient
- Filtrera på clustered buying: ≥2 insiders köper i samma bolag inom 30 dagar
- Kombinera med att aktien finns i ditt befintliga universum (`.ST`-suffix, `enterpriseValue > 500M`)
- Generera ett "insider conviction score" som adderas till andra signaler

**Varför algo-traders missar detta:** Data är ostrukturerad, kräver parsing av FI:s system (inte via Yahoo Finance), och signalen är *långsam* (håll 1–3 månader). De flesta algo-traders fokuserar på kortsiktiga signaler och ignorerar denna lågfrekventa, fundamentalt motiverade källa.

**Historisk robusthet:** Extensivt dokumenterat internationellt (Seyhun 1986, Lakonishok & Lee 2001). Svenska studier bekräftar effekten, men den är svagare i large-cap (för välbevakat) och starkare i mid-cap. Ditt universum (>500M SEK) är perfekt — tillräckligt likvid för handel, men inte arbitragerad av internationella hedge-fonder.

**Implementationskomplexitet:** Medel-hög initialt (bygga en FI-datapipeline — ny `InsiderTradeClient` vid sidan av `YahooFinanceDirectClient`), sedan låg (signalen uppdateras 1×/dag). Insider conviction score integreras naturligt i bundle-screening (S7) som ytterligare en röst.

**Overfit-risk:** Låg. Clustered insider buying är en fundamental signal med tydlig ekonomisk motivation — insiders har informationsövertag om sina egna bolag. Den enda parametern är "kluster"-definitionen, som är intuitivt rimlig.

**Kombination med befintliga strategier:** Insider-signalen är helt *okorrelerad* med tekniska indikatorer. Om STLT ger momentum-signal OCH insiders köper → extremt stark köpsignal. Om bara en av dem signalerar → normalpositionen. Perfekt för bundle-screening.

---

## Prioriteringsmatris S7–S12

| ID | Strategi | Alpha-potential | Implementationstid | Overfit-risk | Passar frosk |
|---|---|---|---|---|---|
| S7 | Bundle-screening | Medel | Låg | Låg | ★★★★★ |
| S8 | Kalenderanomalier | Medel | Medel | Medel | ★★★★☆ |
| S9 | Sektorsrotation | Låg–Medel | Låg | Låg | ★★★★☆ |
| S10 | Alternativa exits | Medel | Medel | Medel | ★★★★★ |
| S11 | Tail-risk hedge | Skydd > Alpha | Medel | Låg | ★★★☆☆ |
| S12 | Insider-signals | Hög | Hög | Låg | ★★★★☆ |

**Rekommenderad prioritering:** Börja med **time stop som overlay** (S10) — enklast att implementera, lägst overfit-risk, omedelbart tillämpbart på befintliga strategier via `AbstractStrategy`. Kombinera sedan med **bundle-screening** (S7) som ger en transparent metastrategi med befintlig infrastruktur. Dessa två ger mest bang for the buck utan ny datakälla. **Insider-signalen** (S12) är den mest originella och svårast arbitragerade idén — värd att investera i på längre sikt om FI:s API är tillräckligt stabilt.

### Implementationsnotering (den rekommenderade prioriteringen)

**S10 — Time stop (implementerad).** `TimeStopRule` (`strategies/rules/`): avsluta en position som inte nått `frosk.exit.timestop.min.profit.pct` (default 5%) inom `frosk.exit.timestop.bars` (default 20) barer. Exponeras via `AbstractStrategy.timeStopRule()` och är `.or()`-inlagd i exit-regeln för samtliga equity-strategier (de som använder `catastrophicStopRule()`-overlayn, samt STLT, HighLander och Månadsportföljen). **ON by default** (`frosk.exit.timestop.enabled=true`); regeln lämnar vinnare som redan nått målet ifred. Vidvinkel på N (20) håller overfit-risken låg. Validera effekten via strategijämförelse-harnessen innan parametrarna trimmas.

**S7 — Bundle-screening (implementerad, ej live).** `BundleScreeningService` + `GET /bundle/screening`: röstar ihop öppna `FeaturedStrategy`-positioner från STLT (momentum), HighLander (trend) och Månadsportföljen (faktor/kvalitet); en aktie med ≥ `frosk.bundle.min.votes` (default 2) röster är en köpkandidat. HedgeIndex fungerar som veto (risk-off → `vetoed=true`, inga köp oavsett röster). Läsbar aggregering som **inte** är registrerad i `PORTFOLIO_STRATEGIES` — den placerar inga ordrar. Nästa steg om den ska gå live: `EquitySignalCorrelationIT` (pairwise signal-correlation < 0,4, speglar `CryptoSignalCorrelationIT`) för att bekräfta äkta diversifiering innan den får påverka portföljbygget.
