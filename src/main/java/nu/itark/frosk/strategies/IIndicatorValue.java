package nu.itark.frosk.strategies;

import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import nu.itark.frosk.dataset.IndicatorValue;
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
