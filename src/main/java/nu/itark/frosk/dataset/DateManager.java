package nu.itark.frosk.dataset;

import java.util.Date;


/**
 * do the sht threeten to/from Java 8...
 * 
 * @author fredrikmoller
 *
 */
public class DateManager {

	
	public static org.threeten.bp.LocalDate getThreetenLocalDate(Date utilDate) {
		
//		LocalDate date310 = row.getLocalDate("Date");
//		String utilDateString = utilDate + " 00:00:00.0";
		String utilDateString = utilDate.toString();
		org.threeten.bp.format.DateTimeFormatter formatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
//		ZonedDateTime dateTime = ZonedDateTime.parse(utilDateString, formatter.withZone(org.threeten.bp.ZoneId.systemDefault()));
		org.threeten.bp.LocalDate dateTime2 = org.threeten.bp.LocalDate.parse(utilDateString, formatter.withZone(org.threeten.bp.ZoneId.systemDefault()));

		return dateTime2;
		

	}
	
	
	
	
}
