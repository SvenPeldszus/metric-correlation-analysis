package metric.correlation.analysis.test;

import java.io.IOException;

import metric.correlation.analysis.selection.GitHubProjectSelector;
import metric.correlation.analysis.vulnerabilities.VulnerabilityDataImporter;

public class InitializeDatabases {

	/**
	 * @author Antoniya Ivanova - initialize the CVE entry database and the projects
	 *         for analysis database.
	 * @throws IOException
	 *
	 */
	public static void main(final String[] args) throws IOException {
		// Initializes the CVE database on a running elastic client
		new VulnerabilityDataImporter().addCVEsToElastic();

		// Initializes the project database on a running elastic client
		try (final var selector = new GitHubProjectSelector()) {
			selector.initializeProjectElasticDatabase(-1);
		}
	}
}
