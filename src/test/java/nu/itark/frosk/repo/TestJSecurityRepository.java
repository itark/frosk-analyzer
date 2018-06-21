package nu.itark.frosk.repo;

import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.model.Security;

@RunWith(SpringRunner.class)
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
	
	
}
