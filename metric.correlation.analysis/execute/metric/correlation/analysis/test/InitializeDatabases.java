package metric.correlation.analysis.test;

import java.io.IOException;

import org.junit.Test;

import metric.correlation.analysis.selection.GitHubProjectSelector;

public class InitializeDatabases {

	/**
	 * @author Antoniya Ivanova - initialize the CVE entry database and the projects
	 *         for analysis database.
	 * @throws IOException
	 *
	 */

	@Test
	public void initializeDatabases() throws IOException {
		// Initializes the CVE database on a running elastic client
		new metric.correlation.analysis.vulnerabilities.VulnerabilityDataImporter();

		// Initializes the project database on a running elastic client
		final GitHubProjectSelector ps = new GitHubProjectSelector();
		ps.initializeProjectElasticDatabase();
	}
}
