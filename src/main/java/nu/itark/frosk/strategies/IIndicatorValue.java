package nu.itark.frosk.strategies;

import nu.itark.frosk.model.StrategyIndicatorValue;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.MinusDIIndicator;
import org.ta4j.core.indicators.adx.PlusDIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public interface IIndicatorValue {
	List<StrategyIndicatorValue> indicatorValues = new ArrayList<>();
	List<StrategyIndicatorValue> getIndicatorValues();

	default void setIndicatorValues(MACDIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(StochasticOscillatorKIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(EMAIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(SMAIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(ClosePriceIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(CCIIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(RSIIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(ParabolicSarIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}
	default void setIndicatorValues(ADXIndicator indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}

	default void setIndicatorValues(CachedIndicator<Num> indicator, String name) {
		StrategyIndicatorValue iv = null;
		for (int i = 0; i < indicator.getBarSeries().getBarCount(); i++) {
			Date date = Date.from(indicator.getBarSeries().getBar(i).getEndTime().toInstant());
			if (indicator.getValue(i).isNaN()) continue;
			BigDecimal value = BigDecimal.valueOf(indicator.getValue(i).doubleValue());
			iv = new StrategyIndicatorValue(date,value, name);
			indicatorValues.add(iv);
		}
	}




}
