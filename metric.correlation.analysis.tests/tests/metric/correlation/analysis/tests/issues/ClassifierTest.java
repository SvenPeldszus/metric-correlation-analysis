package metric.correlation.analysis.tests.issues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.junit.Test;

import metric.correlation.analysis.database.MongoDBHelper;
import metric.correlation.analysis.issues.Classifier;
import metric.correlation.analysis.issues.Issue;
import metric.correlation.analysis.issues.NLPClassifier;
import metric.correlation.analysis.issues.Issue.IssueType;

public class ClassifierTest {
	private static final String METRIC_CORRELATION = "metric_correlation";

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(ClassifierTest.class);

	private static final String BUG_COLLECTION = "issues_test_bugs";
	private static final String SECURITY_COLLECTION = "issues_test_security";

	private Classifier classifier;
	private final List<Issue> bugIssues = new ArrayList<>();
	private final List<Issue> secIssues = new ArrayList<>();

	private final List<IssueType> bugTypes = Arrays.asList(IssueType.BUG, IssueType.SECURITY_BUG);
	private final List<IssueType> secTypes = Arrays.asList(IssueType.SECURITY_BUG, IssueType.SECURITY_REQUEST);

	public void setClassifier(final Classifier classifier) {
		this.classifier = classifier;
	}

	public ClassifierTest() {
		initData();
	}

	private void getTestIssues(final String collection, final List<Issue> issueList) {
		try (MongoDBHelper db = new MongoDBHelper(METRIC_CORRELATION, collection)) {
			final List<Document> docs = db.getDocuments(new HashMap<>());
			for (final Document doc : docs) {
				final Issue issue = new Issue();
				issue.fromDocument(doc);
				issueList.add(issue);
			}
		}
	}

	private void initData() {
		getTestIssues(BUG_COLLECTION, this.bugIssues);
		getTestIssues(SECURITY_COLLECTION, this.secIssues);
	}

	private void runTest(final List<Issue> issues, final List<IssueType> expectedList) {
		int tp = 0;
		int fp = 0;
		int tn = 0;
		int fn = 0;
		for (final Issue issue : issues) {
			final boolean expectedFlag = expectedList.contains(issue.getType());
			final IssueType guessType = this.classifier.classify(issue);
			final boolean actualFlag = expectedList.contains(guessType);
			if (expectedFlag && actualFlag) {
				tp++;
			}
			if (expectedFlag && !actualFlag) {
				fn++;
			}
			if (!expectedFlag && actualFlag) {
				fp++;
			}
			if (!expectedFlag && !actualFlag) {
				tn++;
			}
		}
		final double precision = ((double) tp) / (tp + fp);
		final double recall = ((double) tp) / (tp + fn);
		final double accuracy = ((double) tp + tn) / (tp + tn + fp + fn);
		LOGGER.info("Precision: " + precision);
		LOGGER.info("Recall : " + recall);
		LOGGER.info("Accuracy : " + accuracy);
	}

	public void rateClassifier() {
		LOGGER.info("Rating bug classfication");
		runTest(this.bugIssues, this.bugTypes);
		LOGGER.info("Rating security classfication");
		runTest(this.secIssues, this.secTypes);
	}

	// @Test
	/**
	 * THIS WAS USED TO FILL THE TEST SAMPLE DATABASE
	 */
	public void testSample() {
		List<Document> sample;
		try (MongoDBHelper db = new MongoDBHelper(METRIC_CORRELATION, "issues")) {
			sample = db.sampleDocs(new String[] { "BUG", "FEATURE_REQUEST" }, 250);
			final long deleted = db.delete(sample);
			LOGGER.info("deleted" + deleted);
		}
		if (sample != null) {
			try (MongoDBHelper db = new MongoDBHelper(METRIC_CORRELATION, "issues_test_security")) {
				db.addDocuments(sample);
			}
		}
	}

	@Test
	public void testLists() throws IOException {
		List<Document> sample;
		final List<Issue> trainIssues = new LinkedList<>();
		try (MongoDBHelper db = new MongoDBHelper(METRIC_CORRELATION, "issues")) {
			sample = db.sampleDocs(new String[] { "BUG", "SECURITY_BUG", "FEATURE_REQUEST", "SECURITY_FEATURE" },
					10000);
			for (final Document doc : sample) {
				final Issue issue = new Issue();
				issue.fromDocument(doc);
				trainIssues.add(issue);
			}
		}
		final ClassifierTest tester = new ClassifierTest();
		final Classifier nlp = new NLPClassifier();
		nlp.train(trainIssues);
		tester.setClassifier(nlp);
		tester.rateClassifier();
	}
}
