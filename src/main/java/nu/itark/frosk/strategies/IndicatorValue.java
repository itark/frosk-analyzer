package nu.itark.frosk.strategies;

import java.util.List;

import nu.itark.frosk.dataset.IndicatorValues;

public interface IndicatorValue {

	/**
	 * Return indicator values
	 * 
	 * @return List<IndicatorValues>
	 * @see {@linkplain IndicatorValues}
	 */
	public List<IndicatorValues> getIndicatorValues();

}
