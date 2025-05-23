package nu.itark.frosk.repo;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.HedgeIndex;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.service.HedgeIndexService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.Trade;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
public class TestJHedgeIndexRepository extends BaseIntegrationTest {

	@Autowired
	HedgeIndexRepository hedgeIndexRepository;

	@Mock
	HedgeIndexService hedgeIndexService;

	@Test
	public final void testfindLatestDate() {
		//final Optional<HedgeIndex> topByOrderByDateDesc = hedgeIndexRepository.findTopByOrderByDateDesc();
		final Optional<HedgeIndex> topByOrderByDateDesc = hedgeIndexRepository.findTopByIndicatorOrderByDateDesc("VIX");
		if (topByOrderByDateDesc.isPresent()) {
			log.info("topByOrderByDateDesc:{}", topByOrderByDateDesc.get());
		}
	}

	@Test
	public final void testFindAll() {
		//final Optional<HedgeIndex> topByOrderByDateDesc = hedgeIndexRepository.findTopByOrderByDateDesc();
		final List<HedgeIndex> all = hedgeIndexRepository.findAll();
		log.info("all:{}", all.size());
	}


	@Test
	public void delete() {
		//hedgeIndexRepository.deleteAll();
	}


	@Test
	public void testX() {
		List<HedgeIndex> vix = hedgeIndexRepository.findByIndicator("VIX");
		System.out.println("vix.size:s" + vix.size());

		vix.stream()
				.sorted(Comparator.comparing(HedgeIndex::getDate))
	//		.peek(t-> System.out.println(ReflectionToStringBuilder.toString(t, ToStringStyle.MULTI_LINE_STYLE)))
				.peek(t-> {
					if (t.getRisk().equals(Boolean.TRUE)) {
						System.out.println("Risk on " + t.getDate());
					} else {
						System.out.println("No risk on " + t.getDate());
					}
				})
				.collect(Collectors.toSet());
	}

	@Test
	public void testSummarizeRisk() {
		for (HedgeIndexRepository.RiskSummary riskSummary : hedgeIndexRepository.summarizeRiskByDateDTO()) {
			System.out.println("indicator:"+riskSummary.getIndicator()+",date:"+riskSummary.getDate()+",risk:"+riskSummary.getRiskCount()+", price:"+riskSummary.getPrice());
		}


	}

}