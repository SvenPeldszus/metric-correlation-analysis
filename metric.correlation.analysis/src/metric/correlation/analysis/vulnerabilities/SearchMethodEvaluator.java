package metric.correlation.analysis.vulnerabilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

public class SearchMethodEvaluator {

	private static final Logger LOGGER = Logger.getLogger(SearchMethodEvaluator.class);

	private static final String controlResultsFileLocation = "SearchMethodComparisonResources/bt-data-githubname-cve-v3.0.csv";
	private final VulnerabilityDataQueryHandler vdqh = new VulnerabilityDataQueryHandler();

	/**
	 * A class for representing search results for the vulnerability search
	 *
	 * @param vendorName  the name of the product's vendor to search for
	 * @param productName the name of the project/product to search for
	 * @param cveIDs      a list of found CVE's for this project with their CVE
	 *                    ID's.
	 */
	class VulnerabilitySearchResult implements Comparable<VulnerabilitySearchResult> {

		private final String vendorName;
		private final String productName;
		private final List<String> cveIDs;

		public VulnerabilitySearchResult(final String vendorName, final String productName,
				final List<String> cves) {
			this.vendorName = vendorName;
			this.productName = productName;
			this.cveIDs = cves;
		}

		@Override
		public int compareTo(final VulnerabilitySearchResult vulnerabilityRes) {
			return equals(vulnerabilityRes) ? 1 : 0;
		}

		@Override
		public boolean equals(final Object other) {
			if (other instanceof VulnerabilitySearchResult) {
				final VulnerabilitySearchResult vulnerabilityRes = (VulnerabilitySearchResult) other;
				final int firstListSize = this.cveIDs.size();
				final int secondListSize = vulnerabilityRes.getCveIDs().size();
				if (firstListSize != secondListSize) {
					return false;
				}

				boolean allVulnerabilitiesMatch = false;
				int counter = 0;

				for (final String cveId : this.cveIDs) {
					if (vulnerabilityRes.getCveIDs().contains(cveId)) {
						counter++;
					}
				}

				if (counter == firstListSize) {
					allVulnerabilitiesMatch = true;
				}

				return ((vulnerabilityRes.getProductName().equals(this.productName)) && allVulnerabilitiesMatch);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		public String getProductName() {
			return this.productName;
		}

		public String getVendorName() {
			return this.vendorName;
		}

		public List<String> getCveIDs() {
			return this.cveIDs;
		}

		@Override
		public String toString() {
			return this.productName + this.cveIDs.toString();
		}

	}

	/**
	 * Import the CVE test data for comparison from a given CSV file.
	 *
	 * @return an ArrayList of {@link VulnerabilitySearchResult}, which is defined
	 *         by the oracle
	 */
	private ArrayList<VulnerabilitySearchResult> readControlResultCSVData() {
		final ArrayList<VulnerabilitySearchResult> controlResults = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(controlResultsFileLocation))) {
			String line;
			while ((line = br.readLine()) != null) {
				final String[] vsrString = line.split(",");
				final ArrayList<String> cves = new ArrayList<>();

				for (int i = 2; i < vsrString.length; i++) {
					cves.add(vsrString[i].replace("\"", "").replace(" ", ""));
				}

				controlResults
				.add(new VulnerabilitySearchResult(vsrString[0].toLowerCase().replace("-", "").replace("_", ""),
						vsrString[1].toLowerCase().replace("-", "").replace("_", ""), cves));
			}
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return controlResults;
	}

	/**
	 * Get the a search result containing the project name and a list of CVEs.
	 *
	 * @param product the product name, for which the vulnerabilities should be
	 *                sought.
	 * @return A single vulnerability search result.
	 */
	private VulnerabilitySearchResult getCVEsOfProduct(final String product, final String vendor, final String version,
			final String fuzzyness) {
		final HashSet<SearchHit> results = this.vdqh.getVulnerabilities(product, vendor, version, fuzzyness);
		final ArrayList<String> cveIDs = new ArrayList<>();

		if (!results.isEmpty()) {
			for (final SearchHit searchHit : results) {
				final Map<String, Object> searchHitMap = searchHit.getSourceAsMap();
				cveIDs.add((String) searchHitMap.get("ID"));
			}
		}

		return new VulnerabilitySearchResult(vendor, product, cveIDs);
	}

