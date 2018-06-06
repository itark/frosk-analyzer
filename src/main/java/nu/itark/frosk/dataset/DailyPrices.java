package nu.itark.frosk.dataset;

import org.ta4j.core.Bar;

public class DailyPrices {
//	private static Logger logger = LogManager.getLogger(DailyPrices.class);
	// DateFormat df = new SimpleDateFormat("d-MMM-yy",Locale.ENGLISH); //ex.
	// "16-Jun-16"
	private String date;
	private String open;
	private String high;
	private String low;
	private String close;
	private String volume;

	private double openD;
	private double highD;
	private double lowD;
	private double closeD;
	private double volumeD;

	public DailyPrices(Bar bar) {
		this.setDate(bar.getEndTime().toLocalDate().toString());
		this.setOpen(bar.getOpenPrice().toString());
		this.setHigh(bar.getMaxPrice().toString());
		this.setLow(bar.getMinPrice().toString());
		this.setClose(bar.getClosePrice().toString());
		this.setVolume(bar.getVolume().toString());

		this.setOpenD(bar.getOpenPrice().toDouble());
		this.setHighD(bar.getMaxPrice().toDouble());
		this.setLowD(bar.getMinPrice().toDouble());
		this.setCloseD(bar.getClosePrice().toDouble());
		this.setVolumeD(bar.getVolume().toDouble());

	}

	public double getOpenD() {
		return openD;
	}

	public void setOpenD(double openD) {
		this.openD = openD;
	}

	public double getHighD() {
		return highD;
	}

	public void setHighD(double highD) {
		this.highD = highD;
	}

	public double getLowD() {
		return lowD;
	}

	public void setLowD(double lowD) {
		this.lowD = lowD;
	}

	public double getCloseD() {
		return closeD;
	}

	public void setCloseD(double closeD) {
		this.closeD = closeD;
	}

	public double getVolumeD() {
		return volumeD;
	}

	public void setVolumeD(double volumeD) {
		this.volumeD = volumeD;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getOpen() {
		return open;
	}

	public void setOpen(String open) {
		this.open = open;
	}

	public String getHigh() {
		return high;
	}

	public void setHigh(String high) {
		this.high = high;
	}

	public String getLow() {
		return low;
	}

	public void setLow(String low) {
		this.low = low;
	}

	public String getClose() {
		return close;
	}

	public void setClose(String close) {
		this.close = close;
	}

	public String getVolume() {
		return volume;
	}

	public void setVolume(String volume) {
		this.volume = volume;
	}

//	@Override
//	public String toString() {
//		return ReflectionToStringBuilder.toString(this);
//	}

}
