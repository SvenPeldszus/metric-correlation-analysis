package metric.correlation.analysis.tests.issues;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import metric.correlation.analysis.issues.GithubIssueCrawler;
import metric.correlation.analysis.issues.Issue;

public class IssueCrawlerTest {

	@Test
	public void testRetrieve() throws IOException {
		final GithubIssueCrawler crawler = new GithubIssueCrawler();
		final List<Issue> issues = crawler.getIssues("journeyapps", "zxing-android-embedded", "v3.5.0");
		assertNotNull(issues);
		assertFalse(issues.isEmpty());
	}
}
