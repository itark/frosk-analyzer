package nu.itark.frosk.changedetection;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Fredrik Möller
 *
 * The Cumulative Sum (CUSUM) algorithm has two parameters:
 *
 *      δ (delta)   : The magnitude of acceptable change in standard deviations
 *      λ (lambda) : The threshold in standard deviations
 *              // https://en.wikipedia.org/wiki/CUSUM
 *         //Thorbunr sid 17
 */
@Slf4j
public class ThorburnChangeDetector implements ChangeDetector<Double> {

    private final static double DEFAULT_MAGNITUDE = 0.5; //0.05  https://www.spcforexcel.com/knowledge/variable-control-charts/keeping-process-target-cusum-charts
    private final static double DEFAULT_THRESHOLD = 3; //https://github.com/O5ten/halp/blob/master/src/main/java/com/osten/halp/impl/profiling/detector/Cusum.java
    private final static long DEFAULT_READY_AFTER = 50;  //50

    private double cusumPrev_high = 0;
    private double cusumPrev_low = 0;
    private double cusum_high;
    private double cusum_low;
    private double magnitude;
    private double threshold_high;
    private double threshold_low;
    private double magnitudeMultiplier;
    private double thresholdMultiplier;
    private long   readyAfter;

    long   observationCount = 0;
    double runningMean      = 0;
    double runningVariance = 0;

    boolean change_high = false;
    boolean change_low = false;
    double init_loi  = 0; //true value, sida 18 Thorburn

    
    /**
     * Create a CUSUM detector
     * @param magnitudeMultiplier Magnitude of acceptable change in stddevs
     * @param thresholdMultiplier Threshold in stddevs
     * @param readyAfter Number of observations before allowing change to be signalled
     */
    public ThorburnChangeDetector(double magnitudeMultiplier,
                               double thresholdMultiplier,
                               long readyAfter) {
        this.magnitudeMultiplier = magnitudeMultiplier;
        this.thresholdMultiplier = thresholdMultiplier;
        this.readyAfter = readyAfter;
    }

    public ThorburnChangeDetector() {
        this(DEFAULT_MAGNITUDE, DEFAULT_THRESHOLD, DEFAULT_READY_AFTER);
        
    }

    @Override
    public void update(Double loi) {
    	++observationCount;

    	if (init_loi == 0) {
    	    init_loi = loi;
        }

        // Instead of providing the target mean as a parameter as
        // we would in an offline test, we calculate it as we go to
        // create a target of normality.
        double newMean = runningMean + (loi - runningMean) / observationCount;
        runningMean = newMean;
        runningVariance += (loi - runningMean)*(loi - newMean);
        double std = Math.sqrt(runningVariance);

        threshold_high = thresholdMultiplier * std;
        threshold_low = thresholdMultiplier * std;

        cusum_high = Math.max(0, cusumPrev_high + loi - init_loi  );
        cusum_low = Math.max(0, Math.abs(cusumPrev_low -  loi - init_loi ));


        log.info("loi:{}, init_loi:{},std:{} cusum_high:{}, cusum_low:{}, threshold_high {}", loi,init_loi , std, cusum_high, cusum_low, threshold_high);


        if(isReady()) {
            change_high = cusum_high > threshold_high;
            change_low = cusum_low > threshold_low;

           // log.info("loi:{}, std:{} cusum_high:{}, cusum_low:{}, threshold_high {}", loi,std, cusum_high, cusum_low, threshold_high);

            if (change_high) {
                log.info("HIGH : loi:{}, change_high:{}, change_low:{}, threshold_high {}", loi, change_high, change_low, threshold_high);
                log.info("********** \n");
                reset();
            }

            if (change_low) {
                log.info("LOW : loi:{}, change_high:{}, change_low:{}, threshold_high {}", loi, change_high, change_low, threshold_high);
                log.info("********** \n");
                reset();
            }

        }

        cusumPrev_high = cusum_high;
        cusumPrev_low = cusum_low;

    }

    @Override
    public boolean isChangeHigh() {
        return change_high;
    }

    @Override
    public boolean isChangeLow() {
        return change_low;
    }


    @Override
    public boolean isReady() {
        return this.observationCount >= readyAfter;
    }

    public void reset() {
        this.cusum_high = 0;
        this.cusumPrev_high = 0;
        this.cusum_low = 0;
        this.cusumPrev_low = 0;
        this.runningMean = 0;
        this.runningVariance = 0;
        this.observationCount = 0;
        this.threshold_high = 0;
        this.threshold_low = 0;
        this.init_loi = 0;
    }

    @Override
    public Double cusumHigh() {
        return cusum_high;
    }


    @Override
    public Double cusumLow() {
        return cusum_low;
    }


}
