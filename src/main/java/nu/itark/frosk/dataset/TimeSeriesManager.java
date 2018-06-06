package nu.itark.frosk.dataset;

//import java.time.DayOfWeek;
//import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
//import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoUnit;

import nu.itark.frosk.repo.SecurityPriceRepository;

public class TimeSeriesManager { 
	Logger logger = Logger.getLogger(TimeSeriesManager.class.getName());
	final static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d-MMM-yy", Locale.ENGLISH);
	final private static int startYear = 2017;
	final static String API_KEY = "CdaVrPkU7HH3Axd5pfNi";

	@Value("${frosk.download.years}")
	public static int yearsToDownload;
	
	@Autowired
	SecurityPriceRepository securityRepository;	
	


//	public List<TimeSeries> getDataSet() {
//		Iterable<SecurityPrice> spList = securityRepository.findAll();
//		List<TimeSeries> timeSeries = new ArrayList<TimeSeries>();
//		
//		spList.forEach(sp -> {
//			timeSeries.add(getDataSet(sp.getName()));
//		});
//		
//		return timeSeries;
//		
//	}	
	
	
//	public TimeSeries getDataSet(String name) {
//		List<Bar> bars = new ArrayList<>();
//		List<SecurityPrice> securityPrices =securityRepository.findByName(name);
//		
//		securityPrices.forEach(row -> {
//			ZonedDateTime dateTime = ZonedDateTime.ofInstant(row.getTimestamp().toInstant(),ZoneId.systemDefault());		
//			
//			Bar bar = new BaseBar(dateTime, row.getOpen().toString(), row.getHigh().toString(), row.getLow().toString(), row.getClose().toString(), row.getVolume().toString());
//			
//			bars.add(bar);
//			
//		});
//		
//		return new BaseTimeSeries(name, bars);
//		
//		
//	}


	
	/**
	 * Calculate number of weekdays
	 * 
	 * @return number of weekdays using startDate to now()
	 */
	public static long getNumberOfWeekDays() {
		return calcWeekDays(getStartDate(), getEndDate());
	}	
	
	public static long calcWeekDays(final LocalDate start, final LocalDate end) {
	    final DayOfWeek startWeekDay = start.getDayOfWeek().getValue() < 6 ? start.getDayOfWeek() : DayOfWeek.MONDAY;
	    final DayOfWeek endWeekDay = end.getDayOfWeek().getValue() < 6 ? end.getDayOfWeek() : DayOfWeek.FRIDAY;

	    final long nrOfWeeks = ChronoUnit.DAYS.between(start, end) / 7;
	    final long totalWeekdDays = nrOfWeeks * 5 + Math.floorMod(endWeekDay.getValue() - startWeekDay.getValue(), 5);

	    return totalWeekdDays;
	}
	
	
	public static LocalDate getStartDate() {
//		return LocalDate.of(yearsToDownload, 1, 1);
		return LocalDate.of(startYear, 1, 1);
	}

	public static LocalDate getEndDate() {
		return  LocalDate.now();
	}

	
}
