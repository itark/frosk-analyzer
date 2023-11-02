package nu.itark.frosk.strategies.prediction.labb;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

public class LSTMTimeseriesPrediction {
    public static void main(String[] args) {
        // Define your time series data
        double[] timeSeries = {1, 3, 5, 7, 9};

        // Set the number of time steps for the LSTM
        int numTimeSteps = timeSeries.length;

        // Define the number of input features and output features
        int numInputFeatures = 1;
        int numOutputFeatures = 1;

        // Create training data
        INDArray input = Nd4j.create(timeSeries);
        //INDArray labels = Nd4j.create(timeSeries).get(NDArrayIndex.interval(1, numTimeSteps));
        INDArray labels = Nd4j.create(timeSeries);


        // Define a configuration for the LSTM network
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123) // Set a random seed for reproducibility
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(new LSTM.Builder()
                        .nIn(numInputFeatures)
                        .nOut(10) // Number of LSTM units
                        .activation(Activation.TANH)
                        .build())
                .layer(new LSTM.Builder()
                        .nIn(10)
                        .nOut(10)
                        .activation(Activation.TANH)
                        .build())
                .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(10)
                        .nOut(numOutputFeatures)
                        .build())
               // .pretrain(false)
              // .backprop(true)
                .build();

        // Create the LSTM network
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(100));

        // Create a DataSetIterator for training data
        DataSet dataSet = new DataSet(input, labels);
        DataSetIterator iterator = new ListDataSetIterator<>(dataSet.asList(), 1);

        // Train the network
        int numEpochs = 10;
        for (int i = 0; i < numEpochs; i++) {
            net.fit(iterator);
            iterator.reset();
        }

        // Make predictions for future time steps
        INDArray futureInput = Nd4j.create(new double[] {9});
        for (int i = 0; i < 5; i++) {
            INDArray output = net.rnnTimeStep(futureInput);
            System.out.println("Predicted: " + output.getDouble(0));
            futureInput = output;
        }
    }
}

