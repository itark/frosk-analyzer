package nu.itark.frosk.changedetection;

import java.io.IOException;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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

    private final static double DEFAULT_MAGNITUDE = 0.05;
    private final static double DEFAULT_THRESHOLD = 3;
    private final static long DEFAULT_READY_AFTER = 2;  //50

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
    
	WebSocketSession webSocketsession;

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

	public void setWebSocketsession(WebSocketSession webSocketsession) {
		this.webSocketsession = webSocketsession;
	}   
    
    
    
    @Override
    public void update(Double xi) {
        StringBuilder sb = new StringBuilder();
    	++observationCount;

        // Instead of providing the target mean as a parameter as
        // we would in an offline test, we calculate it as we go to
        // create a target of normality.
        double newMean = runningMean + (xi - runningMean) / observationCount;
        runningMean = newMean;
        runningVariance += (xi - runningMean)*(xi - newMean);
        double std = Math.sqrt(runningVariance);

        magnitude = magnitudeMultiplier * std;
        threshold = thresholdMultiplier * std;

        cusum = Math.max(0, cusumPrev +(xi - runningMean - magnitude));
        
        
        
//        log.info("xi {}",xi);   
//        log.info("cusum {}",cusum);
// 
        if(isReady()) {
            this.change = cusum > threshold;
//        	log.info("isReady...change {}, {}",change, cusum);
        }

        cusumPrev = cusum;
        

        sb.append(cusum);
        
        
        if (webSocketsession != null) {
            sendMessage(sb.toString());
        }
    }
   
	protected void sendMessage(String cusum)  {
		// log.info("msg="+ cusum);
		try {
			// webSocketsession.sendMessage(new TextMessage(msg));
			webSocketsession.sendMessage(new TextMessage(String.format("{\"type\":\"cusum\",\"value\":\"%s\"}", cusum)));

        } catch (IOException e) {
			log.error("Could not send message", e);
			
		}
	}
    
    

    @Override
    public boolean isChange() {
        return change;
    }

    @Override
    public boolean isReady() {
        return this.observationCount >= readyAfter;
    }

    public void reset() {
        this.cusum = 0;
        this.cusumPrev = 0;
        this.runningMean = 0;
        this.observationCount = 0;
    }

}
