package metric.correlation.analysis.calculation.impl;

import static metric.correlation.analysis.calculation.impl.VulnerabilitiesPerKLOCMetrics.MetricKeysImpl.VULNERABIITIES_PER_KLOC;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

import metric.correlation.analysis.calculation.IMetricCalculator;

public class VulnerabilitiesPerKLOCMetrics implements IMetricCalculator {

	private Map<String, String> results;

	@Override
	public boolean calculateMetric(final IJavaProject project, final String productName, final String vendorName, final String version,
			final Map<String, String> map) {
		final String llocKey = SourceMeterMetrics.MetricKeysImpl.LLOC.toString();
		final String numberOfVulnerabilitiesKey = CVEMetrics.MetricKeysImpl.NUMBER_OF_VULNERABILITIES.toString();
		if (!map.containsKey(llocKey) || !map.containsKey(numberOfVulnerabilitiesKey)) {
			return false;
		}
		final double lloc = Double.parseDouble(map.get(llocKey));
		final double numberOfVulnerabilities = Double.parseDouble(map.get(numberOfVulnerabilitiesKey));

		this.results = Collections.singletonMap(VULNERABIITIES_PER_KLOC.toString(), Double.toString((numberOfVulnerabilities / lloc) * 1000));
		return true;

	}

	@Override
	public Map<String, String> getResults() {
		return this.results;
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Collections.singleton(VULNERABIITIES_PER_KLOC.toString());
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		final Set<Class<? extends IMetricCalculator>> dependencies = new HashSet<>();
		dependencies.add(CVEMetrics.class);
		dependencies.add(SourceMeterMetrics.class);
		return dependencies;
	}

	/**
	 * The keys of the relative metric calculator metrics
	 *
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
		VULNERABIITIES_PER_KLOC("VulnerabiitiesPerKLOC");

		private final String value;

		MetricKeysImpl(final String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}
}
