package metric.correlation.analysis.tests.issues;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import metric.correlation.analysis.calculation.impl.IssueMetrics;
import metric.correlation.analysis.issues.GithubIssueCrawler;
import metric.correlation.analysis.issues.Issue;

public class CrawlerTest {

	@Test
	public void testRetrieve() throws IOException {
		IssueMetrics.getContributorCount("journeyapps", "zxing-android-embedded");
		final GithubIssueCrawler crawler = new GithubIssueCrawler();
		final List<Issue> issues = crawler.getIssues("journeyapps", "zxing-android-embedded", "v3.5.0");
		assertEquals(false, issues.isEmpty());
	}
}
