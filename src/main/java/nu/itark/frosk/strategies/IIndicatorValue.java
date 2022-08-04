package nu.itark.frosk.strategies;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.SneakyThrows;
import nu.itark.frosk.dataset.IndicatorValue;
import org.slf4j.LoggerFactory;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;

public interface IIndicatorValue {
	List<IndicatorValue> indicatorValues = new ArrayList<>();
	List<IndicatorValue> getIndicatorValues();

	default void setIndicatorValues(MACDIndicator indicator, String name) {
		IndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			long date = indicator.getBarSeries().getBar(i).getEndTime().toInstant().toEpochMilli();
			if (indicator.getValue(i).isNaN()) continue;
			long value =  indicator.getValue(i).longValue();
			iv = new IndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(StochasticOscillatorKIndicator indicator, String name) {
		IndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			long date = indicator.getBarSeries().getBar(i).getEndTime().toInstant().toEpochMilli();
			if (indicator.getValue(i).isNaN()) continue;
			long value =  indicator.getValue(i).longValue();
			iv = new IndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(EMAIndicator indicator, String name) {
		IndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			long date = indicator.getBarSeries().getBar(i).getEndTime().toInstant().toEpochMilli();
			if (indicator.getValue(i).isNaN()) continue;
			long value =  indicator.getValue(i).longValue();
			iv = new IndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

}
