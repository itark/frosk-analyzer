package nu.itark.frosk.analysis;

import lombok.Data;

/**
 * Holds chart data to UI.
 * 
 * @author fredrikmoller
 *
 */
@Data
public class ChartValueDTO {

	private String date;
	private String open;
	private String high;
	private String low;
	private String close;
	private String volume;
	
	private String value1;
	private String value2;
	private String value3;
	private String value4;
	
	
}
