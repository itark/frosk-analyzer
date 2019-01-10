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
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class FileHelper {
	Logger logger = Logger.getLogger(FileHelper.class.getName());	

	@Autowired
	RestTemplate restTemplate;
	
	@Bean 
	public RestTemplate restTemplate() {
		return new RestTemplate();
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
	restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter()); 

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		HttpEntity<String> entity = new HttpEntity<String>(headers);

		try {
			ResponseEntity<byte[]> response = restTemplate.exchange(downloadUrl, HttpMethod.GET, entity, byte[].class, "1");

			logger.info("File downloaded from:" + downloadUrl + ", size=" + response.getBody().length);
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

										
			}
		} catch (RestClientException rcex) {
			logger.severe("Can not access :" + downloadUrl + rcex);
			throw rcex;
		} catch (FileNotFoundException fnfex) {
			logger.severe("Can not find  :" +writeFile +fnfex);
			throw fnfex;
		} catch (IOException ioex) {
			logger.severe("Can not use  :" +writeFile + ioex);
			throw ioex;
		}
	
		logger.info("File: "+ writeFile + " saved on disk.");
	}

}
