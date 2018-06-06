package nu.itark.frosk.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;
/**
 * Holds the quandl database, code and description on security
 * 
 * @author fredrikmoller
 *
 */
public class SecurityCode {
//	private static Logger logger = LogManager.getLogger(SecurityCode.class);

	private String code;
	private String description;

	/**
	 * Code with prefixed database, e.g. WIKI/AAPL
	 * 
	 * @param code
	 * @param description
	 */
	public SecurityCode(String code, String description) {
		this.code = code;
		this.description = description;
	}
	
	/**
	 * Creates a List of {@linkplain SecurityCode} on provided cvs-file.
	 * 
	 * @param csvfile
	 * @return List<SecurityCode>
	 */
	public static List<SecurityCode> loadCvsSeries(String csvfile) {
		InputStream stream = SecurityCode.class.getClassLoader().getResourceAsStream(csvfile);
		List<SecurityCode> codes = new ArrayList<SecurityCode>();
		InputStreamReader isr = new InputStreamReader(stream, Charset.forName("UTF-8"));
		CSVReader csvReader = new CSVReader(isr, ',', '"', 1);
		try {
			String[] line;
			while ((line = csvReader.readNext()) != null) {

				String db_code = line[0];
				String description = line[1];

				SecurityCode code = new SecurityCode(db_code, description);
				codes.add(code);
			}
		} catch (IOException ioe) {
//			logger.error("something went wrong...with file:"+csvfile);
		}
		return codes;
	}
	
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	
	
}
