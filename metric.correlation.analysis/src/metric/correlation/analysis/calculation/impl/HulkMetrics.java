package metric.correlation.analysis.calculation.impl;

import static metric.correlation.analysis.calculation.impl.HulkMetrics.MetricKeysImpl.BLOB;
import static metric.correlation.analysis.calculation.impl.HulkMetrics.MetricKeysImpl.DIT;
import static metric.correlation.analysis.calculation.impl.HulkMetrics.MetricKeysImpl.IGAM;
import static metric.correlation.analysis.calculation.impl.HulkMetrics.MetricKeysImpl.IGAT;
import static metric.correlation.analysis.calculation.impl.HulkMetrics.MetricKeysImpl.LCOM5;
import static metric.correlation.analysis.calculation.impl.HulkMetrics.MetricKeysImpl.VISIBILITY;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.hulk.HulkAPI;
import org.gravity.hulk.HulkAPI.AntiPatternNames;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HMetric;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.antipatterngraph.metrics.HDepthOfInheritanceMetric;
import org.gravity.hulk.antipatterngraph.metrics.HIGAMMetric;
import org.gravity.hulk.antipatterngraph.metrics.HIGATMetric;
import org.gravity.hulk.antipatterngraph.metrics.HLCOM5Metric;
import org.gravity.hulk.antipatterngraph.metrics.HTotalVisibilityMetric;
import org.gravity.hulk.exceptions.DetectionFailedException;
import org.gravity.tgg.modisco.pm.MoDiscoTGGActivator;
import org.gravity.typegraph.basic.TypeGraph;

import metric.correlation.analysis.calculation.IMetricCalculator;

public class HulkMetrics implements IMetricCalculator {

	private static final Logger LOGGER = Logger.getLogger(HulkMetrics.class);

	private List<HAnnotation> results = null;
	private boolean ok = false;

	/**
	 * A constructor initializing the dependencies
	 */
	public HulkMetrics() {
		MoDiscoTGGActivator.getDefault();
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public boolean calculateMetric(final IJavaProject project, final String productName, final String vendorName, final String version,
			final Map<String, String> map) {
		try {
			cleanResults();
		} catch (final IOException e) {
			LOGGER.warn("Cleaning previous results failed: " + e.getMessage(), e);
		}
		try {
			this.results = HulkAPI.detect(project, new NullProgressMonitor(), AntiPatternNames.BLOB, AntiPatternNames.IGAM,
					AntiPatternNames.IGAT, AntiPatternNames.DIT, AntiPatternNames.LCOM5,
					AntiPatternNames.TOTAL_METHOD_VISIBILITY);
		} catch (final DetectionFailedException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
		this.ok = true;
		return true;
	}

	private void cleanResults() throws IOException {
		if (this.results == null) {
			return;
		}
		final Set<Resource> resources = new HashSet<>();
		for (final HAnnotation metric : this.results) {
			resources.add(metric.eResource());
		}
		this.results.clear();
		this.results = null;
		for (final Resource resource : resources) {
			resource.delete(Collections.emptyMap());
		}
		resources.clear();
	}

	@Override
	public LinkedHashMap<String, String> getResults() {

		final LinkedHashMap<String, String> metrics = new LinkedHashMap<>();
		double igam = 0.0;
		double igat = 0.0;
		double vis = 0.0;
		double lcom = 0.0;
		double dit = 0.0;

		if (!this.ok) {
			throw new IllegalStateException("The metrics haven't been calculated successfully!");
		}
		double blob = 0.0;

		for (final HAnnotation annoatation : this.results) {

			if (annoatation instanceof HBlobAntiPattern) {
				// We count all blobs
				blob++;
			} else if (annoatation.getTAnnotated() instanceof TypeGraph) {
				/*
				 * For all metrics that are not blobs we are only interested in the values for
				 * the whole program model
				 */
				if (annoatation instanceof HIGAMMetric) {
					igam = ((HMetric) annoatation).getValue();
					LOGGER.log(Level.INFO, "IGAM = " + igam);
				} else if (annoatation instanceof HIGATMetric) {
					igat = ((HMetric) annoatation).getValue();
					LOGGER.log(Level.INFO, "IGAT = " + igat);
				} else if (annoatation instanceof HTotalVisibilityMetric) {
					vis = ((HMetric) annoatation).getValue();
				} else if (annoatation instanceof HLCOM5Metric) {
					lcom = ((HMetric) annoatation).getValue();
				} else if (annoatation instanceof HDepthOfInheritanceMetric) {
					dit = ((HMetric) annoatation).getValue();
				}
			}

		}
		metrics.put(BLOB.toString(), Double.toString(blob));
		metrics.put(IGAM.toString(), roundDouble(igam));
		metrics.put(IGAT.toString(), roundDouble(igat));
		metrics.put(VISIBILITY.toString(), roundDouble(vis));
		metrics.put(DIT.toString(), roundDouble(dit));
		metrics.put(LCOM5.toString(), roundDouble(lcom));

		return metrics;
	}

	private String roundDouble(final double d) {
		final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		final DecimalFormat dFormat = new DecimalFormat("0.00", dfs);

		return dFormat.format(d);
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toList());
	}

	/**
	 * The keys of the Hulk metrics
	 *
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
		BLOB("BLOB-Antipattern"), IGAM("IGAM"), IGAT("IGAT"), VISIBILITY("TotalMethodVisibility"), LCOM5("HulkLCOM5"),
		DIT("HulkDIT");

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
