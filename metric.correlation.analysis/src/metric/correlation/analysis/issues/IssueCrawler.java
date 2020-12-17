package metric.correlation.analysis.issues;

import java.util.List;

public interface IssueCrawler {
	void setClassifier(Classifier classifier);

	List<Issue> getIssues(String vendor, String product, String version);
	
	double getReleaseDurationInMonths();
}	
