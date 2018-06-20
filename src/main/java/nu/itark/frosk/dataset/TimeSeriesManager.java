package nu.itark.frosk.dataset;

//import java.time.DayOfWeek;
//import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
//import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoUnit;

import nu.itark.frosk.repo.SecurityPriceRepository;

public class TimeSeriesManager { 
	Logger logger = Logger.getLogger(TimeSeriesManager.class.getName());
	final static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d-MMM-yy", Locale.ENGLISH);
	final private static int startYear = 2017;
	
	@Autowired
	SecurityPriceRepository securityRepository;	
	

	
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
