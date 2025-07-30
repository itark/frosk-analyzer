package nu.itark.frosk.util;

import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import static java.math.RoundingMode.FLOOR;

public class FroskUtil {


    static public String percent(double value) {
        NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
        format.setMinimumFractionDigits(1);
        return format.format(value);
    }

    static public BigDecimal percentOBS(double value) {
        NumberFormat format = NumberFormat.getPercentInstance(Locale.getDefault());
        format.setMinimumFractionDigits(1);
        double raw;
        if (value > 1) {
            raw = value -1;
        } else {
            raw = value - 100;
        }
        value = raw  * 100;
        return new BigDecimal(value).setScale(2, RoundingMode.DOWN);
    }


    static public BigDecimal getPercentage(BigDecimal initValue, BigDecimal targetValue) {
        if (initValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        try {
            return  ((targetValue.subtract(initValue))
                    .divide(initValue, 4, FLOOR))
                    .multiply(BigDecimal.valueOf(100L));
        } catch (Exception e) {
            throw new RuntimeException("initValue:"+initValue,e);
        }
    }




}
