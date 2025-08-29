# frosk-analyzer


**Hedge Fund-Style Custom Index Strategy**

---

### **Overview**
This strategy combines macro-level signals with micro-level stock selection and options-based hedging to build a robust long/short trading framework. The core component is a rule-based signal index that determines market regime (risk-on vs. risk-off) and adjusts exposure accordingly.

---

### **1. Macro Signal: Custom Index**

#### **Goal:**
To generate a daily/weekly score based on key market indicators to determine when to go long, hedge, or go short.

#### **Indicators & Signal Logic:**
Each condition met adds **+1 point** to a **risk-off score** unless noted otherwise. The table below lists the indicators, the rule to evaluate, and how many points they contribute.

| Category | Indicator | Rule / Trigger | Points |Implemented|
|----------|-----------|----------------|--------|--------|
| Volatility | **VIX** | VIX > 25 **and** rising (e.g., 5-day ROC positive) | +1 |YES
| Volatility | **VVIX** | VVIX > 110 **and** rising | +1 |YES
| Tail Risk | **SKEW** | SKEW > 140 | +1 |
| Volatility Skew | **SDEX** | SDEX > 110 **OR** 5-day % change > +5% | +1 |
| Commodities | **Crude Oil** | Crude Oil (WTI) drops > 5% in last 5 trading days | +1 |YES
| Commodities | **Gold** | Gold closes **above** the previous 10-day **high** (breakout) | +1 |YES
| Equities | **S&P 500** | S&P 500 close **below** 200-day SMA | +1 |YES
| Equities | **NASDAQ vs S&P** | NASDAQ 30-day return < S&P 30-day return (underperformance) | +1 |YES
| FX | **USD/JPY** | USD/JPY rising above a threshold (e.g., 150) or strong 5-day appreciation | +1 |
| FX | **AUD/USD** | AUD/USD drops > 2% in last 5 trading days | +1 |
| FX | **DXY (USD Index)** | DXY > 105 **and** rising | +1 |
| Inflation | **US CPI YoY** | CPI YoY > 3.5% **and** sequentially rising | +1 |
| Inflation | **Core CPI MoM** | Core CPI MoM > 0.4% | +1 |
| Interest Rates | **10Y Treasury Yield** | 10Y > 4.5% **and** rising | +1 |
| Yield Curve | **2Y - 10Y Spread** | 2Y-10Y < -50 bps (deep inversion) | +1 |
| Market Breadth | **Advance/Decline** | A/D line falling for 5+ consecutive days | +1 |
| Credit Stress | **High-Yield OAS** | HY OAS > 500 bps (5.00%) | +1 |
| Liquidity | **TED Spread** | TED Spread > 50 bps (0.50%) | +1 |

> **Notes:** thresholds (e.g., VIX 25, DXY 105, 10Y 4.5%) are examples — tune to your universe and regime. Use smoothed/ROC versions where appropriate to avoid single-day noise.

---

#### **Regime Classification & Allocation (Total Score)**
Compute the **total risk-off score** by summing points for all triggered indicators. Use this to set portfolio posture and allocations.

- **0–3 points — Strong Risk-On**
    - Target equity exposure: **80–100%** (long S&P/NASDAQ/Dow)
    - Hedge: minimal (cash or small put protection)
    - Preferred: high-beta growth, momentum names

- **4–7 points — Mild/Transition (Cautious Risk-On / Neutral)**
    - Target equity exposure: **40–70%**
    - Hedge: partial protection (collars, protective puts on core names)
    - Preferred: selective longs, reduce leverage, trim weakest performers

- **8–11 points — Neutral / Defensive**
    - Target equity exposure: **10–40%**
    - Hedge: long Gold, long VIX exposure, increase cash
    - Preferred: defensive sectors, high-quality dividend names

- **12+ points — Strong Risk-Off**
    - Target equity exposure: **0–10%** or net short
    - Hedge: long VIX/volatility products, long Gold, short indices (bear puts, inverse ETFs)
    - Preferred: capitalize on protective trades and volatility spreads

---

### **2. Equity Selection: S&P 500 Stocks**

#### **Goal:**
Find high-beta, high-growth stocks aligned with macro regime.

#### **Yahoo Finance Metrics (or calculated substitutes):**
- **Beta (5Y Monthly)**: > 1.3 (or calculate Beta via returns if unavailable)
- **Revenue Growth (YoY)**: > 15%
- **PEG Ratio**: < 1.5 (if missing, use fallback rules — see later). Including RSI thresholds: 30 and 70
- **Price above 50D & 200D MA, e.g. Golden Cross** for momentum longs
- **Outperformance vs. SPY over 3 months** (relative strength)
- **RSI**: 55–70 preferred for continuation candidates - NOT USED.

#### **In Risk-Off Regime:**
- Favor defensive sectors (e.g., Utilities, Consumer Staples)
- Look for mean-reversion shorts or breakdown setups in high-beta names

---

### **3. Hedging with Options**

| Strategy | Use Case | Description |
|---------|----------|-------------|
| Protective Put | Long in neutral regime | Long stock + buy put to cap downside |
| Collar | Long with risk | Long stock + sell OTM call + buy OTM put |
| Call Spread | Directional long | Buy call + sell higher strike call |
| Bear Put Spread | Directional short | Buy put + sell lower strike put |

**Volatility Cluster Rule (optional):** if **2 or more** of {VIX, VVIX, SKEW, SDEX} are in risk-off, increase hedge size by +1 notch.

---

### **4. Currency Pair Insights**

| Pair | Meaning | Risk-Off Behavior |
|------|---------|-------------------|
| USD/JPY | Safe-haven flow | USD rising = risk-off |
| AUD/USD | Growth sentiment | AUD falling = risk-off |
| DXY | USD strength | Rising = global stress |
| EUR/USD | Broad macro signal | Falling = Eurozone fragility, USD strength |

Use FX trends to confirm or lead equity and commodity signals.

---

### **5. Missing Data / Fallbacks**
- **PEG missing**: fallback to revenue growth (>25%) and forward PE < sector average, OR flag as "speculative" and give lower weight.
- **Split-adjustment**: always use adjusted prices (or back-adjust series) before computing returns/indicators.
- **Series alignment**: align time series (by timestamp intersection) when comparing assets (use earlier `BarSeriesAligner`).

---

### **Summary**
This strategy offers a professional-grade, layered approach to market exposure:
1. **Top-down signal index** to detect regime
2. **Bottom-up stock selection** aligned with regime
3. **Hedging via options** for risk-adjusted exposure
4. **Global insight via currency pairs and macro indicators** to improve timing and signal quality

The framework is adaptable, scalable, and ideal for building an alpha-driven, risk-aware trading system.
