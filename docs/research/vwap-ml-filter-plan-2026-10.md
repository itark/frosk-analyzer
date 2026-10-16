# Plan: stabilt ML-filter för VWAP (Coinbase) med befintlig LSTM-tjänst

Datum: 2026-10-08
Mål: förbättra `CryptoVWAPReversionIntradayStrategy` på Coinbase genom ett ML-baserat signalfilter.
Tjänst: `/Users/fredrikmoller/itark/git/frosk-lstm-service/` (FastAPI + ONNX, `POST /signal-filter`).

## Utgångsläge (mätt)

- VWAP är den enda Coinbase-strategin med mätt edge, men den är liten: +0,09 %/trade i paper (t≈1,3) mot en
  ej modellerad spread. Realistiskt ≈ +0,05 % (t≈0,9). Mätt exekveringskostnad ≈ 0,119 % round trip.
- LSTM-tjänsten är i praktiken icke-funktionell:
  1. `compute_features` krävde kolumnen `time`, som API-barerna (Java `LstmBar`) saknar → varje anrop gav `fallback=True`.
  2. Modellen `lstm_20260907T203529_acc0495` saknar skicklighet: val acc 0,495, val loss 0,695, alla folds kollapsade
     till konstant prediktion; 0 % av prediktionerna ≥ 0,55.
  3. Omträningen laddade om varje ny modell villkorslöst.
- Target (tecknet på 4-bars framåtavkastning, alla barer) är fel fråga för ett VWAP-filter. Filtret ska avgöra
  *om en given VWAP-signal blir vinstgivande*, inte förutsäga marknadens riktning.

## Principer

1. Meta-labeling: träna enbart på tidpunkter där VWAP faktiskt signalerar; label = nettoutfall efter kostnad.
2. Samma exit-regler och kostnadsmodell som live (inkl. spread) i label-beräkningen.
3. Purged walk-forward med embargo ≥ HORIZON; ingen slumpmässig split.
4. Jämför alltid mot enkla regelfilter (regim, tid på dygnet, volatilitet) — ML måste slå dem för att få finnas kvar.
5. PREREG före varje utvärdering mot hold-out; hold-out rörs en gång.
6. En modell promotas bara om den slår baslinjer (promotion gate). Övervakning larmar på konstant modell.
7. Fail-closed i produktion tills modellen bevisats; ingen tyst fallback.

## Faser

### Fas 0 — Göra tjänsten pålitlig (KLAR 2026-10-08, ej committad)
- [x] `time`-buggen: features får inte bero på `time`. Kontraktstester mot Java `LstmBar` (25 barer, ingen tid).
- [x] Promotion gate (`promotion.py`): acc ≥ majoritetsbaslinje + 0,01, val loss < ln 2 − 0,001, inga kollapsade folds.
      `train.py` flyttar `latest.onnx` bara vid godkänt; `retrain_scheduler.py` laddar bara om vid promotion.
- [x] Övervakning: `/health` visar request-/fallback-andel och fördelning av senaste sannolikheter
      (`recent.degenerate`, `share_ge_threshold`); error-logg när modellen är konstant. `model_version` loggas per signal.
- [x] `compute_target` ger NaN (inte 0) för rader utan framtida data; fyra lookahead-tester hade felaktig index-offset.
- Verifierat: 40 tester gröna (`test_lstm_model.py` ej körd — torch saknas i sandbox; träningsgaten ej körd end-to-end).
- Obs: nuvarande `latest.onnx` är fortfarande den kollapsade modellen. Den bör inte aktiveras (flaggorna är av).

### Fas 1 — Data
- Retention är 30 dagar för 15m-barer → för litet. Bygg ett arkiv (egen tabell, via `CryptoIntradayDataService.fetchRange`
  / `ProductService.getPublicCandles`) med ≥ 12 månaders historik för de tickers VWAP handlar.
- Logga varje VWAP-signal med tidsstämpel, features och utfall (för senare meta-label-träning).

### Fas 2 — Baslinje utan ML
- Replay av VWAP-reglerna på arkivet med live-lika kostnader. Testa regelfilter (regim, 21–22 UTC-fönster enligt
  `PREREG_vwap_seasonality_window.md`, volatilitet). Detta är lägsta nivån ML ska slå.

### Fas 3 — Meta-label-modell
- Features: nuvarande 4 + VWAP-avstånd, tid på dygnet, regim, volym relativt snitt, spread-proxy.
- Modell: börja med logistisk regression / gradient boosting; LSTM bara om den tillför något utöver dessa.
- Utvärdering: purged walk-forward, nettoutfall per trade, dagskluster-t, jämförelse mot Fas 2.

### Fas 4 — Skuggkörning
- Java loggar filtrets beslut utan att agera (SHADOW-läge i `CoinbaseStrategyLifecycleService`); minst 30 stängda
  trades / 30 dagar enligt promotionskriterierna.

### Fas 5 — Aktivering
- Båda flaggorna krävs: `forecast.lstm.enabled=true` och `crypto.vwap.lstm.filter.enabled=true`.
- Inför trösklar från Fas 3, fail-closed, kill-switch vid degenererad modell.

## Förväntan
Ett enkelt regelfilter kan mycket väl matcha eller slå ML. Då är rätt utfall att behålla regelfiltret och
låta LSTM-spåret vara avstängt. Det är ett giltigt resultat.

## Öppna beslut (ej besvarade)
1. Arkiv-/backfill-tabell i `CryptoIntradayDataService` — ja/nej och hur långt bakåt.
2. Fail-open eller fail-closed i produktion (rekommendation: fail-closed).
3. Är LSTM ett krav eller en kandidat bland flera (rekommendation: kandidat).
