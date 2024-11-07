package metric.correlation.analysis.calculation.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.IJavaProject;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.TextUIBugReporter;
import edu.umd.cs.findbugs.TextUICommandLine;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.IMetricClassCalculator;
import metric.correlation.analysis.database.MongoDBHelper;

public class SpotBugsMetrics implements IMetricClassCalculator {

	private static final Logger LOGGER = Logger.getLogger(SpotBugsMetrics.class);

	private static final boolean STORE_RESULTS = false; // if the bug results data should be stored
	private static final String BUG_COLLECTION = "SpotBugs";

	private BugReporter bugReporter;
	private IFindBugsEngine engine;
	private final Map<String, Map<String, Integer>> classResults = new HashMap<>();
	private Map<String, Double> metricResults;
	private double lloc = 0;

	private void init() {
		this.engine = new FindBugs2();
		this.bugReporter = new BugReporter();
		this.bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
		this.bugReporter.setRankThreshold(BugRanker.VISIBLE_RANK_MAX);
	}

	@Override
	public boolean calculateMetric(final IJavaProject project, final String productName, final String vendorName, final String version,
			final Map<String, String> map) {
		final String llocKey = SourceMeterMetrics.MetricKeysImpl.LLOC.toString();
		if (!map.containsKey(llocKey)) {
			return false;
		}
		this.lloc = Double.valueOf(map.get(llocKey));
		this.metricResults = new HashMap<>();
		for (final String metricKey : getMetricKeys()) {
			this.metricResults.put(metricKey, 0.0);
		}
		this.metricResults.put("EXPERIMENTAL", 0.0); // not a metric but makes code easier
		// String projectLocation =
		// project.getProject().getLocation().toFile().getAbsolutePath(); // imported
		// code path
		final String projectLocation = project.getProject().getLocation().toFile().getParentFile().getParentFile()
				.getAbsolutePath() + File.separator + "repositories";
		try {
			analyzeProject(projectLocation + File.separator + productName);
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, "spotbugs analysis failed");
			return false;
		}
		final BugCollection bugInstanceCollection = this.engine.getBugReporter().getBugCollection();
		final Collection<Map<String, String>> bugs = collectBugData(bugInstanceCollection);
		evaluateMetrics(bugs, productName, vendorName, version);
		return true;
	}

	private void evaluateMetrics(final Collection<Map<String, String>> bugsList, final String productName, final String vendorName,
			final String version) {
		for (final Map<String, String> bugData : bugsList) {
			if (STORE_RESULTS) {
				bugData.put("productName", productName);
				bugData.put("vendorName", vendorName);
				bugData.put("version", version);
				storeData(bugData);
			}
			final int priority = Integer.parseInt(bugData.get("rank")); // careful with naming
			final String category = bugData.get("category");
			final String className = bugData.get("class");
			if (!this.classResults.containsKey(className)) {
				final HashMap<String, Integer> classMap = new HashMap<>();
				for (final String metricKey : getMetricKeys()) {
					classMap.put(metricKey, 0);
				}
				classMap.put("EXPERIMENTAL", 0);
				this.classResults.put(className, classMap);
			}
			String priorityCat;
			if (priority <= 4) {
				priorityCat = MetricKeysImpl.HIGH_PRIO.toString();
			} else if (priority <= 9) {
				priorityCat = MetricKeysImpl.MEDIUM_PRIO.toString();
			} else {
				priorityCat = MetricKeysImpl.LOW_PRIO.toString();
			}
			final Map<String, Integer> classMap = this.classResults.get(className);
			this.metricResults.put(priorityCat, this.metricResults.get(priorityCat) + 1);
			this.metricResults.put(category, this.metricResults.get(category) + 1);
			classMap.put(category, classMap.get(category) + 1);
			classMap.put(priorityCat, classMap.get(priorityCat) + 1);
		}
		this.metricResults.put(MetricKeysImpl.VIOLATIONS.toString(), (double) bugsList.size());
		this.metricResults.remove("EXPERIMENTAL");
		normalizeResults();
	}

	private void normalizeResults() {
		for (final Entry<String, Double> entry : this.metricResults.entrySet()) {
			final double total =entry.getValue();
			final double norm = (total * 1000.0) / this.lloc;
			this.metricResults.put(entry.getKey(), norm);
		}
	}

	private void storeData(final Map<String, String> bugData) {
		try (MongoDBHelper helper = new MongoDBHelper(MongoDBHelper.DEFAULT_DATABASE, BUG_COLLECTION)) {
			helper.storeData(bugData);
		}
	}

	private void analyzeProject(final String projectLocation) throws IOException {
		init();
		final TextUICommandLine commandLine = new TextUICommandLine();
		FindBugs.processCommandLine(commandLine, new String[] { projectLocation }, this.engine); // initializes the // for
		// the engine
		this.engine.setBugReporter(this.bugReporter); // use our own bug reporter, default one outputs to console
		FindBugs.runMain(this.engine, commandLine); // run the analysis

	}

	private Collection<Map<String, String>> collectBugData(final BugCollection bugInstanceCollection) {
		final Collection<Map<String, String>> bugList = new LinkedList<>();
		for (final BugInstance bugInstance : bugInstanceCollection) {
			final Map<String, String> bugData = new HashMap<>();
			final int priority = bugInstance.getPriority();
			final int rank = bugInstance.getBugRank();
			final String type = bugInstance.getType();
			final String category = bugInstance.getBugPattern().getCategory();
			String className = "";
			for (final BugAnnotation annotation : bugInstance.getAnnotations()) {
				if (annotation instanceof ClassAnnotation) {
					className = ((ClassAnnotation) annotation).getClassName();
					break;
				}
			}
			if (className.isEmpty()) {
				LOGGER.log(Level.WARN, "Could not resolve classname of bug");
			}
			bugData.put("priority", String.valueOf(priority));
			bugData.put("rank", String.valueOf(rank));
			bugData.put("class", className);
			bugData.put("type", type);
			bugData.put("category", category);
			bugList.add(bugData);
		}
		return bugList;
	}

	@Override
	public Map<String, String> getResults() {
		final Map<String, String> result = new HashMap<>();
		for (final String cat : getMetricKeys()) {
			result.put(cat, String.valueOf(this.metricResults.get(cat)));
		}
		return result;
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toList());
	}

	public enum MetricKeysImpl {
		VIOLATIONS("VIOLOATION"), BAD_PRACTICE("BAD_PRACTICE"), CORRECTNESS("CORRECTNESS"),
		MALICIOUS_CODE("MALICIOUS_CODE"), INTERNATIONALIZATION("I18N"), MT_CORRECTNESS("MT_CORRECTNESS"),
		NOISE("NOISE"), PERFORMANCE("PERFORMANCE"), SECURITY("SECURITY"), STYLE("STYLE"), HIGH_PRIO("HIGH_PRIO"),
		MEDIUM_PRIO("MEDIUM_PRIO"), LOW_PRIO("LOW_PRIO");

		private final String value;

		MetricKeysImpl(final String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		final Set<Class<? extends IMetricCalculator>> dependencies = new HashSet<>();
		dependencies.add(SourceMeterMetrics.class);
		return dependencies;
	}

	@Override
	public Map<String, Map<String, String>> getClassResults() {
		final Map<String, Map<String, String>> result = new HashMap<>();
		for (final Entry<String, Map<String, Integer>> entry : this.classResults.entrySet()) {
			final Map<String, String> classMap = entry.getValue().entrySet().stream()
					.collect(Collectors.toMap(Entry::getKey, e-> String.valueOf(e.getValue())));
			result.put(entry.getKey(), classMap);
		}
		return result;
	}

	private class BugReporter extends TextUIBugReporter {
		private final SortedBugCollection bugCollection;

		public BugReporter() {
			this.bugCollection = new SortedBugCollection();
			this.bugCollection.setTimestamp(System.currentTimeMillis());
		}

		@Override
		public void finish() {
			this.bugCollection.bugsPopulated();
		}

		@Override
		public BugCollection getBugCollection() {
			return this.bugCollection;
		}

		@Override
		public void observeClass(final ClassDescriptor classDescriptor) {
			// gets called when a new class is being checked, no action needed

		}

		@Override
		protected void doReportBug(final BugInstance bugInstance) {
			if (this.bugCollection.add(bugInstance)) {
				notifyObservers(bugInstance);
			}
		}

	}
}
