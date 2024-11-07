package metric.correlation.analysis.issues;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import metric.correlation.analysis.issues.Issue.IssueType;

/**
 * just used as a simple classifier for testing
 *
 */
public class TrivialClassifier implements Classifier {

	private Set<String> bugWords = new HashSet<>();
	private Set<String> securityWords = new HashSet<>();
	private Set<String> requestWords = new HashSet<>();

	public TrivialClassifier() {
		bugWords.addAll(Arrays.asList("bug", "fault", "error", "defect", "exception"));
		securityWords.addAll(Arrays.asList("security", "authentication", "authorization", "oidc", "saml"));
		requestWords
				.addAll(Arrays.asList("request", "add", "change", "addition", "feature", "enhancement", "proposal"));
	}

	@Override
	public IssueType classify(Issue issue) {
		String title = issue.getTitle();
		boolean bug = false;
		boolean sec = false;
		boolean req = false;
		for (String s : getWords(title)) {
			if (bugWords.contains(s)) {
				bug = true;
			}
			if (securityWords.contains(s)) {
				sec = true;
			}
			if (requestWords.contains(s)) {
				req = true;
			}
		}
		if (bug && sec) {
			return IssueType.SECURITY_BUG;
		}
		if (bug) {
			return IssueType.BUG;
		}
		if (req && sec) {
			return IssueType.SECURITY_REQUEST;
		}
		if (req) {
			return IssueType.FEATURE_REQUEST;
		}
		return IssueType.BUG;
	}

	private String[] getWords(String text) {
		return text.toLowerCase().replaceAll("[^a-z]", "").split(" ");
	}

	@Override
	public void train(List<Issue> issues) {
		// no training needed :)
	}

}
