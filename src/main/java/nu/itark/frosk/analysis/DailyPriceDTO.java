package nu.itark.frosk.analysis;

import java.time.format.DateTimeFormatter;


import lombok.Data;
import org.ta4j.core.Bar;

@Data
public class DailyPriceDTO {

//	private long time;
	private String time;
	private double open;
	private double high;
	private double low;
	private double close;
	private long value;  //volume, for TradingView Histogram
	private String trade;


	public DailyPriceDTO(Bar bar) {
		//this.setTime(bar.getEndTime().toInstant().toEpochMilli());
		this.setTime(bar.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
		this.setOpen(bar.getOpenPrice().doubleValue());
		this.setHigh(bar.getHighPrice().doubleValue());
		this.setLow(bar.getLowPrice().doubleValue());
		this.setClose(bar.getClosePrice().doubleValue());
		this.setValue(bar.getVolume().longValue());
	}

}
