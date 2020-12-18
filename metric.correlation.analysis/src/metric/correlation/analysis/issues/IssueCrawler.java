package metric.correlation.analysis.issues;

import java.io.IOException;
import java.util.List;

public interface IssueCrawler {
	void setClassifier(Classifier classifier);

	List<Issue> getIssues(String vendor, String product, String version) throws IOException;

	double getReleaseDurationInMonths();
}
