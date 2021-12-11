package nu.itark.frosk.dataset;

import lombok.Data;
import org.ta4j.core.Bar;

@Data
public class IndicatorValue {
	private long date;
	private long value;
	private String name;

	public IndicatorValue(long date, long value, String name) {
		this.date = date;
		this.value = value;
		this.name = name;
	}

}
