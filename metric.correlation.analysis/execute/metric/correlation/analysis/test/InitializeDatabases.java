package metric.correlation.analysis.test;

import org.junit.Test;

import metric.correlation.analysis.selection.ProjectSelector;

public class InitializeDatabases {
	
	/**
	 * @author Antoniya Ivanova - initialize the CVE entry database and the projects
	 *         for analysis database.
	 *
	 */

	@Test
	public void initializeDatabases() {
		// Initializes the CVE database on a running elastic client
		new metric.correlation.analysis.vulnerabilities.VulnerabilityDataImporter();
		
		// Initializes the project database on a running elastic client
		ProjectSelector ps = new ProjectSelector();
		ps.initializeProjectElasticDatabase();
	}
}
