package nu.itark.frosk.repo;

import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.dataset.labb.Post;
import nu.itark.frosk.dataset.labb.Tag;
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
	public void testSave() {
		// Cleanup the tables
		dataSetRepo.deleteAllInBatch();
		securityRepo.deleteAllInBatch();

		// =======================================

		// Create a Post
		// Post post = new Post("Hibernate Many to Many Example with Spring
		// Boot",
		// "Learn how to map a many to many relationship using hibernate",
		// "Entire Post content with Sample code");

		DataSet dataSet = new DataSet("OMX30", "All securities included in the OMX30-index.");

		// Create two tags
		Tag tag1 = new Tag("Spring Boot");
		Tag tag2 = new Tag("Hibernate");

		Security security = securityRepo.findByName("SAND.ST");
		Security security2 = securityRepo.findByName("ABB.ST");

		// Add tag references in the post
		dataSet.getSecurities().add(security);
		dataSet.getSecurities().add(security2);

		// Add post reference in the tags
		// tag1.getPosts().add(post);
		// tag2.getPosts().add(post);

		dataSetRepo.save(dataSet);

		logger.info("datset="+dataSetRepo.findByName("OMX30"));

	}	
	
	

	@Test
	public final void testSaveDataSet() {

//		dataSetRepo.deleteAll();

		DataSet dataSet = new DataSet("OMX30", "All securities included in the OMX30-index.");
		Security security = securityRepo.findByName("SAND.ST");
		
		Assert.assertNotNull(security);
		
		logger.info("security="+security);
		dataSet.getSecurities().add(security);
		
		dataSetRepo.save(dataSet);
		
	}

	
	@Test
	public final void testFindByName() {
		
		logger.info("hello="+dataSetRepo.findByName("OMX30"));
	}		
	
	
	
}
