package nu.itark.frosk.strategies.prediction.labb;

import org.datavec.api.records.reader.impl.collection.CollectionRecordReader;
import org.datavec.api.records.reader.impl.collection.ListStringRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.BaseTrainingListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LSTMPredictionExample {

    public static void main(String[] args) throws IOException, InterruptedException {
        // Set up the UI server for monitoring training progress
/*        UIServer uiServer = UIServer.getInstance();
        InMemoryStatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);*/

        int numEpochs = 100;
        int miniBatchSize = 1;

        // Define the time series data
        List<Double> timeSeries = new ArrayList<>();
        timeSeries.add(1.0);
        timeSeries.add(3.0);
        timeSeries.add(5.0);
        timeSeries.add(7.0);
        timeSeries.add(9.0);

        // Create a data set for training
 /*       CollectionRecordReader recordReader = new CollectionRecordReader();
        recordReader.initialize(new CollectionRecordReader.Collections(timeSeries));
 */       DataSetIterator iterator = new RecordReaderDataSetIterator(null, miniBatchSize, -1, 1);

        // Define the LSTM network configuration
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(0.01))
                .list()
                .layer(new LSTM.Builder()
                        .nIn(1)
                        .nOut(10)
                        .activation(Activation.TANH)
                        .build())
                .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(10)
                        .nOut(1)
                        .build())
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(10));
        net.setListeners(new BaseTrainingListener[] {new BaseTrainingListener(){public void onEpochEnd(MultiLayerNetwork net, int epoch) {
            System.out.println("Epoch " + epoch + " completed.");
        }}});

        // Train the LSTM network
        for (int i = 0; i < numEpochs; i++) {
            net.fit(iterator);
            iterator.reset();
        }

        // Generate predictions
        int steps = 5;
        INDArray input = Nd4j.create(new double[]{9.0}); // Initialize with the last value in the series
        System.out.println("Predicted values:");

        for (int i = 0; i < steps; i++) {
            INDArray predicted = net.rnnTimeStep(input);
            System.out.println(predicted.getDouble(0));
            input = predicted;
        }

        // Shutdown the UI server
        TimeUnit.SECONDS.sleep(10);
  //      uiServer.stop();
    }
}

