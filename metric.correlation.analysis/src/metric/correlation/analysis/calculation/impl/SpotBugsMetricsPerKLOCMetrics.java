package metric.correlation.analysis.calculation.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IJavaProject;

import metric.correlation.analysis.calculation.IMetricCalculator;

public class SpotBugsMetricsPerKLOCMetrics implements IMetricCalculator {

	private Map<String, String> results;

	@Override
	public boolean calculateMetric(final IJavaProject project, final String productName, final String vendorName, final String version,
			final Map<String, String> map) {
		this.results = new HashMap<>();
		final String llocKey = SourceMeterMetrics.MetricKeysImpl.LLOC.toString();
		final double lloc = Double.parseDouble(map.get(llocKey));

		for (final String klockKey : getMetricKeys()) {
			final String absKey = klockKey.substring(0, klockKey.indexOf("_KLOC"));
			if (!map.containsKey(absKey)) {
				return false;
			}
			final double klockValue = (Double.parseDouble(map.get(absKey)) * 1000) / lloc;
			this.results.put(klockKey, String.valueOf(klockValue));
		}
		return true;

	}

	@Override
	public Map<String, String> getResults() {
		return this.results;
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toList());
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		final Set<Class<? extends IMetricCalculator>> dependencies = new HashSet<>();
		dependencies.add(SpotBugsMetrics.class);
		dependencies.add(SourceMeterMetrics.class);
		return dependencies;
	}

	public enum MetricKeysImpl {
		BAD_PRACTICE_KLOC("BAD_PRACTICE_KLOC"), CORRECTNESS_KLOC("CORRECTNESS_KLOC"),
		MALICIOUS_CODE_KLOC("MALICIOUS_CODE_KLOC"), INTERNATIONALIZATION_KLOC("I18N_KLOC"),
		MT_CORRECTNESS_KLOC("MT_CORRECTNESS_KLOC"), NOISE_KLOC("NOISE_KLOC"), PERFORMANCE_KLOC("PERFORMANCE_KLOC"),
		SECURITY_KLOC("SECURITY_KLOC"), STYLE_KLOC("STYLE_KLOC"), HIGH_PRIO_KLOC("HIGH_PRIO_KLOC"),
		MEDIUM_PRIO_KLOC("MEDIUM_PRIO_KLOC"), LOW_PRIO_KLOC("LOW_PRIO_KLOC");

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
