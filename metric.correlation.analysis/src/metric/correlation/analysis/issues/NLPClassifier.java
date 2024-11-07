package metric.correlation.analysis.issues;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import metric.correlation.analysis.Activator;
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

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(NLPClassifier.class);

	private static final String BUG_TRAINING_PATH = "input/trainBugs.txt";
	private static final String SECURITY_TRAINING_PATH = "input/trainSecurity.txt";
	private static final String BUG_CLASSIFIER_PATH = "input/catBugs.bin";
	private static final String SECURITY_CLASSIFIER_PATH = "input/catSec.bin";
	private final Map<IssueType, String> bugMap = new EnumMap<>(IssueType.class);
	private final Map<IssueType, String> securityMap = new EnumMap<>(IssueType.class);
	private DocumentCategorizerME bugCategorizer;
	private DocumentCategorizerME securityCategorizer;

	/**
	 * NOTE: creating the class will already load the model for higher performance,
	 * if you train a new model you need to call init before it will be used
	 * @throws IOException
	 */
	public NLPClassifier() throws IOException {
		init();
	}

	public void init() throws IOException {
		initMaps();
		final Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		final URL bugEntry = bundle.getEntry(BUG_CLASSIFIER_PATH);
		try (InputStream modelIn = bugEntry.openStream()) {
			final DoccatModel me = new DoccatModel(modelIn);
			this.bugCategorizer = new DocumentCategorizerME(me);
		}

		final URL securityEntry = bundle.getEntry(SECURITY_CLASSIFIER_PATH);
		try (InputStream modelIn = securityEntry.openStream()) {
			final DoccatModel me = new DoccatModel(modelIn);
			this.securityCategorizer = new DocumentCategorizerME(me);
		}
	}

	private void initMaps() {
		this.bugMap.put(IssueType.BUG, "BUG");
		this.bugMap.put(IssueType.SECURITY_BUG, "BUG");
		this.bugMap.put(IssueType.FEATURE_REQUEST, "NOBUG");
		this.bugMap.put(IssueType.SECURITY_REQUEST, "NOBUG");
		this.securityMap.put(IssueType.SECURITY_BUG, "SEC");
		this.securityMap.put(IssueType.SECURITY_REQUEST, "SEC");
		this.securityMap.put(IssueType.BUG, "NOSEC");
		this.securityMap.put(IssueType.FEATURE_REQUEST, "NOSEC");
	}

	@Override
	public IssueType classify(final Issue issue) {
		boolean bug;
		boolean sec;
		final String text = issue.getTitle() + " " + issue.getBody() + getComments(issue);
		final String[] words = preProcess(text);
		final double[] outcomesBug = this.bugCategorizer.categorize(words);
		final String categoryBug = this.bugCategorizer.getBestCategory(outcomesBug);
		final double[] outcomesSec = this.securityCategorizer.categorize(words);
		final String categorySec = this.securityCategorizer.getBestCategory(outcomesSec);
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
		final String[] words = text.split(" ");
		final List<String> wordList = new ArrayList<>();
		for (final String next : words) {
			if (next.length() >= 3) {
				wordList.add(next);
			}
		}
		return wordList.toArray(new String[] {});

	}

	private String cleanText(String text) {
		text = text.replaceAll("[\r\n]", "");
		return text.toLowerCase().replaceAll("[^a-z' ]", " ").trim();

	}

	private void createTrainingFile(final List<Issue> issues, final String path, final Map<IssueType, String> cats) throws IOException {
		try (FileWriter fw = new FileWriter(new File(path))) {
			for (final Issue issue : issues) {
				final String[] tokens = preProcess(issue.getTitle() + " " + issue.getBody() + getComments(issue));
				final StringBuilder sb = new StringBuilder();
				for (final String token : tokens) {
					sb.append(token + " ");
				}
				final String line = sb.toString();
				if (line.trim().isEmpty()) {
					continue;
				}
				fw.write(cats.get(issue.getType()) + "\t");
				fw.write(line);
				fw.write("\r\n");
			}
		}
	}

	private String getComments(final Issue issue) {
		final StringBuilder sb = new StringBuilder();
		for (final String comment : issue.getComments()) {
			sb.append(" " + comment);
		}
		return sb.toString();
	}

	private void createModel(final String input, final String output) {
		InputStreamFactory inputStreamFactory;
		ObjectStream<String> lineStream;
		try {
			inputStreamFactory = new MarkableFileInputStreamFactory(new File(input));
			lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
			final ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);
			final TrainingParameters params = ModelUtil.createDefaultTrainingParameters();
			params.put(TrainingParameters.ITERATIONS_PARAM, 500);
			params.put(TrainingParameters.CUTOFF_PARAM, 1);
			params.put(TrainingParameters.ALGORITHM_PARAM, QNTrainer.MAXENT_QN_VALUE);
			final DoccatFactory factory = new DoccatFactory(
					new FeatureGenerator[] { new BagOfWordsFeatureGenerator() });
			final DoccatModel model = DocumentCategorizerME.train("en", sampleStream, params, factory);
			model.serialize(new File(output));
		} catch (final IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public void train(final List<Issue> issues) throws IOException {
		createTrainingFile(issues, BUG_TRAINING_PATH, this.bugMap);
		createTrainingFile(issues, SECURITY_TRAINING_PATH, this.securityMap);
		createModel(BUG_TRAINING_PATH, BUG_CLASSIFIER_PATH);
		createModel(SECURITY_TRAINING_PATH, SECURITY_CLASSIFIER_PATH);
		init();
	}

}
