package nu.itark.frosk.strategies.prediction.labb;

import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LSTMTimeseriesPrediction2 {

    public static void main(String[] args) throws IOException {

        // Create a simple time series dataset
        List<Double> timeSeries = new ArrayList<>();
        timeSeries.add(1.0);
        timeSeries.add(3.0);
        timeSeries.add(5.0);
        timeSeries.add(7.0);
        timeSeries.add(9.0);

        int sequenceLength = 4; // The number of time steps to look back
        int batchSize = 1;
        int numFeatures = 1; // Number of features in the input data

        // Prepare the data
        List<DataSet> dataSets = new ArrayList<>();
        for (int i = 0; i < timeSeries.size() - sequenceLength; i++) {
            double[] input = new double[sequenceLength];
            double[] labels = new double[1];
            for (int j = 0; j < sequenceLength; j++) {
                input[j] = timeSeries.get(i + j);
            }
            labels[0] = timeSeries.get(i + sequenceLength);
            dataSets.add(new DataSet(Nd4j.create(input), Nd4j.create(labels)));
        }

        // Create a DataSetIterator
        DataSetIterator iterator = new ListDataSetIterator(dataSets, batchSize);

        // Define the LSTM configuration
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .weightInit(WeightInit.XAVIER)
                .updater(org.deeplearning4j.nn.conf.Updater.RMSPROP)
                .list()
                .layer(0, new LSTM.Builder()
                        .nIn(numFeatures)
                        .nOut(10)
                        .activation(Activation.TANH)
                        .build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(10)
                        .nOut(1)
                        .build())
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(100));

        // Train the network
/*
        int numEpochs = 1000;
        for (int epoch = 0; epoch < numEpochs; epoch++) {
            iterator.reset();
            while (iterator.hasNext()) {
                net.fit(iterator.next());
            }
        }
*/

        // Train the network
        int numEpochs = 10;
        for (int i = 0; i < numEpochs; i++) {
            net.fit(iterator);
            iterator.reset();
        }



        // Save the model
        ModelSerializer.writeModel(net, new File("lstm_timeseries_model.zip"), true);

        // Load the model
        MultiLayerNetwork loadedModel = ModelSerializer.restoreMultiLayerNetwork(new File("lstm_timeseries_model.zip"), true);

        // Generate predictions
        double[] input = new double[sequenceLength];
        for (int i = 0; i < sequenceLength; i++) {
            input[i] = timeSeries.get(timeSeries.size() - sequenceLength + i);
        }

        double[] predictedValues = new double[1];
        loadedModel.rnnTimeStep(Nd4j.create(input).reshape(1, 1, -1));
        loadedModel.rnnTimeStep(Nd4j.zeros(1, 1, numFeatures));
        predictedValues[0] = loadedModel.rnnTimeStep(Nd4j.zeros(1, 1, numFeatures)).getDouble(0);

        System.out.println("Next value in the sequence: " + predictedValues[0]);
    }
}

