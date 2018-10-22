package metric.correlation.analysis.vulnerabilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

public class SearchMethodEvaluator {

	private static final Logger LOGGER = Logger.getLogger(SearchMethodEvaluator.class);
	
	private static final String controlResultsFileLocation = "SearchMethodComparisonResources/bt-data-githubname-cve-v3.0.csv";
	private VulnerabilityDataQueryHandler VDQH = new VulnerabilityDataQueryHandler();
	
	/**
	 * A class for representing search results for the vulnerability search
	 * 
	 * @param vendorName
	 *            the name of the product's vendor to search for
	 * @param productName
	 *            the name of the project/product to search for
	 * @param cveIDs
	 *            a list of found CVE's for this project with their CVE ID's.
	 */
	class VulnerabilitySearchResult implements Comparable<VulnerabilitySearchResult> {

		private String vendorName;
		private String productName;
		private ArrayList<String> cveIDs;

		public VulnerabilitySearchResult(String vendorName, String productName, ArrayList<String> cves) {
			this.vendorName = vendorName;
			this.productName = productName;
			this.cveIDs = cves;
		}

		@Override
		public int compareTo(VulnerabilitySearchResult vulnerabilityRes) {
			int firstListSize = this.cveIDs.size();
			int secondListSize = vulnerabilityRes.getCveIDs().size();
			if (firstListSize != secondListSize)
				return 0;

			boolean allVulnerabilitiesMatch = false;
			int counter = 0;

			for (String cveId : this.cveIDs) {
				if (vulnerabilityRes.getCveIDs().contains(cveId))
					counter++;
			}

			if (counter == firstListSize)
				allVulnerabilitiesMatch = true;

			return ((vulnerabilityRes.getProductName().equals(this.productName)) && allVulnerabilitiesMatch) ? 1 : 0;
		}

		public String getProductName() {
			return productName;
		}

		public String getVendorName() {
			return vendorName;
		}

		public ArrayList<String> getCveIDs() {
			return cveIDs;
		}

		@Override
		public String toString() {
			return productName + cveIDs.toString();
		}

	}

