package nu.itark.frosk.dataset;

import java.time.LocalDateTime;
import java.util.logging.Logger;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.HighLander;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Slf4j
public class Scheduler {

	@Autowired
	private HighLander highLander;

	@Scheduled(cron="${download.schedule}")
	public void runDownload() {
		log.info("::Scheduler::runDownload() about to execute");
		//highLander.runInstall(Database.COINBASE);
		highLander.runInstall(Database.YAHOO);

	}

}
