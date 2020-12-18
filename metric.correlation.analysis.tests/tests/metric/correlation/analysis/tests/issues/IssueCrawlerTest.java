package metric.correlation.analysis.tests.issues;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;

import metric.correlation.analysis.issues.GithubIssueCrawler;
import metric.correlation.analysis.issues.Issue;

public class IssueCrawlerTest {

	@Test
	public void testRetrieve() {
		final GithubIssueCrawler crawler = new GithubIssueCrawler();
		final List<Issue> issues = crawler.getIssues("journeyapps", "zxing-android-embedded", "v3.5.0");
		assertFalse(issues.isEmpty());
	}
}
