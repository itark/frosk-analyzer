package nu.itark.frosk.dataset;

import java.math.BigDecimal;

import org.ta4j.core.Bar;

import lombok.Data;

@Data
public class DailyPrice {

	private long date;
	private long open;
	private long high;
	private long low;
	private long value;
	private long volume;
	private String trade;

	public DailyPrice(Bar bar) {
		this.setDate(bar.getEndTime().toInstant().toEpochMilli());
		this.setOpen(bar.getOpenPrice().longValue());
		this.setHigh(bar.getMaxPrice().longValue());
		this.setLow(bar.getMinPrice().longValue());
		this.setValue(bar.getClosePrice().longValue());
		this.setVolume(bar.getVolume().longValue());
	}

}
