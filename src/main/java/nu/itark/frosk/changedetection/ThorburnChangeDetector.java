package nu.itark.frosk.changedetection;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Fredrik Möller
 *
 * The Cumulative Sum (CUSUM) algorithm has two parameters:
 *
 *      δ (delta)   : The magnitude of acceptable change in standard deviations
 *      λ (lambda) : The threshold in standard deviations
 */
@Slf4j
public class ThorburnChangeDetector implements ChangeDetector<Double> {

    private final static double DEFAULT_MAGNITUDE = 0.5; //0.05  https://www.spcforexcel.com/knowledge/variable-control-charts/keeping-process-target-cusum-charts
    private final static double DEFAULT_THRESHOLD = 3; //https://github.com/O5ten/halp/blob/master/src/main/java/com/osten/halp/impl/profiling/detector/Cusum.java
    private final static long DEFAULT_READY_AFTER = 50;  //50

    private double cusumPrev_upward = 0;
    private double cusumPrev_downward = 0;
    private double cusum_upward;
    private double cusum_downward;
    private double magnitude;
    private double threshold_upward;
    private double threshold_downward;
    private double magnitudeMultiplier;
    private double thresholdMultiplier;
    private long   readyAfter;

    long   observationCount = 0;
    double runningMean      = 0;
    double runningVariance = 0;

    boolean change_upward = false;
    boolean change_downward = false;
    double init_loi  = 0; //true value

    
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

        threshold_upward = thresholdMultiplier * std;
        threshold_downward = thresholdMultiplier * std;

//        cusum_high = Math.max(0, cusumPrev_high +(loi + runningMean - magnitude));
//        cusum_low = Math.max(0, cusumPrev_low +(loi - runningMean - magnitude));
//        cusum_high = Math.max(0, cusumPrev_high +(loi - runningMean ));
//        cusum_low = Math.max(0, cusumPrev_low - (loi - runningMean ));

        cusum_upward = Math.max(0, cusumPrev_upward + loi - init_loi  );
        cusum_downward = Math.min(0, cusumPrev_downward +  loi - init_loi );

//        positiveCusum = Math.max( 0, positiveCusum + drift + residual );
//        negativeCusum = Math.max( 0, Math.abs( negativeCusum + drift - residual ) );/* Abs mig


        // https://en.wikipedia.org/wiki/CUSUM

        //Thorbunr sid 17

        log.info(" cusum_upward  {},cusum_downward:{}", cusum_upward , cusum_downward);
//        log.info("std {},loi:{}",std, loi);
      //  log.info("init_loi:{}, loi:{}, cusum_high:{}, cusum_low:{}, threshold_high:{}", init_loi, loi, cusum_high, cusum_low, threshold_high);

        if(isReady()) {
            this.change_upward = cusum_upward > threshold_upward;
            this.change_downward = cusum_downward < threshold_upward;

//            if (this.change_high) {
//                log.info("CHANGE HIGH:loi:{}, cusum_high:{}, cusum_low:{}, threshold {}", loi, cusum_high, cusum_low, threshold_high);
//            }
//
//            if (this.change_low) {
//                log.info("CHANGE LOW:loi:{}, cusum_high:{}, cusum_low:{}, threshold {}", loi, cusum_high, cusum_low, threshold_high);
//            }
        }

        cusumPrev_upward = cusum_upward;
        cusumPrev_downward = cusum_downward;

    }


    @Override
    public boolean isChangeHigh() {
        return change_upward;
    }

    @Override
    public boolean isChangeLow() {
        return change_downward;
    }


    @Override
    public boolean isReady() {
        return this.observationCount >= readyAfter;
    }

    public void reset() {
        this.cusum_upward = 0;
        this.cusumPrev_upward = 0;
        this.cusum_downward = 0;
        this.cusumPrev_downward = 0;
        this.runningMean = 0;
        this.runningVariance = 0;
        this.observationCount = 0;
        this.threshold_upward = 0;
        this.init_loi = 0;
    }

    @Override
    public Double cusumHigh() {
        return cusum_upward;
    }


    @Override
    public Double cusumLow() {
        return cusum_downward;
    }


}
