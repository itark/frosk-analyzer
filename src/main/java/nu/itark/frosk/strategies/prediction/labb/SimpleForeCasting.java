package nu.itark.frosk.strategies.prediction.labb;

import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.ArrayList;
import java.util.List;

public class SimpleForeCasting {

    public static void main(String[] args) {
        // Define your stock price values
        double[] stockPrices = {100.0, 105.0, 98.0, 97.0, 96.0, 97.0, 100.0, 102.0};

        // Define the number of time steps (days) to look back for each prediction
        int timeSteps = 5;

        // Prepare the training data
        List<Double> priceList = new ArrayList<>();
        for (double price : stockPrices) {
            priceList.add(price);
        }

        List<DataSet> dataList = new ArrayList<>();
        for (int i = timeSteps; i < priceList.size(); i++) {
            List<Double> prices = priceList.subList(i - timeSteps, i);
            double[] inputArray = new double[timeSteps];
            for (int j = 0; j < timeSteps; j++) {
                inputArray[j] = prices.get(j);
            }
            double[] labelArray = {priceList.get(i)};
            INDArray input = Nd4j.create(inputArray, new int[]{1, timeSteps, 1});
            INDArray label = Nd4j.create(labelArray, new int[]{1, 1, 1});
            dataList.add(new DataSet(input, label));
        }

        DataSetIterator iterator = new ListDataSetIterator<>(dataList, 1);

        // Define the network architecture
        int lstmLayerSize = 100;
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .updater(Updater.ADAM)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                .gradientNormalizationThreshold(1.0)
                .list()
                .layer(0, new LSTM.Builder()
                        .nIn(1)
                        .nOut(lstmLayerSize)
                        .activation(Activation.TANH)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .weightInit(WeightInit.XAVIER)
                        .nIn(lstmLayerSize)
                        .nOut(1)
                        .build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(new ScoreIterationListener(100));

        // Train the model
        int numEpochs = 100;
        for (int i = 0; i < numEpochs; i++) {
            iterator.reset();
            model.fit(iterator);
        }

        // Perform forecasting for the next three dates
        double[] nextDates = {/* Add the next three dates here */};
        double[] lastPrices = priceList.subList(priceList.size() - timeSteps, priceList.size())
                .stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        for (double date : nextDates) {
            INDArray input = Nd4j.create(lastPrices, new int[]{1, timeSteps, 1});
            INDArray output = model.rnnTimeStep(input);
            double predictedPrice = output.getDouble(output.length() - 1);

            // Print the predicted price for each date
            System.out.println("Predicted price for date " + date + ": " + predictedPrice);

            // Update the input array for the next iteration
            System.arraycopy(lastPrices, 1, lastPrices, 0, lastPrices.length - 1);
            lastPrices[lastPrices.length - 1] = predictedPrice;
        }
    }
}

