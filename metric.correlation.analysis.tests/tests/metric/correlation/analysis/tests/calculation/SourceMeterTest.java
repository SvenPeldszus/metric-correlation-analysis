package metric.correlation.analysis.tests.calculation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.util.EclipseProjectUtil;
import org.gravity.eclipse.util.JavaProjectUtil;
import org.junit.Before;
import org.junit.Test;

import metric.correlation.analysis.calculation.MetricCalculatorInitializationException;
import metric.correlation.analysis.calculation.impl.SourceMeterMetrics;

public class SourceMeterTest {
	private SourceMeterMetrics sm;
	private static final String PROJECT_PATH = "resources/testProject";

	@Before
	public void init() {
		final String sourcemeter = System.getenv(SourceMeterMetrics.ENV_VARIABLE_NAME);
		if (sourcemeter == null) {
			return;
		}
		try {
			this.sm = new SourceMeterMetrics();
		} catch (final MetricCalculatorInitializationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void sampleProject() throws CoreException {
		if(this.sm == null) {
			// Skip test
			return;
		}
		final Map<String, String> expected = new HashMap<>();
		expected.put("DIT", "0.00");
		expected.put("CBO", "0.00");
		expected.put("LDC", "0.00");
		expected.put("LLOC", "5.00");
		expected.put("WMC", "1.00");
		expected.put("LCOM5", "1.00");
		expected.put("LOCpC", "5.00");
		final IJavaProject test = JavaProjectUtil.getJavaProject(EclipseProjectUtil.importProject(new File(PROJECT_PATH), null));
		final Map<String, String> results = new HashMap<>();
		final boolean success = this.sm.calculateMetric(test, "testProject", "testVender", "1.0.0", results);
		assertTrue(success);
		assertEquals(expected, this.sm.getResults());
	}

	@Test(expected = IllegalStateException.class)
	public void getResultWithoutCalcuationError() {
		if(this.sm == null) {
			// Skip test
			throw new IllegalStateException();
		}
		this.sm.getResults();
	}

}
