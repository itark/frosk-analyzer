package nu.itark.frosk.strategies.stats;

import java.util.Queue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.MarketDataProxy;
import org.surus.math.AugmentedDickeyFuller;

@Slf4j
public class ADF {
	static int DEFAULT_QUEUE_SIZE = 100;
	Queue<Double> xQueue = new CircularFifoQueue<Double>(DEFAULT_QUEUE_SIZE);	
	AugmentedDickeyFuller adf;

	public ADF(double[] ts) {
		adf = new AugmentedDickeyFuller(ts);
	}

	//https://www.analyticsvidhya.com/blog/2021/06/statistical-tests-to-check-stationarity-in-time-series-part-1/#Augmented_Dickey-Fuller_Test
	public Boolean isStationary() {
		return !adf.isNeedsDiff();
	}

}
