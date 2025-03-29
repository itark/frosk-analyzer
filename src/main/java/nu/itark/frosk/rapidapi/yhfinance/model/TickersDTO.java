package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

import java.util.List;

@Data
public class TickersDTO {
    private Meta meta;
    private List<Stock> body;

    @Data
    public static class Meta {
        private String version;
        private int status;
        private String copywrite;
        private int totalrecords;
        private Headers headers;
    }

    @Data
    public static class Headers {
        private String symbol;
        private String name;
        private String lastsale;
        private String netchange;
        private String pctchange;
        private String marketCap;
    }

    @Data
    public static class Stock {
        private String symbol;
        private String name;
        private String lastsale;
        private String netchange;
        private String pctchange;
        private String marketCap;
    }



}
