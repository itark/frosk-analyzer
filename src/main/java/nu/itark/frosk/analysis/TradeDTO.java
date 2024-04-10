package nu.itark.frosk.analysis;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeDTO {
    private long id;
    private String securityName; //härledd ifrån TimeSeries getName.
    private String strategy;
    private long date;
    private String dateReadable;
    private BigDecimal price;
    private BigDecimal grossProfit;
    private BigDecimal amount;
    private BigDecimal pnl;
    private String type;
}
