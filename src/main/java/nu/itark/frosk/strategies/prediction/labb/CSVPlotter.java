package nu.itark.frosk.strategies.prediction.labb;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

/**
 * Read a csv file. Fit and plot the data using Deeplearning4J.
 *
 * @author Robert Altena
 */
public class CSVPlotter {

	private static File baseDir;

	static {
		try {
			baseDir = new org.springframework.core.io.ClassPathResource("rnnRegression").getFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


    public static void main( String[] args ) throws IOException, InterruptedException
    {
		Path rawPath = Paths.get(baseDir.getAbsolutePath() + "/BTC-EUR_raw.csv");
		//Path rawPath = Paths.get(baseDir.getAbsolutePath() + "/BTC-EUR_raw_with_date.csv");
    	DataSet ds = readCSVDataset(rawPath.toFile().getAbsolutePath());
    	ArrayList<DataSet> DataSetList = new ArrayList<>();
    	DataSetList.add(ds);

    	plotDataset(DataSetList); //Plot the data, make sure we have the right data.

    	MultiLayerNetwork net =fitStraightline(ds);
    	// Get the min and max x values, using Nd4j
    	NormalizerMinMaxScaler preProcessor = new NormalizerMinMaxScaler();
    	preProcessor.fit(ds);
        int nSamples = 50;
        INDArray x = Nd4j.linspace(preProcessor.getMin().getInt(0),preProcessor.getMax().getInt(0),nSamples).reshape(nSamples, 1);
        INDArray y = net.output(x);
        DataSet modeloutput = new DataSet(x,y);
        DataSetList.add(modeloutput);

    	plotDataset(DataSetList);    //Plot data and model fit.

    }

	/**
	 * Fit a straight line using a neural network.
	 * @param ds The dataset to fit.
	 * @return The network fitted to the data
	 */
	private static MultiLayerNetwork fitStraightline(DataSet ds){
		int seed = 12345;
		int iterations = 1;
		int nEpochs = 200;
		double learningRate = 0.00001;
		int numInputs = 1;
	    int numOutputs = 1;

	    //
	    // Hook up one input to the one output.
	    // The resulting model is a straight line.
	    //
		MultiLayerConfiguration conf = new  NeuralNetConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numOutputs)
                        .activation(Activation.IDENTITY)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(numOutputs).nOut(numOutputs).build())
				.build();

		MultiLayerNetwork net = new MultiLayerNetwork(conf);
		net.init();
	    net.setListeners(new ScoreIterationListener(1));

	    for( int i=0; i<nEpochs; i++ ){
	    	net.fit(ds);
	    }

	    return net;
	}

    /**
     * Read a CSV file into a dataset.
     *
     * Use the correct constructor:
     * DataSet ds = new RecordReaderDataSetIterator(rr,batchSize);
     * returns the data as follows:
     * ===========INPUT===================
     *[[12.89, 22.70],
     * [19.34, 20.47],
     * [16.94,  6.08],
     *  [15.87,  8.42],
     *  [10.71, 26.18]]
     *
     *  Which is not the way the framework likes its data.
     *
     *  This one:
     *   RecordReaderDataSetIterator(rr,batchSize, 1, 1, true);
     *   returns
     *   ===========INPUT===================
     * [12.89, 19.34, 16.94, 15.87, 10.71]
     * =================OUTPUT==================
     * [22.70, 20.47,  6.08,  8.42, 26.18]
     *
     *  This can be used as is for regression.
     */
	private static DataSet readCSVDataset(String filename) throws IOException, InterruptedException{
		int batchSize = 1000;
		RecordReader rr = new CSVRecordReader();
		rr.initialize(new FileSplit(new File(filename)));

		DataSetIterator iter =  new RecordReaderDataSetIterator(rr,batchSize, 1, 1, true);
		return iter.next();
	}


	private static void plotTimeseriesDataset(BarSeries series) {

		TimeSeriesCollection xyDataset = createAdditionalDataset(series);


	}


	private static TimeSeriesCollection createAdditionalDataset(BarSeries series) {
		ClosePriceIndicator indicator = new ClosePriceIndicator(series);
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries("Btc price");
		for (int i = 0; i < series.getBarCount(); i++) {
			Bar bar = series.getBar(i);
			chartTimeSeries.add(new Second(new Date(bar.getEndTime().toEpochSecond() * 1000)),
					indicator.getValue(i).doubleValue());
		}
		dataset.addSeries(chartTimeSeries);
		return dataset;
	}



	/**
	 * Generate an xy plot of the datasets provided.
	 */
	private static void plotDataset(ArrayList<DataSet> DataSetList){

		XYSeriesCollection c = new XYSeriesCollection();

		int dscounter = 1; //use to name the dataseries
		for (DataSet ds : DataSetList)
		{
			INDArray features = ds.getFeatures();
			INDArray outputs= ds.getLabels();

			int nRows = features.rows();
			XYSeries series = new XYSeries("S" + dscounter);
			for( int i=0; i<nRows; i++ ){
				series.add(features.getDouble(i), outputs.getDouble(i));
			}

			c.addSeries(series);
		}

        String title = "title";
		String xAxisLabel = "xAxisLabel";
		String yAxisLabel = "yAxisLabel";
		PlotOrientation orientation = PlotOrientation.VERTICAL;
		boolean legend = false;
		boolean tooltips = false;
		boolean urls = false;
		JFreeChart chart = ChartFactory.createScatterPlot(title , xAxisLabel, yAxisLabel, c, orientation , legend , tooltips , urls);
    	JPanel panel = new ChartPanel(chart);

    	 JFrame f = new JFrame();
    	 f.add(panel);
    	 f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
         f.pack();
         f.setTitle("Training Data");

         f.setVisible(true);
	}
}