	/**
	 * Import the CVE test data for comparison from a given CSV file.
	 * 
	 * @return an ArrayList of {@link VulnerabilitySearchResult}, which is defined
	 *         by the oracle
	 */
	private ArrayList<VulnerabilitySearchResult> readControlResultCSVData() {
		ArrayList<VulnerabilitySearchResult> controlResults = new ArrayList<>();
		BufferedReader br = null;
		String line = "";

		try {
			br = new BufferedReader(new FileReader(controlResultsFileLocation));
			while ((line = br.readLine()) != null) {
				String[] vsrString = line.split(",");
				ArrayList<String> cves = new ArrayList<>();

				for (int i = 2; i < vsrString.length; i++) {
					cves.add(vsrString[i].replace("\"", "").replace(" ", ""));
				}

				controlResults
						.add(new VulnerabilitySearchResult(vsrString[0].toLowerCase().replace("-", "").replace("_", ""),
								vsrString[1].toLowerCase().replace("-", "").replace("_", ""), cves));
			}
			br.close();
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return controlResults;
	}

	/**
	 * Get the a search result containing the project name and a list of CVEs.
	 * 
	 * @param product
	 *            the product name, for which the vulnerabilities should be sought.
	 * @return A single vulnerability search result.
	 */
	private VulnerabilitySearchResult getCVEsOfProduct(String product, String vendor, String version,
			String fuzzyness) {
		HashSet<SearchHit> results = VDQH.getVulnerabilities(product, vendor, version, fuzzyness);
		ArrayList<String> cveIDs = new ArrayList<String>();

		if (!results.isEmpty()) {
			for (SearchHit searchHit : results) {
				Map<String, Object> searchHitMap = searchHit.getSourceAsMap();
				cveIDs.add((String) searchHitMap.get("ID"));
			}
		}

		return new VulnerabilitySearchResult(vendor, product, cveIDs);
	}

	/**
	 * Calculate the recall and precision of a search method using an existing list
	 * of control results.
	 * 
	 * @param controlResults
	 *            - An array of search results that should be found.
	 * @param actualResults
	 *            - An array of the results actually found by the method.
	 */
	private void calculateRecallAndPrecision() {
		// Prepare the control results
		ArrayList<VulnerabilitySearchResult> controlResults = readControlResultCSVData();
		ArrayList<String> productNamesOfControlResults = new ArrayList<String>();
		ArrayList<String> vendorNamesOfControlResults = new ArrayList<String>();

		for (VulnerabilitySearchResult controlResult : controlResults) {
			productNamesOfControlResults.add(controlResult.getProductName());
			vendorNamesOfControlResults.add(controlResult.getVendorName());
		}

		// Prepare the actual results
		ArrayList<VulnerabilitySearchResult> actualResults = new ArrayList<VulnerabilitySearchResult>();

		// Get the actual results using the control results' product and vendor name
		for (int i = 0; i < productNamesOfControlResults.size(); i++) {
			VulnerabilitySearchResult actualSearchResult = getCVEsOfProduct(productNamesOfControlResults.get(i),
					vendorNamesOfControlResults.get(i), "", "TWO");
			actualResults.add(actualSearchResult);
		}

		// A list for the recall and precision of every project
		ArrayList<ProjectRecallPrecisionTriple<String, Double, Double>> recallAndPrecisionPerProject = new ArrayList<>();

		// CVE in actual results and CVE in control results.
		int allTruePositives = 0;

		// CVE in actual results, but not in control results.
		int allFalsePositives = 0;

		// CVE not in actual results, but in control results
		int allFalseNegatives = 0;

		// Get control results CVE size
		int CVEsInControlResults = 0;
		for (int i = 0; i < controlResults.size(); i++) {
			CVEsInControlResults += controlResults.get(i).getCveIDs().size();
		}

		// Recall and precision for each project
		float singularRecall = 0, singularPrecision = 0;

		// Iterate the control results
		for (VulnerabilitySearchResult controlResult : controlResults) {
			int truePositives = 0, falsePositives = 0, falseNegatives = 0;
			
			// Get their CVE list and product name
			ArrayList<String> expectedCVEIDs = new ArrayList<String>(controlResult.getCveIDs());
			String controlResultProductName = controlResult.getProductName();

			ArrayList<String> actualCVEIDs = new ArrayList<String>();

			// The triple for a single projects recall and precision calculation
			ProjectRecallPrecisionTriple<String, Double, Double> singleRecallAndPrecision = new ProjectRecallPrecisionTriple<String, Double, Double>(
					controlResultProductName, singularRecall, singularPrecision);

			// Find the fitting actual result to the control result
			for (VulnerabilitySearchResult actualResult : actualResults) {
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
			for (String actualCVEID : actualCVEIDs) {
				if (expectedCVEIDs.remove(actualCVEID)) {
					truePositives++;
					allTruePositives++;
				} else {
					LOGGER.log(Level.ERROR, "FalsePositive for project: " + controlResultProductName + ": " + actualCVEID);
					falsePositives++;
					allFalsePositives++;
				}
			}

			// Calculate the false negatives
			falseNegatives = expectedCVEIDs.size();
			allFalseNegatives += expectedCVEIDs.size();

			// Print out the false negatives
			for (String expectedCVEID : expectedCVEIDs) {
				LOGGER.log(Level.ERROR, "FalseNegative for " + controlResultProductName + ": " + expectedCVEID);
			}

			// Calculate singular recall and precision
			singularRecall = truePositives / (float) (truePositives + falseNegatives);
			if (truePositives + falsePositives > 0) {
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
		double averageRecall = 0, averagePrecission = 0;
		LOGGER.log(Level.INFO, "################Recall and precision per project################");
		for (ProjectRecallPrecisionTriple<String, Double, Double> triple : recallAndPrecisionPerProject) {
			LOGGER.log(Level.INFO, "Recall and precision for project: " + triple.getProjectName() + " was "
					+ triple.getRecall() + " , " + triple.getPrecision());
			averageRecall += triple.getRecall();
			averagePrecission += triple.getPrecision();

		}
		averagePrecission = averagePrecission / recallAndPrecisionPerProject.size();
		averageRecall = averageRecall / recallAndPrecisionPerProject.size();
		LOGGER.log(Level.INFO, "################Recall and precision per project################");

		// Calculate overall recall and precision
		float recall = allTruePositives / (float) (allTruePositives + allFalseNegatives);
		float precision = allTruePositives / (float) (allTruePositives + allFalsePositives);

		LOGGER.log(Level.INFO, "################Recall and precision overall################");
		LOGGER.log(Level.INFO, "For " + CVEsInControlResults + " CVE entries in the control results, there were: ");
		LOGGER.log(Level.INFO, "True positives: " + allTruePositives + " False positives: " + allFalsePositives
				+ " False negatives: " + allFalseNegatives + " ..in the actual results.");
		LOGGER.log(Level.INFO, "The overall recall for this method was: " + recall + " and the precision was: " + precision);
		LOGGER.log(Level.INFO, "The average recall for this method was: " + averageRecall
				+ " and the average precision was: " + averagePrecission);
		LOGGER.log(Level.INFO, "################Recall and precision overall################");
	}

	@Test
	public void testRecallAndPrecision() {
		calculateRecallAndPrecision();
	}

}
