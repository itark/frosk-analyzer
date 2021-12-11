package nu.itark.frosk.strategies;

import java.util.ArrayList;
import java.util.List;

import nu.itark.frosk.dataset.IndicatorValue;

public interface IIndicatorValue {

	List<IndicatorValue> indicatorValues = new ArrayList<>();

	public List<IndicatorValue> getIndicatorValues();

}
