package nu.itark.frosk.strategies.prediction.labb;

import org.apache.commons.io.FileUtils;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;
import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;


/**
 * This example was inspired by Jason Brownlee's regression examples for Keras, found here:
 * http://machinelearningmastery.com/time-series-prediction-lstm-recurrent-neural-networks-python-keras/
 *
 * It demonstrates multi time step regression using LSTM
 */

public class MultiTimestepRegressionExample {

    //https://deeplearning4j.konduit.ai/

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTimestepRegressionExample.class);

    private static File baseDir;

    static {
        try {
            baseDir = new ClassPathResource("rnnRegression").getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File baseTrainDir = new File(baseDir, "multiTimestepTrain");
    private static File featuresDirTrain = new File(baseTrainDir, "features");
    private static File labelsDirTrain = new File(baseTrainDir, "labels");
    private static File baseTestDir = new File(baseDir, "multiTimestepTest");
    private static File featuresDirTest = new File(baseTestDir, "features");
    private static File labelsDirTest = new File(baseTestDir, "labels");


    public static void main(String[] args) throws Exception {

        //Set number of examples for training, testing, and time steps
        int trainSize = 100;
        int testSize = 20;
        int numberOfTimesteps = 20;

        //Prepare multi time step data, see method comments for more info
        List<String> rawStrings = prepareTrainAndTest(trainSize, testSize, numberOfTimesteps);

        //Make sure miniBatchSize is divisable by trainSize and testSize,
        //as rnnTimeStep will not accept different sized examples
        int miniBatchSize = 10;

        // ----- Load the training data -----
        SequenceRecordReader trainFeatures = new CSVSequenceRecordReader();
        trainFeatures.initialize(new NumberedFileInputSplit(featuresDirTrain.getAbsolutePath() + "/train_%d.csv", 0, trainSize-1));
        SequenceRecordReader trainLabels = new CSVSequenceRecordReader();
        trainLabels.initialize(new NumberedFileInputSplit(labelsDirTrain.getAbsolutePath() + "/train_%d.csv", 0, trainSize-1));

        DataSetIterator trainDataIter = new SequenceRecordReaderDataSetIterator(trainFeatures, trainLabels, miniBatchSize, -1, true, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);

        //Normalize the training data
        NormalizerMinMaxScaler normalizer = new NormalizerMinMaxScaler(0, 1);
        normalizer.fitLabel(true);
        normalizer.fit(trainDataIter);              //Collect training data statistics
        trainDataIter.reset();


        // ----- Load the test data -----
        //Same process as for the training data.
        SequenceRecordReader testFeatures = new CSVSequenceRecordReader();
        testFeatures.initialize(new NumberedFileInputSplit(featuresDirTest.getAbsolutePath() + "/test_%d.csv", trainSize, trainSize+testSize-1));
        SequenceRecordReader testLabels = new CSVSequenceRecordReader();
        testLabels.initialize(new NumberedFileInputSplit(labelsDirTest.getAbsolutePath() + "/test_%d.csv", trainSize, trainSize+testSize-1));

        DataSetIterator testDataIter = new SequenceRecordReaderDataSetIterator(testFeatures, testLabels, miniBatchSize, -1, true, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);

        trainDataIter.setPreProcessor(normalizer);
        testDataIter.setPreProcessor(normalizer);


        // ----- Configure the network -----
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(140)
                .weightInit(WeightInit.XAVIER)
                .updater(new Nesterovs(0.15, 0.9))
                .list()
                .layer(0, new LSTM.Builder().activation(Activation.TANH).nIn(1).nOut(10)
                        .build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY).nIn(10).nOut(1).build())
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
    //    net.setListeners(new ScoreIterationListener(20));

        // ----- Train the network, evaluating the test set performance at each epoch -----
        int nEpochs = 50;

        for (int i = 0; i < nEpochs; i++) {
            net.fit(trainDataIter);
            trainDataIter.reset();
           // LOGGER.info("Epoch " + i + " complete. Time series evaluation:");

            //Run regression evaluation on our single column input
            org.nd4j.evaluation.regression.RegressionEvaluation evaluation = new RegressionEvaluation(1);

            //Run evaluation. This is on 25k reviews, so can take some time
/*
            while(testDataIter.hasNext()){
                DataSet t = testDataIter.next();
                INDArray features = t.getFeatures();
                INDArray lables = t.getLabels();
                INDArray predicted = net.output(features,false);

                evaluation.evalTimeSeries(lables,predicted);
            }
*/

           // System.out.println(evaluation.stats());

            testDataIter.reset();
        }

        /**
         * All code below this point is only necessary for plotting
         */

        //Init rrnTimeStemp with train data and predict test data

        while (trainDataIter.hasNext()) {
            DataSet t = trainDataIter.next();
            net.rnnTimeStep(t.getFeatures());
        }

        trainDataIter.reset();

        DataSet t = testDataIter.next();
        INDArray predicted  = net.rnnTimeStep(t.getFeatures());

        DataSet tt = trainDataIter.next();
        INDArray future  = net.rnnTimeStep(tt.getFeatures());

        INDArray output = net.feedForward(tt.getFeatures()).get(0);

        // Generate future
/*
        int nRows = (int)predicted.shape()[2];
        double predictedLast = predicted.getDouble(nRows-1);
        int steps = 5;
        INDArray input = Nd4j.create(new double[]{predictedLast}); // Initialize with the last value in the series
        System.out.println("Predicted values:");
        for (int i = 0; i < steps; i++) {
            INDArray future = net.rnnTimeStep(input);
            System.out.println(predicted.getDouble(i));
            input = future;
        }
*/


        normalizer.revertLabels(predicted);
        normalizer.revertLabels(future);
        normalizer.revertLabels(output);


        //Convert raw string data to IndArrays for plotting
        INDArray trainArray = createIndArrayFromStringList(rawStrings, 0, trainSize);
        INDArray testArray = createIndArrayFromStringList(rawStrings, trainSize, testSize);

        //Create plot with out data

        XYSeriesCollection c = new XYSeriesCollection();
        createSeries(c, trainArray, 0, "Train data");
        createSeries(c, testArray, trainSize, "Actual test data");
        createSeries(c, predicted, trainSize, "Predicted test data");
        createSeries(c, future, trainSize, "Future data");

        final float trainArrayLast = trainArray.getFloat(trainArray.length()-1);
        System.out.println("trainArrayLast:"+trainArrayLast);
        final float testArrayLast = testArray.getFloat(testArray.length()-1);
        System.out.println("testArrayLast:"+testArrayLast);


        int nRows = (int)predicted.shape()[2];
        double predictedLast = predicted.getDouble(nRows-1);
        System.out.println("predictedLast2:"+predictedLast);


        int nRows2 = (int)future.shape()[2];
        double futureLast = future.getDouble(nRows2-1);
        System.out.println("futureLast:"+futureLast);

        int nRows3 = (int)future.shape()[2];
        double outputLast = output.getDouble(nRows3-1);
        System.out.println("outputLast:"+futureLast);


        plotDataset(c);

        LOGGER.info("----- Example Complete -----");
    }


    /**
     * Creates an IndArray from a list of strings
     * Used for plotting purposes
     */
    private static INDArray createIndArrayFromStringList(List<String> rawStrings, int startIndex, int length) {
        List<String> stringList = rawStrings.subList(startIndex,startIndex+length);
        double[] primitives = new double[stringList.size()];

        for (int i = 0; i < stringList.size(); i++) {
            primitives[i] = Double.valueOf(stringList.get(i));
        }

        return Nd4j.create(new int[]{1,length},primitives);
    }

    /**
     * Used to create the different time series for ploting purposes
     */

    private static XYSeriesCollection createSeries(XYSeriesCollection seriesCollection, INDArray data, int offset, String name) {
        int nRows = (int)data.shape()[2];
        XYSeries series = new XYSeries(name);
        //System.out.println("**********name:"+name);
        for (int i = 0; i < nRows; i++) {
            //System.out.println(data.getDouble(i));
            series.add(i + offset, data.getDouble(i));
        }
        seriesCollection.addSeries(series);


        return seriesCollection;
    }


    /**
     * Generate an xy plot of the datasets provided.
     */

    private static void plotDataset(XYSeriesCollection c) {

        String title = "MultiTimestepRegression example";
        String xAxisLabel = "Timestep";
        String yAxisLabel = "Number of passengers";
        PlotOrientation orientation = PlotOrientation.VERTICAL;
        boolean legend = true;
        boolean tooltips = false;
        boolean urls = false;
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, c, orientation, legend, tooltips, urls);

        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();

        // Auto zoom to fit time series in initial window
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(true);

        JPanel panel = new ChartPanel(chart);

        JFrame f = new JFrame();
        f.add(panel);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.pack();
        f.setTitle("Training Data");

        RefineryUtilities.centerFrameOnScreen(f);
        f.setVisible(true);
    }

    /**
     * This method shows how you based on a CSV file can preprocess your data the structure expected for a
     * multi time step problem. This examples uses a single column CSV as input, but the example should be easy to modify
     * for use with a multi column input as well.
     * @return
     * @throws IOException
     */
    private static List<String> prepareTrainAndTest(int trainSize, int testSize, int numberOfTimesteps) throws IOException {
        Path rawPath = Paths.get(baseDir.getAbsolutePath() + "/passengers_raw.csv");

        List<String> rawStrings = Files.readAllLines(rawPath, Charset.defaultCharset());

        //Remove all files before generating new ones

        FileUtils.cleanDirectory(featuresDirTrain);
        FileUtils.cleanDirectory(labelsDirTrain);
        FileUtils.cleanDirectory(featuresDirTest);
        FileUtils.cleanDirectory(labelsDirTest);


        for (int i = 0; i < trainSize; i++) {
            Path featuresPath = Paths.get(featuresDirTrain.getAbsolutePath() + "/train_" + i + ".csv");
            Path labelsPath = Paths.get(labelsDirTrain + "/train_" + i + ".csv");
            int j;
            for (j = 0; j < numberOfTimesteps; j++) {
                Files.write(featuresPath,rawStrings.get(i+j).concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            }
            Files.write(labelsPath,rawStrings.get(i+j).concat(System.lineSeparator()).getBytes(),StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }

        for (int i = testSize; i < testSize+trainSize; i++) {
            Path featuresPath = Paths.get(featuresDirTest + "/test_" + i + ".csv");
            Path labelsPath = Paths.get(labelsDirTest + "/test_" + i + ".csv");
            int j;
            for (j = 0; j < numberOfTimesteps; j++) {
                Files.write(featuresPath,rawStrings.get(i+j).concat(System.lineSeparator()).getBytes(),StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            }
            Files.write(labelsPath,rawStrings.get(i+j).concat(System.lineSeparator()).getBytes(),StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }

        return rawStrings;
    }
}
