package nu.itark.frosk.dataset;

import org.junit.jupiter.api.Test;
import org.threeten.bp.LocalDate;

import com.jimmoores.quandl.DataSetRequest;
import com.jimmoores.quandl.SessionOptions;
import com.jimmoores.quandl.SortOrder;
import com.jimmoores.quandl.TabularResult;
import com.jimmoores.quandl.classic.ClassicQuandlSession;

public class TestJQuand4j {

	@Test
		public void test() {
	    ClassicQuandlSession session = ClassicQuandlSession.create();
	    TabularResult tabularResult = session.getDataSet(
	        DataSetRequest.Builder.of("WIKI/AAPL").withMaxRows(10).build());
	    System.out.println(tabularResult.toPrettyPrintedString());
	}

	@Test
	public void testWithApiKey() {
	String API_KEY = "CdaVrPkU7HH3Axd5pfNi";
//	ClassicQuandlSession session = ClassicQuandlSession.create(SessionOptions.Builder.withAuthToken(API_KEY).build());
	 ClassicQuandlSession session = ClassicQuandlSession.create();
	TabularResult tabularResult = session.getDataSet(DataSetRequest.Builder
			.of("WIKI/AAPL")
			.withStartDate(LocalDate.of(2017, 1, 1)) //TODO
			.withEndDate(LocalDate.now())
			.withSortOrder(SortOrder.ASCENDING)
			.build());   
    
    
    System.out.println(tabularResult.toPrettyPrintedString());
}

}
