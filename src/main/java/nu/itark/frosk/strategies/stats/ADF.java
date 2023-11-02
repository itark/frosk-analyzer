package nu.itark.frosk.strategies.stats;

import java.util.Queue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.MarketDataProxy;
import org.surus.math.AugmentedDickeyFuller;

@Service
@Slf4j
public class ADF {
	static int DEFAULT_QUEUE_SIZE = 100;
	Queue<Double> xQueue = new CircularFifoQueue<Double>(DEFAULT_QUEUE_SIZE);	
	AugmentedDickeyFuller adf;


	
	@Autowired	
	MarketDataProxy marketDataProxy;
	
	public double getPValue(Double x) {
		//sync
		synchronizeXQueue(x);
		//run
		run();
	
		if (xQueue.size() > DEFAULT_QUEUE_SIZE) {
//			return adf.pValue();
			return 0;
		} else {
			return 0;
		}
		
	}

	public void synchronizeXQueue(Double x) {
		if (xQueue.isEmpty()) {
			xQueue.add(x);
			return;
		}
		
	}	

	public void run() {
		Double[] xx = xQueue.toArray(new Double[0]);
		double[] xxx = ArrayUtils.toPrimitive(xx);
		adf = new AugmentedDickeyFuller(xxx);

	}
	
	
}
