package nu.itark.frosk.strategies.util;

import nu.itark.frosk.analysis.IconManager;
import nu.itark.frosk.util.FroskUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJFroskUtil {



    @Test
    public void testPercent() {
        //double decimalFormat= 1.17;
        //double decimalFormat= 5.47;
        double decimalFormat= 0.47;
       String percent = FroskUtil.percent(decimalFormat);
        System.out.println("percent:"+percent);

        NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
        format.setMinimumFractionDigits(1);
        System.out.println("format.format(percent):"+format.format(decimalFormat));;

    }

}
