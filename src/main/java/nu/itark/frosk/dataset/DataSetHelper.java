package nu.itark.frosk.dataset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

import lombok.SneakyThrows;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.SecurityRepository;

@Service
public class DataSetHelper {
	Logger logger = Logger.getLogger(DataSetHelper.class.getName());
	Map<String, String> datasets = new HashMap<String, String>();

	@Autowired
	SecurityRepository securityRepository;

	@PostConstruct
	public void post_construct() {
		datasets.put("YAHOO", "YAHOO-datasets-codes-manual.csv");
//		datasets.put("WIKI", "WIKI-datasets-codes-manual.csv"); 
//		datasets.put("GDAX", "GDAX-datasets-codes-manual.csv"); 
//		datasets.put("BITFINEX", "BITFINEX-datasets-codes-manual.csv"); 
		
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
		Resource file = new ClassPathResource(csvFile);
		Reader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(file.getInputStream()));
		} catch (IOException e) {
			logger.severe("Could not read file, file="+file);
		}		
		
		CSVReader csvReader = new CSVReader(in, ',', '"', 1);
		String[] line;
		while ((line = csvReader.readNext()) != null) {
			String name = line[0];
			String description = line[1];
			Security security = new Security(name, description, database);

			if (securityRepository.existsByName(security.getName())) {
				logger.info("security="+security.getName()+ " exist in database.");
			} else {
				securityRepository.save(security);
				logger.info("Saved security="+security.getName()+ " to database.");
			}

		}
		csvReader.close();

	}

}
