package metric.correlation.analysis.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.importer.ImportException;
import org.junit.Test;

import metric.correlation.analysis.MetricCalculation;

/*
 * NEEDS TO BE RUN AS PLUGIN TEST
 */
public class ImportTest {
	private static final String MAVEN_PROJECT_PATH = "resources/maven-simple";
	private static final String GRADLE_PROJECT_PATH = "resources/gradle-simple";
	
	@Test
	public void testGradleImport() throws ImportException, IOException {
		File f = new File(GRADLE_PROJECT_PATH);
		if (!f.exists()) {
			fail("project does not exist at expected location");
		}
		IJavaProject importProject = new MetricCalculation().importProject(new File(GRADLE_PROJECT_PATH), false);
		assertNotNull(importProject);
	}
	
	@Test
	public void testMavenImport() throws ImportException, IOException {
		File f = new File(MAVEN_PROJECT_PATH);
		if (!f.exists()) {
			fail("project does not exist at expected location");
		}
		IJavaProject importProject = new MetricCalculation().importProject(new File(MAVEN_PROJECT_PATH), false);
		assertNotNull(importProject);
	}
	
	public static StringBuilder collectMessages(final Process process) throws IOException {
		final StringBuilder message = new StringBuilder();
		try (BufferedReader stream = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = stream.readLine()) != null) {
				message.append(line);
				message.append('\n');
			}
		}
		try (BufferedReader stream = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
			String line;
			while ((line = stream.readLine()) != null) {
				message.append(line);
				message.append('\n');
			}
		}
		return message;
	}

}
