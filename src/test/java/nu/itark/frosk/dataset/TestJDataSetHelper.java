package nu.itark.frosk.dataset;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.Security;
import nu.itark.frosk.repo.DataSetRepository;
import nu.itark.frosk.repo.SecurityRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJDataSetHelper {

	@Autowired
	DataSetHelper dataSetHelper;

	@Autowired
	SecurityRepository securityRepository;

	@Autowired
	DataSetRepository datasetRepository;		
	

	
	
	@Test
	public final void run() {

		dataSetHelper.insertDatasetSecuritiesFromCvsFile();
		
	}

//	@Test
//	public final void runDatasetIntoDatabase() {
//
//		dataSetHelper.insertDataset();
//		
//	}
	
	@Test
	public void helloworld() {
		datasetRepository.deleteAllInBatch();
		securityRepository.deleteAllInBatch();
		
		DataSet dataset = new DataSet("kalle", "was here");
		
		Security security = new Security("Volvo","AB", "Sweden");
		Security security2 = new Security("IBM","Corp", "World");
		
		dataset.getSecurities().add(security);
		dataset.getSecurities().add(security2);
		
		datasetRepository.save(dataset);
		
		
	}

	@Test
	public void helloworldAfter() {
		datasetRepository.deleteAllInBatch();
		securityRepository.deleteAllInBatch();
		
		DataSet dataset = new DataSet("kalle", "was here");
		
		Security security = new Security("Volvo","AB", "Sweden");
		Security security2 = new Security("IBM","Corp", "World");
		
		dataset.getSecurities().add(security);
		dataset.getSecurities().add(security2);
		
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
	
	
	
}
