package nu.itark.frosk.service;

import java.util.List;

import org.springframework.stereotype.Service;

import nu.itark.frosk.analysis.ChartValueDTO;

@Service
public class ChartValuesService {

	/**
	 * This method creates values for rendering on UI-chart.
	 * 
	 * @param strategy
	 * @param security
	 * @return
	 */
	public List<ChartValueDTO> getChartValues(String strategy, String security) {
		//TODO
		return null;
		
		
	}
	
}
