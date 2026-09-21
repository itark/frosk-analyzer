package nu.itark.frosk.service;

/**
 * Three-state crypto market regime — BTC daily SMA (direction) crossed with
 * ADX (trend strength) — computed by {@link CryptoRegimeService#getRegime}.
 *
 * <ul>
 *   <li>{@link #RANGING} — ADX below the trend threshold, regardless of which
 *       side of its SMA BTC is on. Reversion strategies (VWAP, Liquidity Sweep)
 *       belong here: no durable trend to fight.</li>
 *   <li>{@link #TRENDING_UP} — BTC above its SMA and ADX at/above the trend
 *       threshold. Momentum-long territory.</li>
 *   <li>{@link #TRENDING_DOWN} — BTC below its SMA and ADX at/above the trend
 *       threshold. Momentum-short territory.</li>
 * </ul>
 *
 * <p>Independent of the older binary {@code isRiskOn}/{@code CryptoRegimeRule}
 * gate (SMA-only, no ADX leg) — that gate is unchanged and this enum does not
 * replace it. {@code RANGING} is also the fail-closed default when there is no
 * BTC data: it grants neither momentum direction, only reversion eligibility.
 */
public enum CryptoMarketRegime {
    RANGING,
    TRENDING_UP,
    TRENDING_DOWN
}
