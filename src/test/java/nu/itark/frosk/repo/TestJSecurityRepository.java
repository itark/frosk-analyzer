package nu.itark.frosk.repo;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.model.Security;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

//@SpringBootTest(classes = FroskApplication.class)
@SpringBootTest
public class TestJSecurityRepository {
	Logger logger = Logger.getLogger(TestJSecurityRepository.class.getName());

	
	@Autowired
	SecurityRepository securityRepo;
	
	@Test
	public final void testExist() {
		
		Security security = new Security("BITFINEX/BTCEUR","name", "BITFINEX");
		
		
		boolean exist = securityRepo.existsByName(security.getName());
		
		
		logger.info("exist="+exist);
	}

	@Test
	public final void testFindByName() {
		
		logger.info("hello="+securityRepo.findByName("ABB.ST"));
	}	
	
	
//	@Test
//	public void testFindBySEcurity() {
//	Security security = securityRepo.findById(new Long(2418800));
//	if (security != null) {
//		
//		logger.info("security="+security.getName());
//	}
//	FeaturedStrategy fs = fsRepo.findBySecurity(security);
//	Assert.assertNotNull(fs);
//	logger.info("fs"+fs);
	
	
//	}		
	
	
	
	
}
