package nu.itark.frosk.repo;

import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.Security;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJDataSetRepository {
	Logger logger = Logger.getLogger(TestJDataSetRepository.class.getName());

	
	@Autowired
	DataSetRepository dataSetRepo;
	
	@Autowired
	SecurityRepository securityRepo;
	

	@Test
	public final void testSaveDataSet() {

		dataSetRepo.deleteAll();

		DataSet dataSet = new DataSet("OMX30", "All securities included in the OMX30-index.");
		Security security = securityRepo.findByName("SAND.ST");
		
		Assert.assertNotNull(security);
		
		logger.info("security="+security);
		dataSet.getSecurities().add(security);
		
		dataSetRepo.save(dataSet);
		
	}
	
	
}
