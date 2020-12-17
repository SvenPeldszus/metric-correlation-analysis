package metric.correlation.analysis.tests.issues;

import org.junit.Test;

import metric.correlation.analysis.calculation.impl.IssueMetrics;
import metric.correlation.analysis.issues.GithubIssueCrawler;

public class CrawlerTest {

	@Test
	public void testRetrieve() {
		IssueMetrics.getContributorCount("journeyapps", "zxing-android-embedded");
		GithubIssueCrawler crawler = new GithubIssueCrawler();
		crawler.getIssues("journeyapps", "zxing-android-embedded", "v3.5.0");
	}
}
