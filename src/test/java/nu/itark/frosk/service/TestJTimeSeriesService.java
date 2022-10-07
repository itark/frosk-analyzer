package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskStartupApplicationListener;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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

		BarSeries timeSeries = ts.getDataSetFromCoinbase("NCT-EUR");
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
