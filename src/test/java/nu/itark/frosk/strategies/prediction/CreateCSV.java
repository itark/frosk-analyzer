package nu.itark.frosk.strategies.prediction;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.analysis.DailyPriceDTO;
import nu.itark.frosk.service.BarSeriesService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

@SpringBootTest(classes = {FroskApplication.class})
@Slf4j
public class CreateCSV {

    @Autowired
    BarSeriesService barSeriesService;


    private final String FILE_PATH = "src/main/resources/rnnRegression/";

    @Test
    public final void run_multiple() throws Exception {
        String securityName = "BTC-EUR";
        File newFile = new File(FILE_PATH + securityName + "_raw.csv");
        boolean success = newFile.createNewFile();
        Path featuresPath = Paths.get(newFile.getAbsolutePath());

        BarSeries barSeries = barSeriesService.getDataSet(securityName, false);

        for (int i = 0; i < barSeries.getBarCount(); i++) {
            Bar bar = barSeries.getBar(i);
            Files.write(featuresPath,bar.getClosePrice().toString().concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }
    }

    @Test
    public final void run_single() throws Exception {
        String securityName = "BTC-EUR";
        //train
        File newFile = new File(FILE_PATH + securityName + "_train_0.csv");
        if (newFile.exists()) {
            newFile.delete();
        }
        boolean success = newFile.createNewFile();
        Path featuresPath = Paths.get(newFile.getAbsolutePath());
        BarSeries barSeries = barSeriesService.getDataSet(securityName, false);
        for (int i = 0; i < 300; i++) {
            Bar bar = barSeries.getBar(i);
            String firstClosePrice = bar.getClosePrice().toString();
            Bar bar2 = barSeries.getBar(i+1);
            String secondClosePrice = bar2.getClosePrice().toString();
            String priceRow = firstClosePrice.concat(";").concat(secondClosePrice);
            Files.write(featuresPath,priceRow.concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }
        //test
        newFile = new File(FILE_PATH + securityName + "_test_0.csv");
        if (newFile.exists()) {
            newFile.delete();
        }
        success = newFile.createNewFile();
        featuresPath = Paths.get(newFile.getAbsolutePath());
        for (int i = 300; i < 340; i++) {
            Bar bar = barSeries.getBar(i);
            String firstClosePrice = bar.getClosePrice().toString();
            Bar bar2 = barSeries.getBar(i+1);
            String secondClosePrice = bar2.getClosePrice().toString();
            String priceRow = firstClosePrice.concat(";").concat(secondClosePrice);
            Files.write(featuresPath,priceRow.concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }
    }

    @Test
    public final void run_multiple_20() throws Exception {
        String securityName = "BTC-EUR";
        File newFile = new File(FILE_PATH + securityName + "_raw.csv");
        if (newFile.exists()) {
            newFile.delete();
        }
        boolean success = newFile.createNewFile();
        Path featuresPath = Paths.get(newFile.getAbsolutePath());
        BarSeries barSeries = barSeriesService.getDataSet(securityName, false);
        for (int i = 0; i < 20; i++) {
            Bar bar = barSeries.getBar(i);
            Files.write(featuresPath,bar.getClosePrice().toString().concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }
    }

    @Test
    public final void run_multiple_date() throws Exception {
        String securityName = "BTC-EUR";
        File newFile = new File(FILE_PATH + securityName + "_raw_with_date.csv");
        if (newFile.exists()) {
            newFile.delete();
        }
        boolean success = newFile.createNewFile();
        Path featuresPath = Paths.get(newFile.getAbsolutePath());
        BarSeries barSeries = barSeriesService.getDataSet(securityName, false);
        for (int i = (barSeries.getBarCount() - 10); i < barSeries.getBarCount(); i++) {
            Bar bar = barSeries.getBar(i);
            String price = bar.getClosePrice().toString();
            String date = bar.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String datePrice = date.concat(",").concat(price);
            Files.write(featuresPath,  datePrice.concat(System.lineSeparator()).getBytes()  , StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }
    }

    @Test
    public void testND4J() {
        INDArray random = Nd4j.rand(3, 3,3);
        System.out.println("random:");
        System.out.println(random);
        INDArray lastTwoRows = random.get(NDArrayIndex.interval(1,2,true),NDArrayIndex.all());
        System.out.println("lastTwoRows");
        System.out.println(lastTwoRows);

    }

}
