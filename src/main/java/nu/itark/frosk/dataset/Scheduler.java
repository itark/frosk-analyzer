package nu.itark.frosk.dataset;

import java.time.LocalDateTime;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class Scheduler {
	Logger logger = Logger.getLogger(Scheduler.class.getName());

	@Autowired
	private DataManager dataManager;

//	@Scheduled(cron="${download.schedule}")
//	@Scheduled(cron="*/10 * * * * *")
	public void runDownload() {
		LocalDateTime now = LocalDateTime.now();
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
				.ofPattern("yyyy-MM-dd HH:mm:ss SS");
		logger.info("::Scheduler::runDownload() about to execute, time=" + now.format(formatter));

		
		dataManager.insertSecurityPricesIntoDatabase(Database.BITFINEX, true);

	}
	
	
	
}
