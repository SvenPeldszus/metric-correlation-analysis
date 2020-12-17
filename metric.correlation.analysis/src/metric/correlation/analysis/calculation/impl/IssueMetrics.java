package metric.correlation.analysis.calculation.impl;

import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.AVG_OPEN_TIME_DAYS;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.AVG_OPEN_TIME_DAYS_CONT;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.AVG_OPEN_TIME_DAYS_KLOC;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.AVG_OPEN_TIME_DAYS_TIME;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.BUG_ISSUES;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.BUG_ISSUES_CONT;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.BUG_ISSUES_KLOC;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.CONTRIBUTOR;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.SECURITY_BUG_ISSUES;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.SECURITY_BUG_ISSUES_CONT;
import static metric.correlation.analysis.calculation.impl.IssueMetrics.MetricKeysImpl.SECURITY_BUG_ISSUES_KLOC;

import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Test;

import com.google.gson.JsonArray;

import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.database.MongoDBHelper;
import metric.correlation.analysis.issues.GithubIssueCrawler;
import metric.correlation.analysis.issues.Issue;
import metric.correlation.analysis.issues.Issue.IssueType;
import metric.correlation.analysis.issues.IssueCrawler;

public class IssueMetrics implements IMetricCalculator {
	private IssueCrawler issueCrawler = new GithubIssueCrawler(); // uses default nlpclassifier
	private Map<String, Double> metricValues = new HashMap<>();
	private Map<String, String> metricResults = new HashMap<>();
	private static final Logger LOGGER = Logger.getLogger(IssueMetrics.class);
	private static final double DAYS_PER_MONTH = 30.4;

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version,
			Map<String, String> map) {
		for (String key : getMetricKeys()) {
			metricValues.put(key, 0.0);
		}
		double lloc = Double.parseDouble(map.get(SourceMeterMetrics.MetricKeysImpl.LLOC.toString()));
		if (lloc <= 0) {
			return false;
		}
		double closedIssues = 0;
		double contributorCount = getContributorCount(vendorName, productName) / 10; // normalize for 10 contributors
		if (contributorCount == -1) {
			return false;
		}
		List<Issue> issues = issueCrawler.getIssues(vendorName, productName, version);
		double releaseDuration = issueCrawler.getReleaseDurationInMonths();
		for (Issue issue : issues) {
			if (issue.isClosed()) {
				closedIssues++;
				double days = ChronoUnit.DAYS.between(issue.getCreationDate(), issue.getClosingDate());
				metricValues.put(AVG_OPEN_TIME_DAYS.toString(), metricValues.get(AVG_OPEN_TIME_DAYS.toString()) + days);
			}
			IssueType issueType = issue.getType();
			if (issueType == null) {
				continue;
			}
			if (issueType.equals(IssueType.BUG)) {
				metricValues.put(BUG_ISSUES.toString(), metricValues.get(BUG_ISSUES.toString()) + 1);
			}
			if (issueType.equals(IssueType.SECURITY_BUG)) {
				metricValues.put(BUG_ISSUES.toString(), metricValues.get(BUG_ISSUES.toString()) + 1);
				metricValues.put(SECURITY_BUG_ISSUES.toString(), metricValues.get(SECURITY_BUG_ISSUES.toString()) + 1);
			}
		}
		double avgTime = closedIssues == 0 ? releaseDuration * DAYS_PER_MONTH
				: metricValues.get(AVG_OPEN_TIME_DAYS.toString()) / (double) closedIssues;

		metricValues.put(CONTRIBUTOR.toString(), contributorCount);

		metricValues.put(AVG_OPEN_TIME_DAYS.toString(), avgTime);
		metricValues.put(AVG_OPEN_TIME_DAYS_CONT.toString(), avgTime / contributorCount);
		metricValues.put(AVG_OPEN_TIME_DAYS_KLOC.toString(), avgTime * 1000 / lloc);
		metricValues.put(AVG_OPEN_TIME_DAYS_TIME.toString(), avgTime / releaseDuration);

		// metricValues.put(ISSUES_TOTAL.toString(), (double) issues.size());
		// metricValues.put(ISSUES_TOTAL_KLOC.toString(), issues.size() * 1000.0 /
		// lloc);
		// metricValues.put(ISSUES_TOTAL_CONT.toString(), issues.size() /
		// contributorCount);

		metricValues.put(BUG_ISSUES_KLOC.toString(), metricValues.get(BUG_ISSUES.toString()) * 1000.0 / lloc);
		metricValues.put(BUG_ISSUES_CONT.toString(), metricValues.get(BUG_ISSUES.toString()) / contributorCount);

		metricValues.put(SECURITY_BUG_ISSUES_KLOC.toString(),
				metricValues.get(SECURITY_BUG_ISSUES.toString()) * 1000.0 / lloc);
		metricValues.put(SECURITY_BUG_ISSUES_CONT.toString(),
				metricValues.get(SECURITY_BUG_ISSUES.toString()) / contributorCount);

		for (String key : metricValues.keySet()) {
			if (key.endsWith("_TIME") && !key.startsWith("AVG_OPEN_TIME_DAYS")) {
				metricValues.put(key, metricValues.get(key.replace("_TIME", "")) / releaseDuration);
			}
		}
		setResults();
		return true;
	}

	private void setResults() {
		DecimalFormat dFormat = SourceMeterMetrics.getFormatter();
		for (String s : getMetricKeys()) {
			metricResults.put(s, dFormat.format(metricValues.get(s)));
		}
	}

	public static double getContributorCount(String vendorName, String productName) {
		double cnt = 0.0;
		for (int i = 1; i < 100; i++) {
			String path = "https://api.github.com/repos/" + vendorName + "/" + productName
					+ "/contributors?per_page=100&page=" + i;
			JsonArray jArray;
			try {
				jArray = GithubIssueCrawler.getJsonFromURL(path).getAsJsonArray();
			} catch (Exception e) {
				LOGGER.error("could not retrieve contributor information", e);
				return -1;
			}
			cnt += jArray.size();
			if (jArray.size() < 100) {
				break;
			}
		}
		return cnt;
	}

	@Override
	public Map<String, String> getResults() {
		return metricResults;
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toList());
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		Set<Class<? extends IMetricCalculator>> dependencies = new HashSet<>();
		dependencies.add(SourceMeterMetrics.class);
		return dependencies;
	}

	public enum MetricKeysImpl {
		BUG_ISSUES("BUG_ISSUES"), BUG_ISSUES_KLOC("BUG_ISSUES_KLOC"), BUG_ISSUES_CONT("BUG_ISSUES_CONT"),
		BUG_ISSUES_TIME("BUG_ISSUES_TIME"), BUG_ISSUES_KLOC_TIME("BUG_ISSUES_KLOC_TIME"),
		SECURITY_BUG_ISSUES("SECURITY_BUG_ISSUES"), SECURITY_BUG_ISSUES_KLOC("SECURITY_BUG_ISSUES_KLOC"),
		SECURITY_BUG_ISSUES_TIME("SECURITY_BUG_ISSUES_TIME"), SECURITY_BUG_ISSUES_CONT("SECURITY_BUG_ISSUES_CONT"),
		SECURITY_BUG_ISSUES_KLOC_TIME("SECURITY_BUG_ISSUES_KLOC_TIME"), AVG_OPEN_TIME_DAYS("AVG_OPEN_TIME_DAYS"),
		AVG_OPEN_TIME_DAYS_CONT("AVG_OPEN_TIME_DAYS_CONT"), AVG_OPEN_TIME_DAYS_KLOC("AVG_OPEN_TIME_DAYS_KLOC"),
		AVG_OPEN_TIME_DAYS_TIME("AVG_OPEN_TIME_DAYS_TIME"), CONTRIBUTOR("CONTRIBUTOR");

		private String value;

		private MetricKeysImpl(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	// @Test
	public void getIssuesFromDatabase() {
		List<Issue> issues = new LinkedList<>();
		try (MongoDBHelper mongo = new MongoDBHelper("metric_correlation", "antlr4issues")) {
			List<Document> maps = mongo.getDocuments(new HashMap<>());
			for (Document doc : maps) {
				Issue issue = new Issue();
				issue.fromDocument(doc);
				issues.add(issue);
			}
		}
		System.out.println(issues.size());
	}

	@Test
	public void retrieveIssues() {
		GithubIssueCrawler crawler = new GithubIssueCrawler();
		String product = "antlr4";
		String vendor = "antlr";
		String version = "4.0";
		List<Issue> issues = crawler.getIssues(vendor, product, version);
		List<Map<String, Object>> issueMaps = new LinkedList<>();
		for (Issue issue : issues) {
			issueMaps.add(issue.asMap());
		}
		// try (MongoDBHelper mongo = new MongoDBHelper("metric_correlation",
		// "antlr4-issues")) {
		// mongo.storeMany(issueMaps);
		// }
	}
}
