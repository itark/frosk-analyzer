package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.Security;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@Slf4j
@SpringBootTest(classes = {FroskApplication.class})
public class TestJSecurityRepository extends BaseIntegrationTest {
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
		
		logger.info("hello="+ ReflectionToStringBuilder.toString(securityRepo.findByName("SHPING-EUR")));
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
