package nu.itark.frosk.marknadssok.fi.proxy;

import java.util.Date;

import lombok.Data;

/**
 * This class represent the event of insynstransaktion.
 * 
 * @author fredrikmoller
 *
 */
@Data
public class InsynsTransaktion {

	private Date publiseringsDatum;
	private String utgivare;
	private String person;
	private String befattning;
	private String narstaende;
	private String karaktar;
	private String instrument;
	private String isin;
	private Date transaktionsDatum;
	private int volym;
	private String volymEnhet;
	private float pris;
	private String valuta;
	private String handelsPlats;
	private String status;

}
