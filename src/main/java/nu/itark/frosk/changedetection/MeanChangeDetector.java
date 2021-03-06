package nu.itark.frosk.changedetection;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Fredrik Möller
 *

 */
@Slf4j
public class MeanChangeDetector implements ChangeDetector<Double> {

    private final static double DEFAULT_MAGNITUDE = 0.05;
    private final static double DEFAULT_THRESHOLD = 3;
    private final static long DEFAULT_READY_AFTER = 50;  //50

    private double cusumPrev = 0;
    private double cusum;
    private double magnitude;
    private double threshold;
    private double magnitudeMultiplier;
    private double thresholdMultiplier;
    private long   readyAfter;

    long   observationCount = 0;
    double runningMean      = 0.0;
    double runningVariance = 0.0;

    boolean change = false;
    
    /**
     * Create a CUSUM detector
     * @param magnitudeMultiplier Magnitude of acceptable change in stddevs
     * @param thresholdMultiplier Threshold in stddevs
     * @param readyAfter Number of observations before allowing change to be signalled
     */
    public MeanChangeDetector(double magnitudeMultiplier,
                               double thresholdMultiplier,
                               long readyAfter) {
        this.magnitudeMultiplier = magnitudeMultiplier;
        this.thresholdMultiplier = thresholdMultiplier;
        this.readyAfter = readyAfter;
    }

    public MeanChangeDetector() {
        this(DEFAULT_MAGNITUDE, DEFAULT_THRESHOLD, DEFAULT_READY_AFTER);
        
    }

    @Override
    public void update(Double xi) {
    	++observationCount;

        // Instead of providing the target mean as a parameter as
        // we would in an offline test, we calculate it as we go to
        // create a target of normality.
        double newMean = runningMean + (xi - runningMean) / observationCount;
//        runningMean = newMean;
//        runningVariance += (xi - runningMean)*(xi - newMean);
//        double std = Math.sqrt(runningVariance);
//
//        magnitude = magnitudeMultiplier * std;
//        threshold = thresholdMultiplier * std;
//
//        cusum = Math.max(0, cusumPrev +(xi - runningMean - magnitude));
        
        if(isReady()) {
            this.change = newMean > xi;
        }

        cusumPrev = cusum;

    }

    @Override
    public boolean isChangeHigh() {
        return false;
    }

    @Override
    public boolean isChangeLow() {
        return false;
    }

//    @Override
//    public boolean isChange() {
//        return change;
//    }

    @Override
    public boolean isReady() {
        return this.observationCount >= readyAfter;
    }

    @Override
    public Double cusumHigh() {
        return null;
    }

    @Override
    public Double cusumLow() {
        return null;
    }

    public void reset() {
        this.cusum = 0;
        this.cusumPrev = 0;
        this.runningMean = 0;
        this.observationCount = 0;
    }

//    @Override
//    public Double cusum() {
//        return cusum;
//    }

}
