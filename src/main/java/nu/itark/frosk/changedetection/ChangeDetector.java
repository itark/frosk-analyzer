package nu.itark.frosk.changedetection;

/**
 * @author Will Faithfull
 */
public interface ChangeDetector<T> {

    /**
     * Provide the next item in the stream
     * @param observation the next item in the stream
     */
    void update(T observation);

    /**
     * Did the detector signal change at the last item?
     * @return true, if it did.
     */
//    boolean isChange();


    boolean isChangeHigh();


    boolean isChangeLow();


    /**
     * Has the detector seen enough items to detect change?
     * @return true, if it has.
     */
    boolean isReady();

    /**
     * Reset the detector, wiping any memory component it retains.
     */
    void reset();

//    /**
//     * Reset the detector for high, wiping any memory component it retains.
//     */
//    void resetHigh();
//
//    /**
//     * Reset the detector for high, wiping any memory component it retains.
//     */
//    void resetLow();


//    /**
//     * Return current cusum
//     * @return
//     */
//    Double cusum();


    /**
     * Return current cusumhigh
     * @return
     */
    Double cusumHigh();

    /**
     * Return current cusumlow
     * @return
     */
    Double cusumLow();




}
