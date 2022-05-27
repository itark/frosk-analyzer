package nu.itark.frosk.dataset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.coinbase.exchange.model.Product;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
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

	@Autowired
	ProductProxy productProxy;
	
	@PostConstruct
	public void post_construct() {
		datasets.add("codes/YAHOO-OMX30-All securites included in OMX30.csv");
		datasets.add("codes/YAHOO-OSCAR-The Money Machine.csv");
		datasets.add("codes/YAHOO-INDEX-World indexes.csv");
//		datasets.add("codes/COINBASE-CB-Simple.csv");
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

	public void addDatasetSecuritiesForCoinBase() {
		saveCoinbaseToRepo();
	}

	@SneakyThrows
	private void saveCoinbaseToRepo() {
		DataSet dataset;
		String database = "COINBASE";
		String datasetName =  "COINBASE";
		if ( (dataset = datasetRepository.findByName(datasetName)) != null ) {
			logger.info("Dataset="+dataset.getName()+ " exist in database: COINBASE");
		} else {
			dataset = new DataSet("COINBASE", "COINBASE");
			dataset = datasetRepository.saveAndFlush(dataset);
			logger.info("Saved dataset="+dataset.getName()+ " to database.");
		}

		for (Product product: productProxy.getProductsForQuoteCurrency("EUR")) {
			Security security = securityRepository.findByName(product.getId());
			if (Objects.nonNull(security) ) {
				logger.info("Security="+security.getName()+ " exist in database:" + database);
				checkIfAddToDataset(datasetName, dataset, security);
			} else {
				logger.info("Security name::"+product.getId()+" to be inserted::");
				security = securityRepository.saveAndFlush(new Security(product.getId(), product.getDisplay_name(), database));
				checkIfAddToDataset(datasetName, dataset, security);
			}
		}

		for (Product product: productProxy.getProductsForQuoteCurrency("USDT")) {
			Security security = securityRepository.findByName(product.getId());
			if (Objects.nonNull(security) ) {
				logger.info("Security="+security.getName()+ " exist in database:" + database);
				checkIfAddToDataset(datasetName, dataset, security);
			} else {
				logger.info("Security name::"+product.getId()+" to be inserted::");
				security = securityRepository.saveAndFlush(new Security(product.getId(), product.getDisplay_name(), database));
				checkIfAddToDataset(datasetName, dataset, security);
			}
		}


		datasetRepository.saveAndFlush(dataset);

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

		csvFile = StringUtils.substringAfter(csvFile, "/");
		String database = StringUtils.substringBefore(csvFile, "-");
		String datasetName =  StringUtils.substringBetween(csvFile, "-");
		String datasetDesc =  StringUtils.substringAfterLast(csvFile, "-");
		datasetDesc = StringUtils.remove(datasetDesc, ".csv");

		DataSet dataset;
		if ( (dataset = datasetRepository.findByName(datasetName)) != null ) {
			logger.info("Dataset="+dataset.getName()+ " exist in database: "+ database);
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
			Security security = securityRepository.findByName(name);
				if (Objects.nonNull(security) ) {
				logger.info("Security="+security.getName()+ " exist in database:" + database);
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
