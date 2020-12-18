package metric.correlation.analysis.calculation;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

public interface IMetricCalculator extends Comparable<IMetricCalculator> {

	boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version, final Map<String, String> map) throws IOException;

	Map<String, String> getResults();

	Collection<String> getMetricKeys();

	Set<Class<? extends IMetricCalculator>> getDependencies();

	@Override
	default int compareTo(final IMetricCalculator other) {
		if(other.getClass().equals(getClass())) {
			return 0;
		}
		if(getDependencies().contains(other.getClass())) {
			if(other.getDependencies().contains(getClass())) {
				throw new IllegalStateException("There is a cycle in the dependencies of metric calculators");
			}
			return 1;
		}
		return -1;
	}
}
