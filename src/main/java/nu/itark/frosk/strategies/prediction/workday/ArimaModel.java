package nu.itark.frosk.strategies.prediction.workday;

import com.github.signaflo.timeseries.TestData;
import com.github.signaflo.timeseries.TimePeriod;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;
import com.workday.insights.timeseries.arima.Arima;
import com.workday.insights.timeseries.arima.struct.ArimaParams;
import com.workday.insights.timeseries.arima.struct.ForecastResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ArimaModel {

    public static void main2(String[] args) {
        ArimaModel arimaModel = new ArimaModel();


// Prepare input timeseries data.
        double[] dataArray = new double[] {2, 1, 2, 5, 2, 1, 2, 5, 2, 1, 2, 5, 2, 1, 2, 5};

// Set ARIMA model parameters.
        int p = 3;
        int d = 0;
        int q = 3;
        int P = 1;
        int D = 1;
        int Q = 0;
        int m = 0;
        int forecastSize = 1;

        ArimaParams arimaParams = new ArimaParams(p,d,q,P,D,Q,m);


// Obtain forecast result. The structure contains forecasted values and performance metric etc.
        ForecastResult forecastResult = Arima.forecast_arima(dataArray, forecastSize, arimaParams);

// Read forecast values
        double[] forecastData = forecastResult.getForecast(); // in this example, it will return { 2 }

      //  System.out.println("forecastData="+ ReflectionToStringBuilder.toString(forecastData));

// You can obtain upper- and lower-bounds of confidence intervals on forecast values.
// By default, it computes at 95%-confidence level. This value can be adjusted in ForecastUtil.java
        double[] uppers = forecastResult.getForecastUpperConf();
        double[] lowers = forecastResult.getForecastLowerConf();

       // System.out.println("uppers="+ ReflectionToStringBuilder.toString(uppers));
       // System.out.println("lowers="+ ReflectionToStringBuilder.toString(lowers));

// You can also obtain the root mean-square error as validation metric.
        double rmse = forecastResult.getRMSE();

// It also provides the maximum normalized variance of the forecast values and their confidence interval.
        double maxNormalizedVariance = forecastResult.getMaxNormalizedVariance();

// Finally you can read log messages.
        String log = forecastResult.getLog();

        //System.out.println("log"+log);

        arimaModel.forecast(dataArray,4);


    }

    public double[] forecast(double[] serie, int forecastSize) {
        int p = 3;
        int d = 0;
        int q = 3;
        int P = 1;
        int D = 1;
        int Q = 0;
        int m = 0;

/*
     * @param p ARIMA parameter, the order (number of time lags) of the autoregressive model
     * @param d ARIMA parameter, the degree of differencing
                * @param q ARIMA parameter, the order of the moving-average model
                * @param P ARIMA parameter, autoregressive term for the seasonal part
                * @param D ARIMA parameter, differencing term for the seasonal part
                * @param Q ARIMA parameter, moving average term for the seasonal part
                * @param m ARIMA parameter, the number of periods in each season
*/

        ArimaParams arimaParams = new ArimaParams(p,d,q,P,D,Q,m);
        ForecastResult forecastResult = Arima.forecast_arima(serie, forecastSize, arimaParams);
        double[] forecastData = forecastResult.getForecast();
        //log.info("forecastData="+ ReflectionToStringBuilder.toString(forecastData));
        String log = forecastResult.getLog();
      //  System.out.println("log"+log);

        return forecastData;

    }


    public static void main(String[] args) {
        ArimaModel model = new ArimaModel();
        model.forecast2(null, 0);
    }

    public double[] forecast2(double[] serie, int forecastSize) {

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


        return null;


    }

}
