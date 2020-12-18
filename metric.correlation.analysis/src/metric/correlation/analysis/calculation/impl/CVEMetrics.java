package metric.correlation.analysis.calculation.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.eclipse.jdt.core.IJavaProject;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.MetricCalculatorInitializationException;
import metric.correlation.analysis.vulnerabilities.VulnerabilityDataQueryHandler;

public class CVEMetrics implements IMetricCalculator {

	private Map<String, String> results;

	public CVEMetrics() throws MetricCalculatorInitializationException {
		// Check if ES is running
		try (RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("localhost", 9200, "http")))) {

			client.info(RequestOptions.DEFAULT);

		} catch (final IOException e) {
			throw new MetricCalculatorInitializationException("ElasticSearch isn't running!");
		}
	}

	@Override
	public boolean calculateMetric(final IJavaProject project, final String productName, final String vendorName, final String version,
			final Map<String, String> map) {
		final VulnerabilityDataQueryHandler handler = new VulnerabilityDataQueryHandler();
		this.results = handler.getMetrics(handler.getVulnerabilities(productName, vendorName, version, "TWO"));
		return !this.results.isEmpty();
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
		return Collections.emptySet();
	}

	/**
	 * The keys of the CVE metrics
	 *
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
		AVERAGE_CVSS3("AverageCVSS3"),
		AVERAGE_CVSS2("AverageCVSS2"),
		MAX_CVSS3("MaxCVSS3"),
		MAX_CVSS2("MaxCVSS2"),
		NUMBER_OF_VULNERABILITIES("NumberOfVulnerabilities");

		private String value;

		MetricKeysImpl(final String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}
}