package nu.itark.frosk.dataset;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Trade {
    private long id;
    private String securityName; //härledd ifrån TimeSeries getName.
    private String strategy;
    private long date;
    private String dateReadable;
    private long price;
    private String type;
}
