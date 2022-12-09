package nu.itark.frosk.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeManager {

//	static String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	static ZonedDateTime endZdt = ZonedDateTime.now(ZoneId.systemDefault());
//	static String end = DateTimeFormatter.ofPattern(PATTERN).format(endZdt);

	/**
	 * now, from ZoneId.systemDefault.
	 * 
	 * @return
	 */
	public static String end() {
		return DateTimeFormatter.ISO_INSTANT.format(endZdt);
	}

	/**
	 * Doing minus from now.
	 * 
	 * @param days , minus from now
	 * @return
	 */
	public static String start(int days) {
		ZonedDateTime startZdt = endZdt.minusDays(days);
		return DateTimeFormatter.ISO_INSTANT.format(startZdt);
	}

	public static Date get(ZonedDateTime zdt) {
		return Date.from(zdt.toInstant());
	}

}
