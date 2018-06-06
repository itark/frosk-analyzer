package nu.itark.frosk.marknadssok.fi.proxy;

import java.util.Date;

/**
 * This class represent the event of insynstransaktion.
 * 
 * @author fredrikmoller
 *
 */
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

	public Date getPubliseringsDatum() {
		return publiseringsDatum;
	}

	public void setPubliseringsDatum(Date publiseringsDatum) {
		this.publiseringsDatum = publiseringsDatum;
	}

	public String getUtgivare() {
		return utgivare;
	}

	public void setUtgivare(String utgivare) {
		this.utgivare = utgivare;
	}

	public String getPerson() {
		return person;
	}

	public void setPerson(String person) {
		this.person = person;
	}

	public String getBefattning() {
		return befattning;
	}

	public void setBefattning(String befattning) {
		this.befattning = befattning;
	}

	public String getNarstaende() {
		return narstaende;
	}

	public void setNarstaende(String narstaende) {
		this.narstaende = narstaende;
	}

	public String getKaraktar() {
		return karaktar;
	}

	public void setKaraktar(String karaktar) {
		this.karaktar = karaktar;
	}

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getIsin() {
		return isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public Date getTransaktionsDatum() {
		return transaktionsDatum;
	}

	public void setTransaktionsDatum(Date transaktionsDatum) {
		this.transaktionsDatum = transaktionsDatum;
	}

	public int getVolym() {
		return volym;
	}

	public void setVolym(int volym) {
		this.volym = volym;
	}

	public String getVolymEnhet() {
		return volymEnhet;
	}

	public void setVolymEnhet(String volymEnhet) {
		this.volymEnhet = volymEnhet;
	}

	public float getPris() {
		return pris;
	}

	public void setPris(float pris) {
		this.pris = pris;
	}

	public String getValuta() {
		return valuta;
	}

	public void setValuta(String valuta) {
		this.valuta = valuta;
	}

	public String getHandelsPlats() {
		return handelsPlats;
	}

	public void setHandelsPlats(String handelsPlats) {
		this.handelsPlats = handelsPlats;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
