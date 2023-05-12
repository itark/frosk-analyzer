package nu.itark.frosk.crypto.coinbase.model;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;


@Data
public class Candles {

    private List<Candle> candles;

/*
    public Candles(List<String[]> candles) {
        this.candleList = candles.stream().map(Candle::new).collect(Collectors.toList());
    }
*/

/*
    public List<Candle> getCandleList() {
        return candleList;
    }
*/
}
