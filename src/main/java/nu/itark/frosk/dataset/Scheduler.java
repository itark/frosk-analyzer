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

	@Scheduled(cron="${download.schedule}")
//	@Scheduled(cron="*/20 * * * * * ")

//	The pattern is a list of six single space-separated fields: representing second, minute, hour, day, month, weekday. Month and weekday names can be given as the first three letters of the English names.


	
//Example patterns:
//
//"0 0 * * * *" = the top of every hour of every day.
//"*/10 * * * * *" = every ten seconds.
//"0 0 8-10 * * *" = 8, 9 and 10 o'clock of every day.
//"0 0 6,19 * * *" = 6:00 AM and 7:00 PM every day.
//"0 0/30 8-10 * * *" = 8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day.
//"0 0 9-17 * * MON-FRI" = on the hour nine-to-five weekdays
//"0 0 0 25 12 ?" = every Christmas Day at midnight

	
	public void runDownload() {
		LocalDateTime now = LocalDateTime.now();
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
				.ofPattern("yyyy-MM-dd HH:mm:ss SS");
		logger.info("::Scheduler::runDownload() about to execute, time=" + now.format(formatter));

//		dataManager.insertSecurityPricesIntoDatabase(Database.BITFINEX, true);
		dataManager.addSecurityPricesIntoDatabase(Database.YAHOO);

	}
	
	
	
}
