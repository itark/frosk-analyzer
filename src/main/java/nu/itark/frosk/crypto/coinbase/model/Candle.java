package nu.itark.frosk.crypto.coinbase.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;


@Data
public class Candle {

    private Instant start;
    private BigDecimal low;
    private BigDecimal high;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal volume;

/*
    public Candle(String[] entry) {
        this(Instant.ofEpochSecond(Long.parseLong(entry[0])),
                new BigDecimal(entry[1]),
                new BigDecimal(entry[2]),
                new BigDecimal(entry[3]),
                new BigDecimal(entry[4]),
                new BigDecimal(entry[5]));
    }

    public Candle(Instant time, BigDecimal low, BigDecimal high, BigDecimal open, BigDecimal close, BigDecimal volume) {
        this.time   = time;
        this.low    = low;
        this.high   = high;
        this.open   = open;
        this.close  = close;
        this.volume = volume;
    }
*/

}
