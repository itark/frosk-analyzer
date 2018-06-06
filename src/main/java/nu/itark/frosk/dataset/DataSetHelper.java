package nu.itark.frosk.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.persistence.PostRemove;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

import lombok.SneakyThrows;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.SecurityPriceRepository;
import nu.itark.frosk.repo.SecurityRepository;

@Service
public class DataSetHelper {
	Logger logger = Logger.getLogger(DataSetHelper.class.getName());
	Map<String, String> datasets = new HashMap<String, String>();

	@Autowired
	SecurityRepository securityRepository;

	@Autowired
	SecurityPriceRepository securityPriceRepository;
	
	@PostConstruct
	public void post_construct() {
		datasets.put("YAHOO", "YAHOO-datasets-codes-manual.csv");
		datasets.put("WIKI", "WIKI-datasets-codes-manual.csv");
	}
	/**
	 * Insert all securities from cvsFiles.
	 * 
	 */
	public void insertSecurityFromCvsFile() {
		for (Map.Entry<String, String> entry : datasets.entrySet()) {
			String database = entry.getKey();
			String csvFile = entry.getValue();

			saveToRepo(database, csvFile);

		}
	}
	
	@SneakyThrows
	private void saveToRepo(String database, String csvFile) {
		InputStream stream = SecurityCode.class.getClassLoader().getResourceAsStream(csvFile);
		InputStreamReader isr = new InputStreamReader(stream, Charset.forName("UTF-8"));
		CSVReader csvReader = new CSVReader(isr, ',', '"', 1);
		String[] line;
		while ((line = csvReader.readNext()) != null) {
			String name = line[0];
			String description = line[1];
			Security security = new Security(name, description, database);

			securityRepository.save(security);
			
			logger.info("Saved security="+security.getName()+ " to database.");

		}
		csvReader.close();

	}

}
