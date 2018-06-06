package nu.itark.frosk.marknadssok.fi.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * This class helps managing files for HovedEnheter and Underenheter
 * 
 * 
 * @author Fredrik Möller
 * @date Dec 2, 2016
 *
 */
public class FileHelper {
//	private static Logger logger = LogManager.getLogger(FileHelper.class);
	private RestTemplate restTemplate = null;
	
	/**
	 * Constructor injects the RestTemplate
	 * 
	 * @param restTemplate
	 */
	public FileHelper(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	
	/**
	 * This method get file from url and save it on disk.
	 * 
	 * 
	 * @param downloadUrl the url where file is hosted.
	 * @param filePath path to file.
	 * @param cvsFile  the name of the file to be saved on disk.
	 * @throws IOException 
	 */
	public void downloadFile(String downloadUrl, String writeFile) throws IOException  {
	restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter()); //Funkar för google finance download
//		restTemplate.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		HttpEntity<String> entity = new HttpEntity<String>(headers);

		try {
			ResponseEntity<byte[]> response = restTemplate.exchange(downloadUrl, HttpMethod.GET, entity, byte[].class, "1");

//			logger.info("File downloaded from:" + downloadUrl + ", size=" + response.getBody().length);
			if (response.getStatusCode() == HttpStatus.OK) {
				ByteArrayInputStream bis = new ByteArrayInputStream(response.getBody());
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bis));
				FileOutputStream fos = new FileOutputStream(writeFile);
				BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
				int len = 0;
				char[] buffer = new char[1024];
				while ((len = bufferedReader.read(buffer)) > 0) {
					bufferedWriter.write(buffer, 0, len);
				}				
				
				bufferedReader.close();
				bufferedWriter.flush();
				bufferedWriter.close();
				fos.close();
				bis.close();


//				ByteArrayInputStream bis = new ByteArrayInputStream(response.getBody());
//				FileOutputStream fos = new FileOutputStream(writeFile);
//				byte[] buffer = new byte[1024];
//				int len = 0;
//				while ((len = bis.read(buffer)) > 0) {
//					fos.write(buffer, 0, len);
//				}
//
//				fos.close();
//				bis.close();
										
			}
		} catch (RestClientException rcex) {
//			logger.info("Can not access :" + downloadUrl, rcex);
			throw rcex;
		} catch (FileNotFoundException fnfex) {
//			logger.info("Can not find  :" +writeFile, fnfex);
			throw fnfex;
		} catch (IOException ioex) {
//			logger.info("Can not use  :" +writeFile, ioex);
			throw ioex;
		}
	
//		logger.info("File: "+ writeFile + " saved on disk.");
	}

}
