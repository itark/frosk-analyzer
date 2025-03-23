package nu.itark.frosk.rapidapi.yhfinance.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class YahooFinanceResponse {
        private Meta meta;
        private Map<String, StockData> body;

        public Meta getMeta() {
            return meta;
        }

        public void setMeta(Meta meta) {
            this.meta = meta;
        }

        public Map<String, StockData> getBody() {
            return body;
        }

        public void setBody(Map<String, StockData> body) {
            this.body = body;
        }

        public static class Meta {
            private String processedTime;
            private String currency;
            private String symbol;
            private String exchangeName;
            private String fullExchangeName;
            private String instrumentType;
            private long firstTradeDate;
            private long regularMarketTime;
            private boolean hasPrePostMarketData;
            private int gmtoffset;
            private String timezone;
            private String exchangeTimezoneName;
            private double regularMarketPrice;
            private double fiftyTwoWeekHigh;
            private double fiftyTwoWeekLow;
            private double regularMarketDayHigh;
            private double regularMarketDayLow;
            private long regularMarketVolume;
            private String longName;
            private String shortName;
            private double chartPreviousClose;
            private int priceHint;
            private String dataGranularity;
            private String range;
            private String version;
            private int status;
            private String copywrite;

            // Getters and setters
        }

        public static class StockData {
            private String date;
            @JsonProperty("date_utc")
            private long dateUtc;
            private double open;
            private double high;
            private double low;
            private double close;
            private long volume;
            private double adjclose;

            // Getters and setters
        }

}
