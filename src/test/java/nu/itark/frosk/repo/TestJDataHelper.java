package nu.itark.frosk.repo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.dataset.DataSetHelper;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJDataHelper {
	
	@Autowired
	DataSetHelper dataSetHelper;

	String[] csvFiles = {"YAHOO-datasets-codes-manual.csv","WIKI-datasets-codes-manual.csv"};
	
	@Test
	public final void run() {

//		dataSetHelper.loadCvsSeries(csvFiles);
		
		dataSetHelper.insertSecurityFromCvsFile();
		
	}
	
}
