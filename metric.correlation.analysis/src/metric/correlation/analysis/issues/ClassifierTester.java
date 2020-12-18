package metric.correlation.analysis.issues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.junit.Test;

import metric.correlation.analysis.database.MongoDBHelper;
import metric.correlation.analysis.issues.Issue.IssueType;

public class ClassifierTester {
	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(ClassifierTester.class);

	private static final String BUG_COLLECTION = "issues_test_bugs";
	private static final String SECURITY_COLLECTION = "issues_test_security";

	private Classifier classifier;
	private List<Issue> bugIssues = new ArrayList<>();
	private List<Issue> secIssues = new ArrayList<>();

	private List<IssueType> bugTypes = Arrays.asList(IssueType.BUG, IssueType.SECURITY_BUG);
	private List<IssueType> secTypes = Arrays.asList(IssueType.SECURITY_BUG, IssueType.SECURITY_REQUEST);

	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
	}

	public ClassifierTester() {
		initData();
	}

	private void getTestIssues(String collection, List<Issue> issueList) {
		try (MongoDBHelper db = new MongoDBHelper("metric_correlation", collection)) {
			List<Document> docs = db.getDocuments(new HashMap<>());
			for (Document doc : docs) {
				Issue issue = new Issue();
				issue.fromDocument(doc);
				issueList.add(issue);
			}
		}
	}

	private void initData() {
		getTestIssues(BUG_COLLECTION, bugIssues);
		getTestIssues(SECURITY_COLLECTION, secIssues);
	}

	private void runTest(List<Issue> issues, List<IssueType> expectedList) {
		int tp = 0;
		int fp = 0;
		int tn = 0;
		int fn = 0;
		for (Issue issue : issues) {
			boolean expectedFlag = expectedList.contains(issue.getType());
			IssueType guessType = classifier.classify(issue);
			boolean actualFlag = expectedList.contains(guessType);
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
		double precision = ((double) tp) / (tp + fp);
		double recall = ((double) tp) / (tp + fn);
		double accuracy = ((double) tp + tn) / (tp + tn + fp + fn);
		LOGGER.info("Precision: " + precision);
		LOGGER.info("Recall : " + recall);
		LOGGER.info("Accuracy : " + accuracy);
	}

	public void rateClassifier() {
		LOGGER.info("Rating bug classfication");
		runTest(bugIssues, bugTypes);
		LOGGER.info("Rating security classfication");
		runTest(secIssues, secTypes);
	}

	// @Test
	/**
	 * THIS WAS USED TO FILL THE TEST SAMPLE DATABASE
	 */
	public void testSample() {
		List<Document> sample;
		try (MongoDBHelper db = new MongoDBHelper("metric_correlation", "issues")) {
			sample = db.sampleDocs(new String[] { "BUG", "FEATURE_REQUEST" }, 250);
			long deleted = db.delete(sample);
			LOGGER.info("deleted" + deleted);
		}
		if (sample != null) {
			try (MongoDBHelper db = new MongoDBHelper("metric_correlation", "issues_test_security")) {
				db.addDocuments(sample);
			}
		}
	}

	@Test
	public void testLists() {
		List<Document> sample;
		List<Issue> trainIssues = new LinkedList<>();
		try (MongoDBHelper db = new MongoDBHelper("metric_correlation", "issues")) {
			sample = db.sampleDocs(new String[] { "BUG", "SECURITY_BUG", "FEATURE_REQUEST", "SECURITY_FEATURE" },
					10000);
			for (Document doc : sample) {
				Issue issue = new Issue();
				issue.fromDocument(doc);
				trainIssues.add(issue);
			}
		}
		ClassifierTester tester = new ClassifierTester();
		Classifier nlp = new NLPClassifier();
		//nlp.train(trainIssues);
		tester.setClassifier(nlp);

		tester.rateClassifier();
	}
}
