package nu.itark.frosk.dataset;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.SecurityRepository;

@SpringBootTest
public class TestJDataSetHelper  extends BaseIntegrationTest {

	@Autowired
	DataSetHelper dataSetHelper;

	@Autowired
	SecurityRepository securityRepository;

	@Autowired
	DataSetRepository datasetRepository;		
	
	@Test
	public final void runAddFromFile() {
		dataSetHelper.addDatasetSecuritiesFromCvsFile();
	}

	@Test
	public final void runSaveYahooSwedishListToRepo() {
		Security sec =dataSetHelper.getYahooSwedishSecurity("NIBE-B.ST", "nibe", "YAHOO");
		System.out.println("sec"+sec);
	}

	@Test
	public final void runAddForCoinbase() {
		dataSetHelper.addDatasetSecuritiesForCoinBase();
	}


	@Test
	public void helloworld() {
		datasetRepository.deleteAllInBatch();
		securityRepository.deleteAllInBatch();
		
		DataSet dataset = new DataSet("kalle", "was here");
		
		Security security = null ;
/*
				=Security.builder()
				.name("Volvo")
				.description("AB")
				.database("Sweden")
				.build();
*/

		
		dataset.getSecurities().add(security);

		datasetRepository.save(dataset);
		
		
	}

	@Test
	public final void testSubstrings() {
		String csvFile = "YAHOO-OMX30-description.csv";
		
		String database = StringUtils.substringBefore(csvFile, "-");
		String dataset =  StringUtils.substringBetween(csvFile, "-");
		String datasetDesc =  StringUtils.substringAfterLast(csvFile, "-");
		datasetDesc = StringUtils.remove(datasetDesc, ".csv");
		

		System.out.println("database="+database);
		System.out.println("dataset="+dataset);
		System.out.println("datasetDesc="+datasetDesc);

		
	}

	@Test
	public void testModifyFile() {
		dataSetHelper.modifyCustomListFile();


	}
	
	
}
