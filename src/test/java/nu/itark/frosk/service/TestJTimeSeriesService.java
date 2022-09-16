package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;


//@ExtendWith(SpringExtension.class)
//@Import({IntegrationTestConfiguration.class})
@SpringBootTest
public class TestJTimeSeriesService extends BaseIntegrationTest {

	@Autowired
	private BarSeriesService ts;

	
	@Test
	public void testSecurityId() throws Exception {
		Long sec_id = ts.getSecurityId("SAND.ST");
		assertNotNull(sec_id);
		
	}	
	
	@Test
	public void testGetDataSet() throws Exception {
		List<BarSeries> sec = ts.getDataSet();
		assertNotNull(sec);
		
//		System.out.println("size="+sec.getBarCount());
		
	}		

	
	@Test
	public void testGetDataSetSecurity() throws Exception {
		Long sec_id = ts.getSecurityId("ERIC-B.ST");
		System.out.println("sec_id="+sec_id);
		BarSeries sec = ts.getDataSet(sec_id );
//		assertNotNull(sec);
		
//		System.out.println("size="+sec.getBarCount());
		
	}	
	
	@Test
	public void testGetCandlesFromCoinbase() {

	BarSeries timeSeries = ts.getDataSetFromCoinbase("BTC-EUR");

	System.out.println("barCount="+timeSeries.getBarCount());

		Map<ZonedDateTime, List<Bar>> byYear = timeSeries.getBarData().stream()
				.collect(groupingBy(d -> d.getBeginTime().withMonth(1).withDayOfMonth(1)));

		Double byYearAvg = timeSeries.getBarData().stream()
				.collect(averagingDouble(b -> (b.getOpenPrice().minus(b.getClosePrice()).doubleValue())));

		Num pnlOverTime = DecimalNum.valueOf(0);


		for (Bar bar: timeSeries.getBarData().subList(1, 10) ){
			Num profit = bar.getClosePrice().minus(bar.getOpenPrice());
			Num pnlPercent = profit.dividedBy(bar.getOpenPrice()).multipliedBy(timeSeries.numOf(100));
			pnlOverTime = pnlOverTime.plus(pnlPercent);
		}

		System.out.println(pnlOverTime);


/*
		Map<BlogPostType, Double> averageLikesPerType = posts.stream()
				.collect(groupingBy(BlogPost::getType, averagingInt(BlogPost::getLikes)));

*/

		Map<ZonedDateTime, List<Bar>> byMonth = timeSeries.getBarData().stream()
				.collect(groupingBy(d -> d.getBeginTime().withDayOfMonth(1)));
		Map<ZonedDateTime, List<Bar>> byWeek =  timeSeries.getBarData().stream()
				.collect(groupingBy(d -> d.getBeginTime().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));
	assertNotNull(timeSeries);

	}

	public Num calculate(BarSeries series, Position position) {
		if (position.isClosed()) {
			Num entryPrice = position.getEntry().getValue();
			Num pnl = position.getProfit().dividedBy(entryPrice).multipliedBy(series.numOf(100));
			return pnl;
		} else {
			return series.numOf(0);
		}
	}




	
	@Test
	public void testDatessss(){
		
		long time = 1550664480;  //GMT: Wednesday 20 February 2019 12:08:00 
		
		long m = System.currentTimeMillis();
		
		System.out.println("m="+m);
		
		
		ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1550664480),ZoneId.systemDefault());

		System.out.println("dateTime="+dateTime);

		
		LocalDate date =
			    Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
		
		
		System.out.println("date="+date);
		

		LocalDateTime date2 =
			    LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());		
		
		
		System.out.println("date2="+date2);
		
		LocalDateTime date3 =
			    LocalDateTime.ofInstant(Instant.ofEpochMilli(m), ZoneId.systemDefault());	
		
		System.out.println("date3="+date3);
		
		
		
	}
	
	
	
	
}
