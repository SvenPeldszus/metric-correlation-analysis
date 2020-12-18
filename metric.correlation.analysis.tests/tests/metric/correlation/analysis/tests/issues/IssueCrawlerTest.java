package metric.correlation.analysis.tests.issues;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import metric.correlation.analysis.database.MongoDBHelper;
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


	@Test
	public void test() throws IOException {
		final GithubIssueCrawler crawler = new GithubIssueCrawler();
		final List<Issue> issues = crawler.getIssues("quarkusio", "quarkus", "0.0.1");
		try (MongoDBHelper db = new MongoDBHelper("metric_correlation", "issues")) {
			final List<Map<String, Object>> test = new ArrayList<>();
			for (final Issue issue : issues) {
				test.add(issue.asMap());
			}
			db.storeMany(test);
		}
	}
}
