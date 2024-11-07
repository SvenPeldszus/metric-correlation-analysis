package metric.correlation.analysis.issues;

import java.io.IOException;
import java.util.List;

import metric.correlation.analysis.issues.Issue.IssueType;

public interface Classifier {

	IssueType classify(Issue issue);

	void train(List<Issue> issues) throws IOException;
}
