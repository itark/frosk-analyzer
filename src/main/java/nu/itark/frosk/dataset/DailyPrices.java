package nu.itark.frosk.dataset;

import org.ta4j.core.Bar;

import lombok.Data;

@Data
public class DailyPrices {

	private String date;
	private String open;
	private String high;
	private String low;
	private String close;
	private String volume;

	public DailyPrices(Bar bar) {
		this.setDate(bar.getEndTime().toLocalDate().toString());
		this.setOpen(bar.getOpenPrice().toString());
		this.setHigh(bar.getMaxPrice().toString());
		this.setLow(bar.getMinPrice().toString());
		this.setClose(bar.getClosePrice().toString());
		this.setVolume(bar.getVolume().toString());

	}

}
