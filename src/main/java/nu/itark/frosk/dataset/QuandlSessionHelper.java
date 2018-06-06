package nu.itark.frosk.dataset;

import com.jimmoores.quandl.DataSetRequest;
import com.jimmoores.quandl.SessionOptions;
import com.jimmoores.quandl.SortOrder;
import com.jimmoores.quandl.TabularResult;
import com.jimmoores.quandl.classic.ClassicQuandlSession;

public class QuandlSessionHelper {

	final static String API_KEY = "CdaVrPkU7HH3Axd5pfNi";
	/**
	 * Prepare a TabularResult with API-KEY on provided SecurityCode
	 * 
	 * @param securityCode
	 * @return TabularResult
	 */
	public static TabularResult getTabularResult(SecurityCode securityCode) {
	ClassicQuandlSession session = ClassicQuandlSession.create(SessionOptions.Builder.withAuthToken(API_KEY).build());

	TimeSeriesManager.getStartDate();
	return session.getDataSet(DataSetRequest.Builder
			.of(securityCode.getCode())
			.withStartDate(TimeSeriesManager.getStartDate())
			.withEndDate(TimeSeriesManager.getEndDate())
			.withSortOrder(SortOrder.ASCENDING)
			.build());
	
	}
	
	/**
	 * Prepare a TabularResult WITHOUT API-KEY on provided SecurityCode
	 * 
	 * @param securityCode
	 * @return TabularResult
	 */
	public static TabularResult getTabularResultWithoutApiKey(SecurityCode securityCode) {
		ClassicQuandlSession session = ClassicQuandlSession.create();	return session.getDataSet(DataSetRequest.Builder
			.of(securityCode.getCode())
			.withStartDate(TimeSeriesManager.getStartDate())
			.withEndDate(TimeSeriesManager.getEndDate())
			.withSortOrder(SortOrder.ASCENDING)
			.build());
	
	}	
	
}
