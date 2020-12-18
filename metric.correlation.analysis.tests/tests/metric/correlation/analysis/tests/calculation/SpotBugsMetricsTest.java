package metric.correlation.analysis.tests.calculation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.util.EclipseProjectUtil;
import org.gravity.eclipse.util.JavaProjectUtil;
import org.junit.Test;

import metric.correlation.analysis.calculation.impl.SpotBugsMetrics;


public class SpotBugsMetricsTest {
	private static final String PROJECT_PATH = "resources/testProject";

	@Test
	public void sampleProject() throws CoreException {
		final IJavaProject test = JavaProjectUtil.getJavaProject(EclipseProjectUtil.importProject(new File(PROJECT_PATH), null));
		final Map<String, String> results = new HashMap<>();
		final boolean success = new SpotBugsMetrics().calculateMetric(test, "testProject", "testVender", "1.0.0", results);
		assertTrue(success);
	}
}
