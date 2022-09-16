package nu.itark.frosk.dataset;

import java.math.BigDecimal;

import org.ta4j.core.Bar;

import lombok.Data;

@Data
public class DailyPrice {

	private long time;
	private long open;
	private long high;
	private long low;
	private long close;
	private long value;  //volume, for TradingView Histogram
	private String trade;

	public DailyPrice(Bar bar) {
		this.setTime(bar.getEndTime().toInstant().toEpochMilli());
		this.setOpen(bar.getOpenPrice().longValue());
		this.setHigh(bar.getHighPrice().longValue());
		this.setLow(bar.getLowPrice().longValue());
		this.setClose(bar.getClosePrice().longValue());
		this.setValue(bar.getVolume().longValue());
	}

}
