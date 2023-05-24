package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.Security;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.logging.Logger;

@Slf4j
@SpringBootTest(classes = {FroskApplication.class})
public class TestJSecurityRepository extends BaseIntegrationTest {

	@Autowired
	SecurityRepository securityRepo;
	
	@Test
	public final void testExist() {

		Security security = null;
/*
		Security security = Security.builder()
				.name("Name")
				.description("NameDesc")
				.database("database")
				.build();
*/

		boolean exist = securityRepo.existsByName(security.getName());
		log.info("exist="+exist);
	}

	@Test
	public final void testFindByName() {
		log.info("hello="+ ReflectionToStringBuilder.toString(securityRepo.findByName("SHPING-EUR")));
	}

	@Test
	public final void findAllByActiveAndQuoteCurrency() {
		List<Security> all = securityRepo.findAll();
		log.info("all.size() {}",all.size());
		List<Security> allActive = securityRepo.findAllByActiveAndQuoteCurrency(true, "EUR");
		log.info("allActive.size() {}",allActive.size());

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
