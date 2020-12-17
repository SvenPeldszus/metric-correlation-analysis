package metric.correlation.analysis.issues;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import metric.correlation.analysis.issues.Issue.IssueType;
import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.ml.maxent.quasinewton.QNTrainer;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ModelUtil;

public class NLPClassifier implements Classifier {

	private static final String BUG_TRAINING_PATH = "input/trainBugs.txt";
	private static final String SECURITY_TRAINING_PATH = "input/trainSecurity.txt";
	private static final String BUG_CLASSIFIER_PATH = "input/catBugs.bin";
	private static final String SECURITY_CLASSIFIER_PATH = "input/catSec.bin";
	private Map<IssueType, String> bugMap = new HashMap<>();
	private Map<IssueType, String> securityMap = new HashMap<>();
	private DocumentCategorizerME bugCategorizer;
	private DocumentCategorizerME securityCategorizer;

	/**
	 * NOTE: creating the class will already load the model for higher performance, if you train a new model 
	 * you need to call init before it will be used
	 */
	public NLPClassifier() {
		init();
	}

	public void init() {
		initMaps();
		try (InputStream modelIn = new FileInputStream(BUG_CLASSIFIER_PATH)) {
			DoccatModel me = new DoccatModel(modelIn);
			bugCategorizer = new DocumentCategorizerME(me);
		} catch (Exception e) {

		}
		try (InputStream modelIn = new FileInputStream(SECURITY_CLASSIFIER_PATH)) {
			DoccatModel me = new DoccatModel(modelIn);
			securityCategorizer = new DocumentCategorizerME(me);
		} catch (Exception e) {

		}
	}

	private void initMaps() {
		bugMap.put(IssueType.BUG, "BUG");
		bugMap.put(IssueType.SECURITY_BUG, "BUG");
		bugMap.put(IssueType.FEATURE_REQUEST, "NOBUG");
		bugMap.put(IssueType.SECURITY_REQUEST, "NOBUG");
		securityMap.put(IssueType.SECURITY_BUG, "SEC");
		securityMap.put(IssueType.SECURITY_REQUEST, "SEC");
		securityMap.put(IssueType.BUG, "NOSEC");
		securityMap.put(IssueType.FEATURE_REQUEST, "NOSEC");
	}

	@Override
	public IssueType classify(Issue issue) {
		boolean bug = false;
		boolean sec = false;
		String text = issue.getTitle() + " " + issue.getBody() + getComments(issue);
		String[] words = preProcess(text);
		double[] outcomesBug = bugCategorizer.categorize(words);
		String categoryBug = bugCategorizer.getBestCategory(outcomesBug);
		double[] outcomesSec = securityCategorizer.categorize(words);
		String categorySec = securityCategorizer.getBestCategory(outcomesSec);
		bug = categoryBug.equals("BUG");
		sec = categorySec.equals("SEC");
		if (bug && sec) {
			return IssueType.SECURITY_BUG;
		}
		if (bug) {
			return IssueType.BUG;
		}
		if (sec) {
			return IssueType.SECURITY_REQUEST;
		}
		return IssueType.FEATURE_REQUEST;

	}

	private String[] preProcess(String text) {
		text = cleanText(text);
		String[] words = text.split(" ");
		List<String> wordList = new ArrayList<>();
		for (int i = 0; i < words.length; i++) {
			String next = words[i];
			if (next.length() >= 3) {
				wordList.add(next);
			} else {
				wordList.add(words[i]);
			}
		}
		return (String[]) wordList.toArray(new String[] {});

	}

	private String cleanText(String text) {
		text = text.replaceAll("[\r\n]", "");
		return text.toLowerCase().replaceAll("[^a-z' ]", " ").trim();

	}

	private void createTrainingFile(List<Issue> issues, String path, Map<IssueType, String> cats) {
		try (FileWriter fw = new FileWriter(new File(path))) {
			for (Issue issue : issues) {
				String[] tokens = preProcess(issue.getTitle() + " " + issue.getBody() + getComments(issue));
				StringBuilder sb = new StringBuilder();
				for (String token : tokens) {
					sb.append(token + " ");
				}
				String line = sb.toString();
				if (line.trim().isEmpty()) {
					continue;
				}
				fw.write(cats.get(issue.getType()) + "\t");
				fw.write(line);
				fw.write("\r\n");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getComments(Issue issue) {
		StringBuilder sb = new StringBuilder();
		for (String comment : issue.getComments()) {
			sb.append(" " + comment);
		}
		return sb.toString();
	}

	private void createModel(String input, String output) {
		InputStreamFactory inputStreamFactory;
		ObjectStream<String> lineStream;
		try {
			inputStreamFactory = new MarkableFileInputStreamFactory(new File(input));
			lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
			ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);
			TrainingParameters params = ModelUtil.createDefaultTrainingParameters();
			params.put(TrainingParameters.ITERATIONS_PARAM, 500);
			params.put(TrainingParameters.CUTOFF_PARAM, 1);
			params.put(TrainingParameters.ALGORITHM_PARAM, QNTrainer.MAXENT_QN_VALUE);
			DoccatFactory factory = new DoccatFactory(new FeatureGenerator[] { new BagOfWordsFeatureGenerator() });
			DoccatModel model = DocumentCategorizerME.train("en", sampleStream, params, factory);
			model.serialize(new File(output));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void train(List<Issue> issues) {
		createTrainingFile(issues, BUG_TRAINING_PATH, bugMap);
		createTrainingFile(issues, SECURITY_TRAINING_PATH, securityMap);
		createModel(BUG_TRAINING_PATH, BUG_CLASSIFIER_PATH);
		createModel(SECURITY_TRAINING_PATH, SECURITY_CLASSIFIER_PATH);
		init();
	}

}
