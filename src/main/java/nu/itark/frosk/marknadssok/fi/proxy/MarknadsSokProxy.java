package nu.itark.frosk.marknadssok.fi.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.web.client.RestTemplate;

public class MarknadsSokProxy {
//	private static Logger logger = LogManager.getLogger(MarknadsSokProxy.class);
//	private String url = "https://marknadssok.fi.se/publiceringsklient/sv-SE/Search/Search?SearchFunctionType=Insyn&Utgivare=&PersonILedandeSt%C3%A4llningNamn=&Transaktionsdatum.From=&Transaktionsdatum.To=&Publiceringsdatum.From=&Publiceringsdatum.To=&button=export&Page=1";

	private String url = "https://marknadssok.fi.se/publiceringsklient/sv-SE/Search/Search?SearchFunctionType=Insyn&Utgivare=&PersonILedandeSt%C3%A4llningNamn=&Transaktionsdatum.From=&Transaktionsdatum.To=&Publiceringsdatum.From=2018-09-01&Publiceringsdatum.To=2018-09-06&button=export&Page=1";	
	
	private RestTemplate restTemplate = new RestTemplate();
	private FileHelper fileHelper = new FileHelper(restTemplate);
	
	public void downloadFile(Date fromPubliseringsDatum, Date toPubliseringsDatum) {
		try {
			fileHelper.downloadFile(url, "marknassok.csv");
		} catch (IOException e) {
//			logger.error("Error:", e);
		}
	
	}
	
	public List<InsynsTransaktion> get(Date fromPubliseringsDatum, Date toPubliseringsDatum) {
		List<InsynsTransaktion> transaktioner = new ArrayList<InsynsTransaktion>();
		
		return transaktioner;
	}
	
}
