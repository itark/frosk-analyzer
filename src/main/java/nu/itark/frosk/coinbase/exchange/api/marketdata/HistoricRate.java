package nu.itark.frosk.coinbase.exchange.api.marketdata;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * https://docs.pro.coinbase.com/?r=1#get-trades
 * 
 * https://github.com/knowm/XChange/issues/1580
 * https://stackoverflow.com/questions/44886450/gdax-api-get-historic-rates-returning-inconsistent-results
 * 
 * 
 * @author fredrikmoller
 *
 */
@Data
@JsonDeserialize(using = HistoricRate.HistoricRateDeserializer.class)
public class HistoricRate {

	private final long time;
	private final BigDecimal low;
	private final BigDecimal high;
	private final BigDecimal open;
	private final BigDecimal close;
	private final BigDecimal volume;

	public HistoricRate(long time,BigDecimal low, BigDecimal high, BigDecimal open, BigDecimal close, BigDecimal volume) {
	this.time = time;
	this.low = low;
	this.high = high;
	this.open = open;
	this.close = close;
	this.volume = volume;
	}

	static class HistoricRateDeserializer extends JsonDeserializer {

		@Override
		public HistoricRate deserialize(JsonParser jsonParser, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {

			ObjectCodec oc = jsonParser.getCodec();
			JsonNode node = oc.readTree(jsonParser);
			
			
			if (node.isArray()) {
				long time = node.path(0).asLong();

				BigDecimal low = new BigDecimal(node.path(1).asText());
				BigDecimal high = new BigDecimal(node.path(2).asText());
				BigDecimal open = new BigDecimal(node.path(3).asText());
				BigDecimal close = new BigDecimal(node.path(4).asText());
				BigDecimal volume = new BigDecimal(node.path(5).asText());

				return new HistoricRate(time, low, high, open, close, volume);
			}
			return null;
		}
	}	
	
}
