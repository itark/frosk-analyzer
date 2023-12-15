package nu.itark.frosk.strategies.prediction.workday;

import com.github.signaflo.timeseries.TestData;
import com.github.signaflo.timeseries.TimePeriod;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.forecast.Forecast;
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

    public static void main(String[] args) {
        ArimaModel arimaModel = new ArimaModel();


// Prepare input timeseries data.
        double[] dataArray = new double[] {2, 1, 2, 5, 2, 1, 2, 5, 2, 1, 2, 5, 2, 1, 2, 5};

// Set ARIMA model parameters.
        int p = 3;
        int d = 0;
        int q = 3;

        int P = 0;
        int D = 0;
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

}
