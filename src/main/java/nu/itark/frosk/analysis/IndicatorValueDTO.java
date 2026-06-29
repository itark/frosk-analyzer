package nu.itark.frosk.analysis;

import lombok.Data;
import nu.itark.frosk.util.DateTimeManager;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

@Data
public class IndicatorValueDTO {
	private String time;
	private BigDecimal value;
	private String name;

	public IndicatorValueDTO(Date date, BigDecimal value, String name) {
		this(date, value, name, false);
	}

	public IndicatorValueDTO(Date date, BigDecimal value, String name, boolean intraday) {
		LocalDateTime ldt = date.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();
		// Intraday series keep the time component even at midnight. An intraday
		// 00:00 bar serialized as date-only ('yyyy-MM-dd') is parsed as UTC
		// midnight by the browser (new Date('yyyy-MM-dd')), whereas the other
		// bars ('yyyy-MM-dd HH:mm') parse as local time — the mix produces
		// out-of-order timestamps that crash lightweight-charts ("Value is null").
		// Daily series still collapse midnight to a date-only business day.
		if (!intraday && ldt.getHour() == 0 && ldt.getMinute() == 0) {
			this.setTime(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE));
		} else {
			this.setTime(ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
		}
		this.value = value.setScale(6, RoundingMode.HALF_EVEN);
		this.name = name;
	}

}
