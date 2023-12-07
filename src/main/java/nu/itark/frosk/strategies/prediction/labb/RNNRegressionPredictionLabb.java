package nu.itark.frosk.strategies.prediction.labb;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.graph.vertex.impl.rnn.LastTimeStepVertex;
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
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.core.io.ClassPathResource;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;


@Slf4j
public class RNNRegressionPredictionLabb {

    private static File baseDir;

    private static String securityName = "BTC-EUR";

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
        int trainSize = 20;
        int testSize = 8;
        int numberOfTimesteps = 1;

        //Prepare multi time step data, see method comments for more info
        List<String> rawStrings = prepareTrainAndTest(trainSize, testSize, numberOfTimesteps);

        //Make sure miniBatchSize is divisable by trainSize and testSize,
        //as rnnTimeStep will not accept different sized examples
        int miniBatchSize = 4;

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


        // ----- Train the network, evaluating the test set performance at each epoch -----
        int nEpochs = 100;

        for (int i = 0; i < nEpochs; i++) {
            net.fit(trainDataIter);
            trainDataIter.reset();
/*
            log.info("Epoch " + i + " complete. Time series evaluation:");
            //Run regression evaluation on our single column testDataInput
            RegressionEvaluation evaluation = new RegressionEvaluation(1);
            //Run evaluation. This is on 25k reviews, so can take some time
            while(testDataIter.hasNext()){
                DataSet testDataDataSet = testDataIter.next();
                INDArray features = testDataDataSet.getFeatures();
                INDArray lables = testDataDataSet.getLabels();
                INDArray predicted = net.output(features,false);
                evaluation.evalTimeSeries(lables,predicted);
            }
            log.info(evaluation.stats());
*/
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

        DataSet testDataDataSet = testDataIter.next();
        INDArray predictionsTestdata  = net.rnnTimeStep(testDataDataSet.getFeatures());

/*
        float[] samples = sampleFromNetwork(testDataDataSet.getFeatures(), numberOfTimesteps, net);
        System.out.println("samples:"+samples.length);
        for (int i = 0; i < samples.length; i++) {
            System.out.println("samples:"+samples[i]);
        }
*/

      //  INDArray preOutput = Nd4j.argMax(predictionsTestdata, 2);

        INDArray preOutput = predictionsTestdata.tensorAlongDimension((int) predictionsTestdata.size(2) - 1, 1, 0);    //Gets the last time step output



        System.out.println("shape: "+Nd4j.shape(predictionsTestdata));;

        normalizer.revertLabels(predictionsTestdata);


        net.rnnClearPreviousState();
        INDArray output = net.rnnTimeStep(preOutput);

        normalizer.revertLabels(output);
        RegressionEvaluation evaluation = net.evaluateRegression(testDataIter);
        testDataIter.reset();
        log.info(evaluation.stats());

        //Convert raw string data to IndArrays for plotting
        INDArray trainArray = createIndArrayFromStringList(rawStrings, 0, trainSize);
        INDArray testArray = createIndArrayFromStringList(rawStrings, trainSize, testSize);
        INDArray allDataArray = createIndArrayFromStringList(rawStrings, 0, rawStrings.size());

        //Create plot with out data
        XYSeriesCollection c = new XYSeriesCollection();
        createSeries(c, trainArray, 0, "Train data");
        createSeries(c, testArray, trainSize, "Actual test data");
        createSeries(c, predictionsTestdata, trainSize, "Predicted test data");
        createSeries2(c, output, trainSize , "Predicted output data");
   //     createSeries(c, allDataArray, 0, "Raw data");


        plotDataset(c);

        log.info("----- Example Complete -----");
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
        for (int i = 0; i < nRows; i++) {
            series.add(i + offset, data.getDouble(i));
        }

        seriesCollection.addSeries(series);

        return seriesCollection;
    }

    private static XYSeriesCollection createSeries2(XYSeriesCollection seriesCollection, INDArray data, int offset, String name) {
        int nRows = (int)data.shape()[0];
        XYSeries series = new XYSeries(name);
        for (int i = 0; i < nRows; i++) {
            series.add(i + offset, data.getDouble(i));
        }

        seriesCollection.addSeries(series);

        return seriesCollection;
    }

    private static XYSeriesCollection createSeries(XYSeriesCollection seriesCollection, List<String> rawStrings, int offset, String name) {
        XYSeries series = new XYSeries(name);
        for (int i = 0; i < rawStrings.size(); i++) {
            series.add(i + offset, Double.valueOf(rawStrings.get(i)));
        }
        seriesCollection.addSeries(series);
        return seriesCollection;
    }

    /**
     * Generate an xy plot of the datasets provided.
     */

    private static void plotDataset(XYSeriesCollection c) {

        String title = "MultiTimestepRegression:"+securityName;
        String xAxisLabel = "Timestep";
        String yAxisLabel = "Price";
        PlotOrientation orientation = PlotOrientation.VERTICAL;
        boolean legend = true;
        boolean tooltips = true;
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
        Path rawPath = Paths.get(baseDir.getAbsolutePath() + "/" + securityName + "_raw.csv");
        List<String> rawStrings = Files.readAllLines(rawPath, Charset.defaultCharset());

        log.info(securityName + "_raw.csv"+"(number of observations): "+rawStrings.size());

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
                Path path = Files.write(featuresPath,rawStrings.get(i+j).concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                System.out.println("Path: "+path);
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

    public static float[] sampleFromNetwork(INDArray priori, int numTimeSteps, MultiLayerNetwork net){
        //  int inputCount = this.getNumOfInputs();
        int inputCount = 1;
        float[] samples = new float[numTimeSteps];

        System.out.println("priori.size(1)"+priori.size(1));

/*
        if(priori.size(1) != inputCount) {
            String format = String.format("the priori should have the same number of inputs [%s] as the trained network [%s]", priori.size(1), inputCount);
            throw new RuntimeException(format);
        }
        if(priori.size(2) < inputCount) {
            String format = String.format("the priori should have enough timesteps [%s] to prime the new inputs [%s]", priori.size(2), inputCount);
            throw new RuntimeException(format);
        }
*/

        net.rnnClearPreviousState();
        INDArray output = net.rnnTimeStep(priori);

        output = output.ravel();
        // Store the output for use in the inputs
        LinkedList<Float> prevOutput = new LinkedList<>();
        for (int i = 0; i < output.length(); i++) {
            prevOutput.add(output.getFloat(0, i));
        }

        for( int i=0; i<numTimeSteps; ++i ){
            samples[i] = (prevOutput.peekLast());
            //Set up next input (single time step) by sampling from previous output
            INDArray nextInput = Nd4j.zeros(1,inputCount);

            float[] newInputs = new float[inputCount];
            newInputs[inputCount-1] = prevOutput.peekLast();
            for( int j=0; j<newInputs.length-1; j++ ) {
                newInputs[j] = prevOutput.get(prevOutput.size()-inputCount-j);
            }

            nextInput.assign(Nd4j.create(newInputs)); //Prepare next time step input

            net.rnnClearPreviousState();
            output = net.rnnTimeStep(nextInput); //Do one time step of forward pass
            // Add the output to the end of the previous output queue
            prevOutput.addLast(output.ravel().getFloat(0, output.length()-1));
        }
        return samples;
    }

}
