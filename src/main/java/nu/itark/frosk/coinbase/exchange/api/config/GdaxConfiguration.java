package nu.itark.frosk.coinbase.exchange.api.config;

import java.util.Arrays;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Created by robevansuk on 07/02/2017.
 */
@SpringBootConfiguration
public class GdaxConfiguration {

	/**
	 * Explicit settings of Jackson ObjectMapper.
	 * <li> Support Java 8 LocalDateTime.</li>
	 * <li> Indent output </li>
	 * <li> Exlude properties with null vales</li>
	 * 
	 * @return ObjectMapper
	 */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
    	return Jackson2ObjectMapperBuilder.json()
	            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
	            .createXmlMapper(false)
	            .featuresToEnable(SerializationFeature.INDENT_OUTPUT) //nicer output
	            .serializationInclusion(Include.NON_NULL) //exclude null values
	            .build();
    }
	
	
	
	@Bean
    public RestTemplate restTemplate() {
 
		RestTemplate restTemplate = new RestTemplate(Arrays.asList(new MappingJackson2HttpMessageConverter(objectMapper())));		
		
		
		
		return restTemplate;
    }
}
