package nu.itark.frosk.strategies.prediction.labb;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeSeriesForecastingExample {
    private static final Logger log = LoggerFactory.getLogger(TimeSeriesForecastingExample.class);

    public static void main(String[] args) throws IOException, ParseException {
        // Define the stock price values for 10 dates
        double[] stockPrices = {100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 130.0, 135.0, 140.0, 145.0};

        // Define the date range for the stock prices
        LocalDate startDate = LocalDate.parse("2023-06-01");
        List<LocalDate> dateRange = new ArrayList<>();
        for (int i = 0; i < stockPrices.length; i++) {
            dateRange.add(startDate.plusDays(i));
        }

        // Define the number of future dates to forecast
        int numFutureDates = 3;

        // Prepare the data for training
        INDArray timeSeries = Nd4j.create(stockPrices, new int[]{stockPrices.length, 1});
        INDArray features = timeSeries.get(NDArrayIndex.all(), NDArrayIndex.point(0));

/*
        List<org.nd4j.linalg.dataset.DataSet> trainingData = new ArrayList<>();
        trainingData.add(new DataSet(timeSeries, features));
*/


        // Normalize the data using Min-Max scaling

        NormalizerMinMaxScaler normalizer = new NormalizerMinMaxScaler();
        normalizer.fitLabel(true);
        normalizer.fit((org.nd4j.linalg.dataset.api.DataSet) features);
        normalizer.transform(features);


        // Split the data into input features and labels
        INDArray input = features.get(NDArrayIndex.interval(0, stockPrices.length - 1), NDArrayIndex.all());
        INDArray labels = features.get(NDArrayIndex.interval(1, stockPrices.length), NDArrayIndex.all());

        List<org.nd4j.linalg.dataset.DataSet> trainingData = new ArrayList<>();
        trainingData.add(new DataSet(input, labels));


        // Define the model configuration
        int numInputs = 1;
        int numHiddenNodes = 10;
        int numOutputs = 1;
        int numEpochs = 100;
        double learningRate = 0.01;

        // Build the network configuration
        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(123)
                .updater(new Nesterovs(learningRate, 0.9))
                .weightInit(WeightInit.XAVIER)
                .l2(0.001)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(numInputs)
                        .nOut(numHiddenNodes)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunction.MSE)
                        .nIn(numHiddenNodes)
                        .nOut(numOutputs)
                        .activation(Activation.IDENTITY)
                        .build());

        // Create the model
        MultiLayerConfiguration configuration = builder.build();
        MultiLayerNetwork model = new MultiLayerNetwork(configuration);
        model.init();

        // Train the model
        for (int epoch = 0; epoch < numEpochs; epoch++) {
            model.fit(input, labels);
        }

        // Perform forecasting for the next three dates
        INDArray forecast = features.get(NDArrayIndex.interval(stockPrices.length - 1, stockPrices.length), NDArrayIndex.all());
        for (int i = 0; i < numFutureDates; i++) {
            INDArray nextInput = model.output(forecast);
            forecast = Nd4j.concat(0, forecast, nextInput);
        }

        // Denormalize the forecasted values
        //INDArray denormalizedForecast = normalizer.revert(forecast);
        INDArray denormalizedForecast = forecast;

        // Print the forecasted values and dates
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        for (int i = 0; i < denormalizedForecast.rows(); i++) {
            Date date = Date.from(dateRange.get(stockPrices.length - 1 + i).atStartOfDay(ZoneId.systemDefault()).toInstant());
            String formattedDate = dateFormat.format(date);
            double forecastValue = denormalizedForecast.getDouble(i);
            log.info("Date: {}, Forecast: {}", formattedDate, forecastValue);
        }
    }
}




