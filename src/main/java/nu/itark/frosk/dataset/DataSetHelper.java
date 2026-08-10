package nu.itark.frosk.dataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import lombok.SneakyThrows;
import nu.itark.frosk.crypto.coinbase.ProductProxy;
import nu.itark.frosk.crypto.coinbase.model.Product;
import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.rapidapi.yhfinance.model.Body;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.SecurityRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

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

	@Autowired
	YahooFinanceDirectClient yahooFinanceClient;
	
	@PostConstruct
	public void post_construct() {
		datasets.add("codes/YAHOO-OMX30-All securites included in OMX30.csv");
		datasets.add("codes/YAHOO-OSCAR-The Money Machine.csv");
		datasets.add("codes/YAHOO-INDEX-World indexes.csv");
		datasets.add("codes/YAHOO-FUTURES-Trend following universe.csv");
	}

	/**
	 * Insert all securities from cvsFiles.
	 * 
	 */
	public void addDatasetSecuritiesFromCvsFile() {
		datasets.forEach(csvFile -> {
			saveToRepo(csvFile);
		});
		saveYahooSwedishListToRepo();
	}

	public void addDatasetSecuritiesForCoinBase() {
		saveCoinbaseToRepo();
	}

	/**
	 * Quote currencies eligible for gap-filling: coins with no EUR pair still
	 * get one security, using whichever of these has the highest 24h volume.
	 * Kept separate from live trading — {@code crypto.intraday.products} (the
	 * live/intraday whitelist) is EUR-only and untouched by this method, so
	 * gap-filled securities are backtest/screening data only.
	 */
	private static final List<String> GAP_FILL_QUOTE_CURRENCIES = List.of("USD", "USDC");

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

		List<Product> allProducts = productProxy.getProducts().getProducts();

		Set<String> eurBaseCurrencies = new HashSet<>();
		for (Product product : allProducts) {
			if ("EUR".equals(product.getQuote_currency_id())) {
				eurBaseCurrencies.add(product.getBase_currency_id());
			}
		}

		// Coins without an EUR pair: gap-fill with the single highest-volume USD/USDC pair.
		Map<String, Product> bestGapProductByBase = new HashMap<>();
		for (Product product : allProducts) {
			if (!GAP_FILL_QUOTE_CURRENCIES.contains(product.getQuote_currency_id())) continue;
			if (!isTradeable(product)) continue;
			String base = product.getBase_currency_id();
			if (eurBaseCurrencies.contains(base)) continue;
			double volume = parseVolume(product.getApproximate_quote_24h_volume());
			Product existing = bestGapProductByBase.get(base);
			if (existing == null || volume > parseVolume(existing.getApproximate_quote_24h_volume())) {
				bestGapProductByBase.put(base, product);
			}
		}

		List<Product> toInsert = new ArrayList<>();
		for (Product product : allProducts) {
			if ("EUR".equals(product.getQuote_currency_id())) {
				toInsert.add(product);
			}
		}
		toInsert.addAll(bestGapProductByBase.values());
		logger.info("Coinbase sync: "+eurBaseCurrencies.size()+" EUR pairs, "+bestGapProductByBase.size()+" gap-filled USD/USDC pairs (no EUR listing).");

		for (Product product: toInsert) {
			Security security = securityRepository.findByName(product.getProduct_id());
			if (Objects.nonNull(security) ) {
				//logger.info("Security="+security.getName()+ " exist in database:" + database);
				checkIfAddToDataset(datasetName, dataset, security);
			} else {
				//logger.info("Product_id::"+product.getProduct_id()+" to be inserted::");
				security = securityRepository.save(new Security(product.getProduct_id(), product.getBase_name() + " " + product.getQuote_name(), database, product.getQuote_currency_id()));
				checkIfAddToDataset(datasetName, dataset, security);
			}
		}
		datasetRepository.saveAndFlush(dataset);
	}

	private double parseVolume(String value) {
		if (value == null || value.isBlank()) return 0.0;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	/** Excludes delisted/disabled/view-only gap-fill candidates (no live orders and often no fresh candle data). */
	private boolean isTradeable(Product product) {
		return "online".equals(product.getStatus())
				&& !Boolean.parseBoolean(product.getIs_disabled())
				&& !Boolean.parseBoolean(product.getTrading_disabled());
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
				security = securityRepository.saveAndFlush(new Security(name, description, database, null));
				checkIfAddToDataset(datasetName, dataset, security);

			}
		}

		datasetRepository.saveAndFlush(dataset);
		
		csvReader.close();
		in.close();

	}

	public void modifyCustomListFile() {
		Resource file = new ClassPathResource("codes/YAHOO-SWEDISH-All securities in Sweden.csv");
		String outputFileName = "processed_securities.csv";

		try (Reader in = new BufferedReader(new InputStreamReader(file.getInputStream()));
			 CSVReader csvReader = new CSVReader(in, ';', '"', 1);
			 FileWriter fileWriter = new FileWriter(outputFileName);
			 CSVWriter csvWriter = new CSVWriter(fileWriter, ';', '"', '"', "\n")) {

			// Write header to new file
			String[] header = {"Name", "Description"};
			csvWriter.writeNext(header);

			String[] line;
			while ((line = csvReader.readNext()) != null) {
				String name = line[0];
				String description = line[1];

				// Replace blank spaces with dashes in name and add .ST suffix
				String modifiedName = name.replace(" ", "-") + ".ST";

				// Write name and description to new file
				String[] outputLine = {modifiedName, description};
				csvWriter.writeNext(outputLine);
			}

			logger.info("Successfully processed file and created: " + outputFileName);

		} catch (IOException e) {
			logger.severe("Error processing file: " + e.getMessage());
		}
	}

	@SneakyThrows
	public void saveYahooSwedishListToRepo() {
		Resource file = new ClassPathResource("codes/YAHOO-SWEDISH-All securities in Sweden.csv");
		Reader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(file.getInputStream()));
		} catch (IOException e) {
			logger.severe("Could not read file, file="+file);
		}
		String csvFile = StringUtils.substringAfter("codes/YAHOO-SWEDISH-All securities in Sweden.csv", "/");
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

		CSVReader csvReader = new CSVReader(in, ';', '"', 1);
		String[] line;
		while ((line = csvReader.readNext()) != null) {
			String name = line[0];
			String description = line[1];
			Security security = securityRepository.findByName(name);
			if (Objects.nonNull(security) ) {
				logger.info("Security="+security.getName()+ " exist in database:" + database);
				checkIfAddToDataset(datasetName, dataset, security);
			} else {
				logger.info("Security name::"+name+" to be inserted.");
				Security newSecurity = getYahooSwedishSecurity(name, description, database);
				security = securityRepository.saveAndFlush(newSecurity);
				checkIfAddToDataset(datasetName, dataset, security);
			}
		}
		datasetRepository.saveAndFlush(dataset);
		csvReader.close();
		in.close();
	}

	public Security getYahooSwedishSecurity(String name, String description, String database) {
		double yoyGrowth = getYahooMetaData(name);
		Security newSecurity = new Security(name, description, database, null);
		newSecurity.setYoyGrowth(yoyGrowth);
		return newSecurity;
	}

	private double getYahooMetaData(String name) {
		double totalRevenueThisYear = 0;
		double totalRevenueLastYear = 0;
		Body module = null;
		try {
			module = yahooFinanceClient.getModuleIncomeStatement(name);
			if (module == null) {
				return 0;
			}
			if (module.getIncomeStatementHistory() != null && !module.getIncomeStatementHistory().getIncomeStatementHistory().isEmpty()) {
				totalRevenueThisYear = module.getIncomeStatementHistory().getIncomeStatementHistory().get(0).getTotalRevenue().getRaw();
				if (module.getIncomeStatementHistory().getIncomeStatementHistory().size() > 1) {
					totalRevenueLastYear = module.getIncomeStatementHistory().getIncomeStatementHistory().get(1).getTotalRevenue().getRaw();
				}
			}
			return ((totalRevenueThisYear - totalRevenueLastYear) / totalRevenueLastYear) * 100.0;
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkIfAddToDataset(String datasetName, DataSet dataset, Security security) {
		boolean match = security.getDatasets()
				.stream()
				.anyMatch(ds -> ds.getName().equals(datasetName));
		if (!match) {
			logger.info("adding security=" + security.getName() + ", to dataset="+ dataset.getName());
			dataset.getSecurities().add(security);
		}
	}

}
