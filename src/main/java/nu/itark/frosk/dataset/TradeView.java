package nu.itark.frosk.dataset;

import java.time.LocalDate;

import org.ta4j.core.Trade;

public class TradeView extends Trade {

	private static final long serialVersionUID = 1L;
	
	private LocalDate date;
	private String type;

	public String getDate() {
		return date.toString();
	}
	public void setDate(LocalDate date) {
		this.date = date;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	
	
}
