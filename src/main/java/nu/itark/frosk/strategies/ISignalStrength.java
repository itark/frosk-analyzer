package nu.itark.frosk.strategies;

/**
 * Opt-in interface for a strategy that can score how strongly a given bar's
 * entry conditions were met, beyond the plain pass/fail of the entry rule.
 *
 * <p>Only implement this once {@code buildStrategy(BarSeries)} has been called
 * on this instance for the series in question — the indicators it reads are
 * built there. Call after checking {@code shouldEnter}, not instead of it:
 * this never gates entry, it only scores an entry that has already fired.
 */
public interface ISignalStrength {
    SignalStrength getSignalStrength(int index);
}
