package nu.itark.frosk.marknadssok.fi.proxy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.opencsv.CSVReader;

@Service
public class MarknadsSokProxy {
	Logger logger = Logger.getLogger(MarknadsSokProxy.class.getName());	
	//	private String url = "https://marknadssok.fi.se/publiceringsklient/sv-SE/Search/Search?SearchFunctionType=Insyn&Utgivare=&PersonILedandeSt%C3%A4llningNamn=&Transaktionsdatum.From=&Transaktionsdatum.To=&Publiceringsdatum.From=&Publiceringsdatum.To=&button=export&Page=1";
	private String url = "https://marknadssok.fi.se/publiceringsklient/sv-SE/Search/Search?SearchFunctionType=Insyn&Utgivare=&PersonILedandeSt%C3%A4llningNamn=&Transaktionsdatum.From=&Transaktionsdatum.To=&Publiceringsdatum.From=2018-09-01&Publiceringsdatum.To=2018-09-06&button=export&Page=1";	
	static String fileName = "marknadsok.csv";
	
	@Bean 
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
		return restTemplate;
	}	

	@Autowired
	FileHelper fileHelper;
	
	public void downloadFile(Date fromPubliseringsDatum, Date toPubliseringsDatum) {
		try {
			fileHelper.downloadFile(url, "marknadsok.csv");
		} catch (IOException e) {
			logger.info("Error:"+ e);
		}
	
	}	
	
	public List<InsynsTransaktion> getInsynshandel(Date fromPubliseringsDatum, Date toPubliseringsDatum) throws IOException {
		List<InsynsTransaktion> list = new ArrayList<InsynsTransaktion>();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		
		ResponseEntity<byte[]> response = restTemplate().exchange(url, HttpMethod.GET, entity, byte[].class);
		logger.info("File downloaded from:" + url + ", size=" + response.getBody().length);
	
		ByteArrayInputStream bis = new ByteArrayInputStream(response.getBody());
		Reader in = new BufferedReader(new InputStreamReader(bis));

		CSVReader csvReader = new CSVReader(new InputStreamReader(bis, Charset.forName("UTF-8")), ';');

		List<String[]> lines = null;
		lines = csvReader.readAll();
        lines.remove(0); // Removing header line		
		
		lines.forEach(line -> {
			if (line.length > 1) {
				logger.info("line[0]="+line[0]);
				logger.info("line[1]="+line[1]);
				logger.info("line[2]="+line[2]);
				logger.info("line[3]="+line[3]);
				logger.info("line[4]="+line[4]);
				logger.info("line[5]="+line[5]);
				logger.info("line[6]="+line[6]);
				logger.info("line[7]="+line[7]);
				logger.info("line[8]="+line[8]);
				logger.info("line[9]="+line[9]);
				logger.info("line[10]="+line[10]);
				logger.info("line[11]="+line[11]);
				logger.info("line[12]="+line[12]);
				logger.info("line[13]="+line[13]);
				logger.info("line[14]="+line[14]);
				logger.info("line[15]="+line[15]);
				logger.info("line[16]="+line[16]);
				logger.info("line[17]="+line[17]);
				logger.info("line[18]="+line[18]);
				logger.info("line[19]="+line[19]);
				logger.info("line[20]="+line[20]);
				logger.info("line[21]="+line[21]);
			}
			
		});
		
		
		csvReader.close();
		in.close();	
		
		return list;
		
	}	
	
	
	
	public List<InsynsTransaktion> get(Date fromPubliseringsDatum, Date toPubliseringsDatum) {
		List<InsynsTransaktion> transaktioner = new ArrayList<InsynsTransaktion>();
		
		return transaktioner;
	}
	
}
