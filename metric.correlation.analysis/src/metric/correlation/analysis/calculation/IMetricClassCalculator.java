package metric.correlation.analysis.calculation;

import java.util.Map;

public interface IMetricClassCalculator extends IMetricCalculator {
	public Map<String, Map<String, String>> getClassResults();
}
