package nu.itark.frosk.dataset;

import java.util.Calendar;
import java.util.Date;

import org.threeten.bp.LocalDate;

import com.jimmoores.quandl.DataSetRequest;
import com.jimmoores.quandl.SessionOptions;
import com.jimmoores.quandl.SortOrder;
import com.jimmoores.quandl.TabularResult;
import com.jimmoores.quandl.classic.ClassicQuandlSession;

import nu.itark.frosk.model.Security;

public class QuandlSessionHelper {

	final static String API_KEY = "CdaVrPkU7HH3Axd5pfNi";
	/**
	 * Prepare a TabularResult with API-KEY on provided SecurityCode
	 * 
	 * @param securityCode
	 * @return TabularResult
	 */
	public static TabularResult getTabularResult(Security security) {
	ClassicQuandlSession session = ClassicQuandlSession.create(SessionOptions.Builder.withAuthToken(API_KEY).build());

	TimeSeriesManager.getStartDate();
	return session.getDataSet(DataSetRequest.Builder
			.of(security.getName())
			.withStartDate(TimeSeriesManager.getStartDate())
			.withEndDate(TimeSeriesManager.getEndDate())
			.withSortOrder(SortOrder.ASCENDING)
			.build());
	
	}
	
	/**
	 * Prepare a TabularResult WITHOUT API-KEY on provided SecurityCode
	 * 
	 * @param security
	 * @param fromDate 
	 * @return TabularResult
	 */
	public static TabularResult getTabularResultWithoutApiKey(Security security, Date fromDate) {
		LocalDate startDate;
		if (fromDate == null) {
			startDate = TimeSeriesManager.getStartDate();
		} else {
			startDate = DateManager.getThreetenLocalDate(fromDate);
		}

		ClassicQuandlSession session = ClassicQuandlSession.create();	
		return session.getDataSet(DataSetRequest.Builder
			.of(security.getName())
			.withStartDate(startDate.plusDays(1))
			.withEndDate(LocalDate.now())
			.withSortOrder(SortOrder.ASCENDING)
			.build());
	
	}	
	
}
