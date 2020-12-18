package metric.correlation.analysis.issues;

import java.util.List;

import metric.correlation.analysis.issues.Issue.IssueType;

public interface Classifier {

	public IssueType classify(Issue issue);

	public void train(List<Issue> issues);
}
