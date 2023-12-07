package nu.itark.frosk.strategies.prediction.signaflo;

import com.github.signaflo.timeseries.TestData;
import com.github.signaflo.timeseries.TimePeriod;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;
import com.workday.insights.timeseries.arima.Arima;
import com.workday.insights.timeseries.arima.struct.ArimaParams;
import com.workday.insights.timeseries.arima.struct.ForecastResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
public class ArimaModel {

    public static void main(String[] args) {
        ArimaModel model = new ArimaModel();
        model.forecast(null, 1);
    }

    public double[] forecast(double[] serie, int forecastSize) {
        com.github.signaflo.timeseries.model.arima.Arima.model(TestData.ukcars, ArimaOrder.order(0, 2, 0, com.github.signaflo.timeseries.model.arima.Arima.Drift.INCLUDE));
        TimeSeries timeSeries = TimeSeries.from(TimePeriod.oneMonth(), "2000-01-01T00:00",
                30.4375, 29.455645161290324, 40.58333333333333, 41.23790322580645, 15.21875, 10.800403225806452,
                30.4375, 25.002232142857142, 54.983870967741936, 33.48125,
                19.637096774193548, 35.51041666666667, 25.528225806451616,
                17.673387096774196, 8.116666666666667, 12.764112903225808,
                3.04375, 6.872983870967742, 25.528225806451616, 14.69396551724138,
                6.872983870967742, 13.189583333333333, 14.727822580645162, 12.175,
                3.9274193548387095, 31.419354838709676, 8.116666666666667, 30.4375,
                10.145833333333332, 10.800403225806452, 25.528225806451616,
                4.348214285714286, 0.9818548387096774, 1.0145833333333334,
                4.909274193548387, 9.13125);
        com.github.signaflo.timeseries.model.arima.Arima model = com.github.signaflo.timeseries.model.arima.Arima.model(timeSeries, ArimaOrder.order(1, 1, 0, 1, 0, 1));

        Forecast forecast =  model.forecast(forecastSize, 0.05);

        System.out.println("forecast:"+forecast);
        return null;

    }
}
