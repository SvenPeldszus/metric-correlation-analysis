package metric.correlation.analysis.calculation.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
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
	private Map<String, Map<String, Integer>> classResults = new HashMap<>();
	private Map<String, Double> metricResults;
	private double lloc = 0;

	private void init() {
		this.engine = new FindBugs2();
		bugReporter = new BugReporter();
		bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
		bugReporter.setRankThreshold(BugRanker.VISIBLE_RANK_MAX);
	}

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version,
			Map<String, String> map) {
		String llocKey = SourceMeterMetrics.MetricKeysImpl.LLOC.toString();
		if (!map.containsKey(llocKey)) {
			return false;
		}
		lloc = Double.valueOf(map.get(llocKey));
		metricResults = new HashMap<>();
		for (String metricKey : getMetricKeys()) {
			metricResults.put(metricKey, 0.0);
		}
		metricResults.put("EXPERIMENTAL", 0.0); // not a metric but makes code easier
		// String projectLocation =
		// project.getProject().getLocation().toFile().getAbsolutePath(); // imported
		// code path
		String projectLocation = project.getProject().getLocation().toFile().getParentFile().getParentFile()
				.getAbsolutePath() + File.separator + "repositories";
		try {
			analyzeProject(projectLocation + File.separator + productName);
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "spotbugs analysis failed");
			return false;
		}
		BugCollection bugInstanceCollection = engine.getBugReporter().getBugCollection();
		Collection<Map<String, String>> bugs = collectBugData(bugInstanceCollection);
		evaluateMetrics(bugs, productName, vendorName, version);
		return true;
	}

	private void evaluateMetrics(Collection<Map<String, String>> bugsList, String productName, String vendorName,
			String version) {
		for (Map<String, String> bugData : bugsList) {
			if (STORE_RESULTS) {
				bugData.put("productName", productName);
				bugData.put("vendorName", vendorName);
				bugData.put("version", version);
				storeData(bugData);
			}
			int priority = Integer.parseInt(bugData.get("rank")); // careful with naming
			String category = bugData.get("category");
			String className = bugData.get("class");
			if (!classResults.containsKey(className)) {
				HashMap<String, Integer> classMap = new HashMap<>();
				for (String metricKey : getMetricKeys()) {
					classMap.put(metricKey, 0);
				}
				classMap.put("EXPERIMENTAL", 0);
				classResults.put(className, classMap);
			}
			String priorityCat;
			if (priority <= 4) {
				priorityCat = MetricKeysImpl.HIGH_PRIO.toString();
			} else if (priority <= 9) {
				priorityCat = MetricKeysImpl.MEDIUM_PRIO.toString();
			} else {
				priorityCat = MetricKeysImpl.LOW_PRIO.toString();
			}
			Map<String, Integer> classMap = classResults.get(className);
			metricResults.put(priorityCat, metricResults.get(priorityCat) + 1);
			metricResults.put(category, metricResults.get(category) + 1);
			classMap.put(category, classMap.get(category) + 1);
			classMap.put(priorityCat, classMap.get(priorityCat) + 1);
		}
		metricResults.put(MetricKeysImpl.VIOLATIONS.toString(), (double) bugsList.size());
		metricResults.remove("EXPERIMENTAL");
		normalizeResults();
	}

	private void normalizeResults() {
		for (String key : metricResults.keySet()) {
			double total = metricResults.get(key);
			double norm = total * 1000.0 / lloc;
			metricResults.put(key, norm);
		}
	}

	private void storeData(Map<String, String> bugData) {
		try (MongoDBHelper helper = new MongoDBHelper(MongoDBHelper.DEFAULT_DATABASE, BUG_COLLECTION)) {
			helper.storeData(bugData);
		}
	}

	private void analyzeProject(String projectLocation) throws IOException {
		init();
		TextUICommandLine commandLine = new TextUICommandLine();
		FindBugs.processCommandLine(commandLine, new String[] { projectLocation }, engine); // initializes the // for
																							// the engine
		engine.setBugReporter(bugReporter); // use our own bug reporter, default one outputs to console
		FindBugs.runMain(engine, commandLine); // run the analysis

	}

	private Collection<Map<String, String>> collectBugData(BugCollection bugInstanceCollection) {
		Collection<Map<String, String>> bugList = new LinkedList<>();
		for (BugInstance bugInstance : bugInstanceCollection) {
			Map<String, String> bugData = new HashMap<>();
			int priority = bugInstance.getPriority();
			int rank = bugInstance.getBugRank();
			String type = bugInstance.getType();
			String category = bugInstance.getBugPattern().getCategory();
			String className = "";
			for (BugAnnotation annotation : bugInstance.getAnnotations()) {
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
		Map<String, String> result = new HashMap<>();
		for (String cat : getMetricKeys()) {
			result.put(cat, String.valueOf(metricResults.get(cat)));
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

		private String value;

		private MetricKeysImpl(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		Set<Class<? extends IMetricCalculator>> dependencies = new HashSet<Class<? extends IMetricCalculator>>();
		dependencies.add(SourceMeterMetrics.class);
		return dependencies;
	}

	@Override
	public Map<String, Map<String, String>> getClassResults() {
		Map<String, Map<String, String>> result = new HashMap<>();
		for (String className : classResults.keySet()) {
			Map<String, String> classMap = new HashMap<>();
			classResults.get(className).entrySet().stream()
					.forEach(e -> classMap.put(e.getKey(), String.valueOf(e.getValue())));
			result.put(className, classMap);
		}
		return result;
	}

	private class BugReporter extends TextUIBugReporter {
		private SortedBugCollection bugCollection;

		public BugReporter() {
			this.bugCollection = new SortedBugCollection();
			bugCollection.setTimestamp(System.currentTimeMillis());
		}

		@Override
		public void finish() {
			bugCollection.bugsPopulated();
		}

		@Override
		public BugCollection getBugCollection() {
			return bugCollection;
		}

		@Override
		public void observeClass(ClassDescriptor classDescriptor) {
			// gets called when a new class is being checked, no action needed

		}

		@Override
		protected void doReportBug(BugInstance bugInstance) {
			if (bugCollection.add(bugInstance)) {
				notifyObservers(bugInstance);
			}
		}

	}
}
