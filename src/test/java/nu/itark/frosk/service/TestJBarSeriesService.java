package nu.itark.frosk.service;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.dataset.Database;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TestJBarSeriesService extends BaseIntegrationTest {

	@Autowired
	private BarSeriesService barSeriesService;

	
	@Test
	public void testSecurityId() throws Exception {
		Long sec_id = barSeriesService.getSecurityId("BTC-EUR");
		assertNotNull(sec_id);
		
	}	
	
	@Test
	public void testGetDataSet() throws Exception {
		List<BarSeries> sec = barSeriesService.getDataSet(Database.COINBASE);
		assertNotNull(sec);
		System.out.println("size="+sec.size());

		BarSeries barSeries = sec.stream()
				.filter(s -> s.getName().equals("BTC-EUR"))
				.findFirst()
				.get();
		assertNotNull(barSeries);

	}

	
	@Test
	public void testGetDataSetSecurity() throws Exception {
		Long sec_id = barSeriesService.getSecurityId("BTC-EUR");
		System.out.println("sec_id="+sec_id);
		BarSeries sec = barSeriesService.getDataSet(sec_id );
		assertNotNull(sec);
		System.out.println("size="+sec.getBarCount());
		
	}	
	
	@Test
	public void testGetCandlesFromCoinbase() {

		BarSeries timeSeries = barSeriesService.getDataSetFromCoinbase("NCT-EUR");
		ZonedDateTime lastEndTime = timeSeries.getLastBar().getEndTime();

		Bar lastbar = timeSeries.getLastBar();


		System.out.println("kalle");

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


	@Test
	public void testBigDecimal(){

		BigDecimal big = new BigDecimal(0.123456789);
		System.out.println("big="+big);
		BigDecimal big2 = big.round(MathContext.DECIMAL32);
		System.out.println("big2="+big2);
		BigDecimal big3 =big.round(new MathContext(2, RoundingMode.HALF_EVEN));
		System.out.println("big3="+big3);
		BigDecimal big4 = big.setScale(2, RoundingMode.HALF_EVEN);
		System.out.println("big4="+big4);



	}
	
}
