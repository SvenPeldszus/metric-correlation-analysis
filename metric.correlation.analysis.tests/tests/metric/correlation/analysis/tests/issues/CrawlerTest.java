package metric.correlation.analysis.tests.issues;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import metric.correlation.analysis.calculation.impl.IssueMetrics;
import metric.correlation.analysis.issues.GithubIssueCrawler;
import metric.correlation.analysis.issues.Issue;

public class CrawlerTest {

	@Test
	public void testRetrieve() {
		IssueMetrics.getContributorCount("journeyapps", "zxing-android-embedded");
		GithubIssueCrawler crawler = new GithubIssueCrawler();
		List<Issue> issues = crawler.getIssues("journeyapps", "zxing-android-embedded", "v3.5.0");
		assertEquals(false, issues.isEmpty());
	}
}
