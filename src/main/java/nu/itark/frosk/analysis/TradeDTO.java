package nu.itark.frosk.analysis;

import lombok.Data;

@Data
public class TradeDTO {
    private long id;
    private String securityName; //härledd ifrån TimeSeries getName.
    private String strategy;
    private long date;
    private String dateReadable;
    private long price;
    private String type;
}
