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
		LocalDateTime ldt = date.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();
		this.setTime(ldt.format( DateTimeFormatter.ISO_LOCAL_DATE));
		this.value = value.setScale(2, RoundingMode.HALF_EVEN);
		this.name = name;
	}

}
