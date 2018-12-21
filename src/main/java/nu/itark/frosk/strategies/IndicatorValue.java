package nu.itark.frosk.strategies;

import java.util.List;
import java.util.Set;

import nu.itark.frosk.model.StrategyIndicatorValue;

public interface IndicatorValue {

	/**
	 * Return indicator values
	 * 
	 * @return List<StrategyIndicatorValue>
	 * @see {@linkplain StrategyIndicatorValue}
	 */
	public Set<StrategyIndicatorValue> getIndicatorValues();
	

	public List<StrategyIndicatorValue> getIndicatorValues2();	
	

}
