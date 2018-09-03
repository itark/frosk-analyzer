package nu.itark.frosk.dataset;

import java.math.BigDecimal;

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
		this.setOpen(bar.getOpenPrice().getDelegate().setScale(2,BigDecimal.ROUND_UP).toString());
		this.setOpen(bar.getOpenPrice().getDelegate().setScale(2,BigDecimal.ROUND_UP).toString());
		this.setHigh(bar.getMaxPrice().getDelegate().setScale(2,BigDecimal.ROUND_UP).toString());
		this.setLow(bar.getMinPrice().getDelegate().setScale(2,BigDecimal.ROUND_UP).toString());
		this.setClose(bar.getClosePrice().getDelegate().setScale(2,BigDecimal.ROUND_UP).toString());
		this.setVolume(bar.getVolume().getDelegate().setScale(2,BigDecimal.ROUND_UP).toString());

	}

}