	/**
	 * Calculate the recall and precision of a search method using an existing list
	 * of control results.
	 *
	 * @param controlResults - An array of search results that should be found.
	 * @param actualResults  - An array of the results actually found by the method.
	 */
	private void calculateRecallAndPrecision() {
		// Prepare the control results
		final ArrayList<VulnerabilitySearchResult> controlResults = readControlResultCSVData();
		final ArrayList<String> productNamesOfControlResults = new ArrayList<>();
		final ArrayList<String> vendorNamesOfControlResults = new ArrayList<>();

		for (final VulnerabilitySearchResult controlResult : controlResults) {
			productNamesOfControlResults.add(controlResult.getProductName());
			vendorNamesOfControlResults.add(controlResult.getVendorName());
		}

		// Prepare the actual results
		final ArrayList<VulnerabilitySearchResult> actualResults = new ArrayList<>();

		// Get the actual results using the control results' product and vendor name
		for (int i = 0; i < productNamesOfControlResults.size(); i++) {
			final VulnerabilitySearchResult actualSearchResult = getCVEsOfProduct(productNamesOfControlResults.get(i),
					vendorNamesOfControlResults.get(i), "", "TWO");
			actualResults.add(actualSearchResult);
		}

		// A list for the recall and precision of every project
		final ArrayList<ProjectRecallPrecisionTriple<String, Double, Double>> recallAndPrecisionPerProject = new ArrayList<>();

		// CVE in actual results and CVE in control results.
		int allTruePositives = 0;

		// CVE in actual results, but not in control results.
		int allFalsePositives = 0;

		// CVE not in actual results, but in control results
		int allFalseNegatives = 0;

		// Get control results CVE size
		int CVEsInControlResults = 0;
		for (final VulnerabilitySearchResult controlResult : controlResults) {
			CVEsInControlResults += controlResult.getCveIDs().size();
		}

		// Recall and precision for each project
		float singularRecall = 0;
		float singularPrecision = 0;

		// Iterate the control results
		for (final VulnerabilitySearchResult controlResult : controlResults) {
			int truePositives = 0;
			int falsePositives = 0;
			int falseNegatives;

			// Get their CVE list and product name
			final ArrayList<String> expectedCVEIDs = new ArrayList<>(controlResult.getCveIDs());
			final String controlResultProductName = controlResult.getProductName();

			List<String> actualCVEIDs = new ArrayList<>();

			// The triple for a single projects recall and precision calculation
			final ProjectRecallPrecisionTriple<String, Double, Double> singleRecallAndPrecision = new ProjectRecallPrecisionTriple<>(
					controlResultProductName, singularRecall, singularPrecision);

			// Find the fitting actual result to the control result
			for (final VulnerabilitySearchResult actualResult : actualResults) {
				if (actualResult.getProductName().equals(controlResultProductName)) {
					// Get the actual CVE IDs
					actualCVEIDs = actualResult.getCveIDs();
					break;
				}
			}

			// If nothing is found for this project - recall and precision are 0
			if (actualCVEIDs == null) {
				LOGGER.log(Level.ERROR, "No actual results for: " + controlResultProductName + " by vendor "
						+ controlResult.getVendorName());
				singleRecallAndPrecision.setPrecision(0f);
				singleRecallAndPrecision.setRecall(0f);
				continue;
			}

			// If something is found we have true and false positives
			for (final String actualCVEID : actualCVEIDs) {
				if (expectedCVEIDs.remove(actualCVEID)) {
					truePositives++;
					allTruePositives++;
				} else {
					LOGGER.log(Level.ERROR,
							"FalsePositive for project: " + controlResultProductName + ": " + actualCVEID);
					falsePositives++;
					allFalsePositives++;
				}
			}

			// Calculate the false negatives
			falseNegatives = expectedCVEIDs.size();
			allFalseNegatives += expectedCVEIDs.size();

			// Print out the false negatives
			for (final String expectedCVEID : expectedCVEIDs) {
				LOGGER.log(Level.ERROR, "FalseNegative for " + controlResultProductName + ": " + expectedCVEID);
			}

			// Calculate singular recall and precision
			singularRecall = truePositives / (float) (truePositives + falseNegatives);
			if ((truePositives + falsePositives) > 0) {
				singularPrecision = truePositives / (float) (truePositives + falsePositives);
			} else {
				singularPrecision = 0;
			}

			// Set the recall and precision in the projects triple
			singleRecallAndPrecision.setRecall(singularRecall);
			singleRecallAndPrecision.setPrecision(singularPrecision);

			// Add the triple to the result triples
			recallAndPrecisionPerProject.add(singleRecallAndPrecision);

		}

		// Print out recall and precision for each project
		double averageRecall = 0;
		double averagePrecission = 0;
		LOGGER.log(Level.INFO, "################Recall and precision per project################");
		for (final ProjectRecallPrecisionTriple<String, Double, Double> triple : recallAndPrecisionPerProject) {
			LOGGER.log(Level.INFO, "Recall and precision for project: " + triple.getProjectName() + " was "
					+ triple.getRecall() + " , " + triple.getPrecision());
			averageRecall += triple.getRecall();
			averagePrecission += triple.getPrecision();

		}
		averagePrecission = averagePrecission / recallAndPrecisionPerProject.size();
		averageRecall = averageRecall / recallAndPrecisionPerProject.size();
		LOGGER.log(Level.INFO, "################Recall and precision per project################");

		// Calculate overall recall and precision
		final float recall = allTruePositives / (float) (allTruePositives + allFalseNegatives);
		final float precision = allTruePositives / (float) (allTruePositives + allFalsePositives);

		LOGGER.log(Level.INFO, "################Recall and precision overall################");
		LOGGER.log(Level.INFO, "For " + CVEsInControlResults + " CVE entries in the control results, there were: ");
		LOGGER.log(Level.INFO, "True positives: " + allTruePositives + " False positives: " + allFalsePositives
				+ " False negatives: " + allFalseNegatives + " ..in the actual results.");
		LOGGER.log(Level.INFO,
				"The overall recall for this method was: " + recall + " and the precision was: " + precision);
		LOGGER.log(Level.INFO, "The average recall for this method was: " + averageRecall
				+ " and the average precision was: " + averagePrecission);
		LOGGER.log(Level.INFO, "################Recall and precision overall################");
	}

	@Test
	public void testRecallAndPrecision() {
		calculateRecallAndPrecision();
	}

}
