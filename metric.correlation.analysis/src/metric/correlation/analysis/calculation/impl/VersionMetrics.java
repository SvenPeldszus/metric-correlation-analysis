package metric.correlation.analysis.calculation.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IJavaProject;
import metric.correlation.analysis.calculation.IMetricCalculator;

/**
 * Stores the project version as result
 * 
 * @author speldszus
 *
 */
public class VersionMetrics implements IMetricCalculator {

	private HashMap<String, String> results = new HashMap<>();

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version,
			final Map<String, String> map) {
		results.put(MetricKeysImpl.VERSION.toString(), version);
		results.put(MetricKeysImpl.PRODUCT.toString(), productName);
		results.put(MetricKeysImpl.VENDOR.toString(), vendorName);
		return !results.isEmpty();
	}

	@Override
	public HashMap<String, String> getResults() {
		return results;
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).parallelStream().map(Object::toString).collect(Collectors.toList());
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		return Collections.emptySet();
	}

	/**
	 * The keys of the version metrics
	 * 
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
		VERSION("version"), 
		PRODUCT("product"), 
		VENDOR("vendor");
		
		private String value;

		private MetricKeysImpl(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}