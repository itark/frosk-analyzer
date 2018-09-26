package nu.itark.frosk.dataset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

import lombok.SneakyThrows;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.SecurityRepository;

/**
 * This class inserts securities and connect them to dataset.
 * Convention: <database>-<dataset>.csv
 * Note: {@linkplain DataSet} must exist in database.
 * 
 * @author fredrikmoller
 *
 */
@Service
public class DataSetHelper {
	Logger logger = Logger.getLogger(DataSetHelper.class.getName());
	/**
	 * dataset files convention:
	 * <Database>-<name>-<description>.csv
	 */
	List<String> datasets = new ArrayList<String>();

	@Autowired
	SecurityRepository securityRepository;

	@Autowired
	DataSetRepository datasetRepository;	
	
	@PostConstruct
	public void post_construct() {
		datasets.add("YAHOO-OMX30-All securites included in OMX30.csv");	
		datasets.add("YAHOO-OSCAR-The Money Machine.csv");	
	}

	/**
	 * Insert all securities from cvsFiles.
	 * 
	 */
	public void addDatasetSecuritiesFromCvsFile() {
	
		datasets.forEach(csvFile -> {
			saveToRepo(csvFile);
		});
		
	}	
	
	@SneakyThrows
	private void saveToRepo(String csvFile) {
		Resource file = new ClassPathResource(csvFile);
		Reader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(file.getInputStream()));
		} catch (IOException e) {
			logger.severe("Could not read file, file="+file);
		}		

		String database = StringUtils.substringBefore(csvFile, "-");
		String datasetName =  StringUtils.substringBetween(csvFile, "-");
		String datasetDesc =  StringUtils.substringAfterLast(csvFile, "-");
		datasetDesc = StringUtils.remove(datasetDesc, ".csv");

		DataSet dataset;
		if ( (dataset = datasetRepository.findByName(datasetName)) != null ) {
			logger.info("Dataset="+dataset.getName()+ " exist in database.");
		} else {
			dataset = new DataSet(datasetName, datasetDesc);
			dataset = datasetRepository.saveAndFlush(dataset);
			logger.info("Saved dataset="+dataset.getName()+ " to database.");
		}		
		
		
		CSVReader csvReader = new CSVReader(in, ',', '"', 1);
		String[] line;
		while ((line = csvReader.readNext()) != null) {
			String name = line[0];
			String description = line[1];
			Security security;
	
			if ( (security = securityRepository.findByName(name)) != null ) {
				logger.info("Security="+security.getName()+ " exist in database.");

				checkIfAddToDataset(datasetName, dataset, security);

			} else {
				logger.info("Security name::"+name+" to be inserted::");
				security = securityRepository.saveAndFlush(new Security(name, description, database));

				checkIfAddToDataset(datasetName, dataset, security);
				
			}


		}

		datasetRepository.saveAndFlush(dataset);
		
		csvReader.close();
		in.close();

	}

	private void checkIfAddToDataset(String datasetName, DataSet dataset, Security security) {
		long match = security.getDatasets().stream()
		   .filter(ds -> { 
			   return ds.getName().equals(datasetName);
			})
		   .count();

		if (match == 0) {
			logger.info("add security=" + security.getName() + ", to dataset="+ dataset.getName());
			dataset.getSecurities().add(security);
		}
	}

	
}
