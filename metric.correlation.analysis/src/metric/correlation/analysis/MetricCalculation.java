package metric.correlation.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.bson.Document;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.gravity.eclipse.importer.ImportException;
import org.gravity.eclipse.importer.NoRootFolderException;
import org.gravity.eclipse.importer.ProjectImport;
import org.gravity.eclipse.importer.gradle.GradleImport;
import org.gravity.eclipse.importer.maven.MavenImport;
//import org.gravity.eclipse.importer.gradle.GradleImport;
import org.gravity.eclipse.io.FileUtils;
import org.gravity.eclipse.io.GitCloneException;
import org.gravity.eclipse.io.GitTools;

import com.google.common.io.Files;

import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.impl.IssueMetrics;
import metric.correlation.analysis.calculation.impl.SourceMeterMetrics;
import metric.correlation.analysis.calculation.impl.SpotBugsMetrics;
import metric.correlation.analysis.calculation.impl.VersionMetrics;
import metric.correlation.analysis.configuration.ProjectConfiguration;
import metric.correlation.analysis.database.MongoDBHelper;
import metric.correlation.analysis.io.Storage;
import metric.correlation.analysis.io.VersionHelper;
import metric.correlation.analysis.statistic.StatisticExecuter;

/**
 * A class for executing different metric calculations of git java projects and
 * performing statistics on the results
 *
 * @author speldszus
 *
 */
public class MetricCalculation {

	// BEGIN: Configuration variables
	/**
	 * The location where the results should be stored
	 */
	private static final File RESULTS = new File("results");

	private static final boolean REUSE_REPOSITORIES = true;

	/**
	 * The location where the git repositories should be cloned to
	 */
	private static final File REPOSITORIES = new File("repositories");

	/**
	 * The classes of the calculators which should be executed
	 */
	private static final Collection<Class<? extends IMetricCalculator>> METRIC_CALCULATORS = Arrays.asList(
			// HulkMetrics.class,
			SpotBugsMetrics.class, SourceMeterMetrics.class, IssueMetrics.class,
			// VulnerabilitiesPerKLOCMetrics.class, AndrolyzeMetrics.class,
			// CVEMetrics.class,
			VersionMetrics.class);

	// END
	// Don't edit below here

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = LogManager.getLogger(MetricCalculation.class);

	private final SortedSet<IMetricCalculator> calculators;
	private final String timestamp;
	private Set<String> errors;
	private final Storage storage;

	/**
	 * A mapping from metric names to all values for this metric
	 */
	private final LinkedHashMap<String, List<String>> allMetricResults;

	/**
	 * The folder in which the results are stored
	 */
	private final File outputFolder;

	private final File errorFile;

	private List<String> successFullVersions;
	private List<String> notApplicibleVersions;

	/**
	 * Initialized the list of calculators
	 *
	 * @throws IOException If the results file cannot be initialized
	 */
	public MetricCalculation() throws IOException {
		this.errors = new HashSet<>();

		// Get the time stamp of this run
		this.timestamp = new SimpleDateFormat("yyyy-MM-dd_HH_mm").format(new Date());

		// Initialize the metric calculators
		final FileAppender initLogger = addLogAppender("initialization");
		this.calculators = new TreeSet<>();
		for (final Class<? extends IMetricCalculator> clazz : METRIC_CALCULATORS) {
			try {
				this.calculators.add(clazz.getConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				LOGGER.warn(e.getMessage(), e);
			}
		}

		this.allMetricResults = new LinkedHashMap<>();
		// Collect all metric keys
		final Set<String> metricKeys = new HashSet<>();
		for (final IMetricCalculator calculator : this.calculators) {
			metricKeys.addAll(calculator.getMetricKeys());
		}

		// Initialize the metric results
		for (final String metricKey : metricKeys) {
			this.allMetricResults.put(metricKey, new LinkedList<>());
		}
		this.outputFolder = new File(RESULTS, "Results-" + this.timestamp);
		this.storage = new Storage(new File(this.outputFolder, "results.csv"), metricKeys);
		this.errorFile = new File(this.outputFolder, "errors.csv");
		Files.write("vendor,product,version,errors\n".getBytes(), this.errorFile);
		dropLogAppender(initLogger);
	}

	/**
	 * The main method for calculating metrics for multiple versions of multiple
	 * projects. This method has to be called from a running eclipse workspace!
	 *
	 * @param configurations The project configurations which should be considered
	 */
	public boolean calculateAll(final Collection<ProjectConfiguration> configurations) {
		for (final ProjectConfiguration config : configurations) {
			if (!calculate(config)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The main method for calculating metrics for multiple versions of a project.
	 * This method has to be called from a running eclipse workspace!
	 *
	 * @param configurations The project configuration which should be considered
	 */
	public boolean calculate(final ProjectConfiguration config) {
		this.successFullVersions = new ArrayList<>();
		this.notApplicibleVersions = new ArrayList<>();

		// Create a project specific file logger
		final FileAppender fileAppender = addLogAppender(config);

		// Reset previously recored errors
		this.errors = new HashSet<>();

		// Clone the project
		boolean success = true;
		final String productName = config.getProductName();
		final String vendorName = config.getVendorName();
		final File srcLocation = new File(REPOSITORIES, productName);
		if (REUSE_REPOSITORIES && srcLocation.exists()) {
			cleanGradle(srcLocation);

			try (Git git = Git.open(srcLocation)) {
				for (final Entry<String, String> entry : config.getVersionCommitIdPairs()) {
					final String commitId = entry.getValue();
					final String version = entry.getKey();
					try {
						git.reset().setMode(ResetType.HARD).setRef(commitId).call();
					} catch (final Exception e) {
						LOGGER.error("Error while checking out commit", e);
						return false;
					}
					success &= setMetricResults(productName, vendorName, version, srcLocation);
				}
				dropLogAppender(fileAppender);
				return success;
			} catch (final IOException e) {
				LOGGER.error("Error while opening location as git repository", e);
				return false;
			}

		}
		try (GitTools git = new GitTools(config.getGitUrl(), REPOSITORIES, true, true)) {

			// Calculate metrics for each commit of the project configuration
			for (final Entry<String, String> entry : config.getVersionCommitIdPairs()) {
				final String commitId = entry.getValue();
				final String version = entry.getKey();
				LOGGER.log(Level.INFO, "\n\n\n#############################");
				LOGGER.log(Level.INFO, "### " + this.timestamp + " ###");
				LOGGER.log(Level.INFO, "#############################");
				LOGGER.log(Level.INFO, "Checkingout commit : " + commitId);
				LOGGER.log(Level.INFO, "#############################\n");

				// Checkout the specific commit
				if (!git.changeVersion(commitId)) {
					success = false;
					this.errors.add("change commit");
					LOGGER.warn("Skipped commit: " + commitId);
					continue;
				}
				FileUtils.recursiveDelete(new File(RESULTS, "SourceMeter"));

				success &= setMetricResults(productName, vendorName, version, srcLocation);
			}
		} catch (GitCloneException | IOException e) {
			LOGGER.log(Level.ERROR, e);
			success = false;
		}

		// Drop the project specific file logger
		dropLogAppender(fileAppender);
		return success;
	}

	private void cleanGradle(final File srcLocation) {
		final File buildGradle = new File(srcLocation.getAbsolutePath(), "build.gradle");
		String cleanContent = "";
		if (buildGradle.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(buildGradle))) {
				final StringBuilder content = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					if (line.contains("task exportCode")) {
						break;
					}
					content.append(line + "\r\n");
				}
				cleanContent = content.toString();
			} catch (final IOException e) {
				LOGGER.log(Level.WARN, "could not clean build.gradle");
			}
			if (!cleanContent.isEmpty()) {
				try (FileWriter fw = new FileWriter(buildGradle)) {
					fw.write(cleanContent);
				} catch (final IOException e) {
					LOGGER.log(Level.WARN, "could not clean build.gradle");
				}
			}
		}
	}

	private boolean setMetricResults(final String productName, final String vendorName, final String version,
			final File srcLocation) {
		boolean success = true;
		LOGGER.log(Level.INFO, "Start metric calculation");
		try {
			success &= calculateMetrics(productName, vendorName, version, srcLocation);
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			success = false;
		}
		if (success) {
			this.successFullVersions.add(version);
		} else {
			LOGGER.log(Level.ERROR, "\n### METRIC CALCULATION FAILED ###\nproject: " + productName + "\nvendor: "
					+ vendorName + "\nversion: " + version + "\n### ###");
		}
		return success;
	}

	public IJavaProject importProject(final File src, final boolean ignoreBuildErrors) throws ImportException {
		LOGGER.log(Level.INFO, "Importing  project to Eclipse workspace");
		if (!src.exists()) {
			final String message = "src folder does not exist";
			this.errors.add(message);
			throw new ImportException(message);
		}
		ProjectImport projectImport;
		if (Arrays.stream(src.listFiles()).anyMatch(f -> f.getName().contentEquals("build.gradle"))) {
			try {
				projectImport = new GradleImport(src, ignoreBuildErrors);
			} catch (IOException | ImportException e) {
				this.errors.add("new GradleImport()");
				throw new ImportException(e);
			}
		} else if (Arrays.stream(src.listFiles()).anyMatch(f -> f.getName().contentEquals("pom.xml"))) {
			try {
				projectImport = new MavenImport(src, ignoreBuildErrors);
			} catch (final ImportException e) {
				this.errors.add("new MavenImport()");
				throw new ImportException(e);
			}
		} else {
			final String message = "not maven or gradle project";
			this.errors.add(message);
			throw new ImportException(message);
		}

		IJavaProject project;

		try {
			project = projectImport.importProject(new NullProgressMonitor());
		} catch (final NoRootFolderException e) {
			this.errors.add(projectImport.getClass().getSimpleName());
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			throw new ImportException(e);
		} catch (final ImportException e) {
			this.errors.add(projectImport.getClass().getSimpleName());
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			Thread.currentThread().interrupt();
			throw e;
		}
		return project;
	}

	/**
	 * Creates a new project specific logger with an new output file for a project
	 * configuration
	 *
	 * @param config The project configuration
	 * @return The project specific logger
	 */
	private FileAppender addLogAppender(final ProjectConfiguration config) {
		return addLogAppender(config.getVendorName() + '-' + config.getProductName());
	}

	/**
	 * Creates a log appender which appends to a file with the given name
	 *
	 * @param name The file name
	 * @return The logger
	 */
	private FileAppender addLogAppender(final String name) {
		FileAppender fileAppender = null;
		try {
			fileAppender = new FileAppender(new PatternLayout("%d %-5p [%c{1}] %m%n"),
					"logs/" + this.timestamp + '/' + name + ".txt");
			fileAppender.setThreshold(Level.ALL);
			fileAppender.activateOptions();
			Logger.getRootLogger().addAppender(fileAppender);
		} catch (final IOException e) {
			LOGGER.warn("Adding file appender failed!");
		}
		return fileAppender;
	}

	/**
	 * Drops the log appender
	 *
	 * @param fileAppender the logger
	 */
	private void dropLogAppender(final FileAppender fileAppender) {
		if (fileAppender != null) {
			Logger.getRootLogger().removeAppender(fileAppender);
		}
	}

	/**
	 * Calculate the correlation metrics
	 *
	 * @param productName The name of the software project
	 * @param vendorName  The name of the projects vendor
	 * @param version     The version which should be inspected
	 * @param src         The location of the source code
	 * @return true if everything went okay, otherwise false
	 */
	private boolean calculateMetrics(final String productName, final String vendorName, final String version,
			final File src) {
		// Import the sourcecode as maven or gradle project
		IJavaProject project;
		try {
			project = importProject(src, true);
		} catch (final ImportException e) {
			return false;
		}
		if (project == null) {
			return false;
		}

		// Calculate all metrics
		boolean success = true;
		final HashMap<String, String> results = new HashMap<>();
		for (final IMetricCalculator calc : this.calculators) {
			LOGGER.log(Level.INFO, "Execute metric calculation: " + calc.getClass().getSimpleName());
			try {
				if (calc.calculateMetric(project, productName, vendorName, version,
						Collections.unmodifiableMap(results))) {
					results.putAll(calc.getResults());
					success &= plausabilityCheck(calc);
					/*
					 * if (calc instanceof IMetricClassCalculator) {
					 * LOGGER.info("processing class metrics"); processClassMetrics(productName,
					 * vendorName, version, ((IMetricClassCalculator) calc).getClassResults()); }
					 */
				} else {
					this.errors.add(calc.getClass().getSimpleName());
					success = false;
				}
			} catch (final Exception e) {
				success = false;
				this.errors.add(calc.getClass().getSimpleName());
				LOGGER.log(Level.ERROR, "A detection failed with an Exception: " + e.getMessage(), e);
			}
		}
		LOGGER.info("writing to log");
		// Store all results in a csv file
		if (!this.storage.writeCSV(productName, results)) {
			LOGGER.log(Level.ERROR, "Writing results for \"" + productName + "\" failed!");
			this.errors.add("Writing results");
			success = false;
		}

		// If all metrics have been calculated successfully add them to the metric
		// results and store them in the database
		if (success) {
			LOGGER.info("success writing to db");
			try (MongoDBHelper dbHelper = new MongoDBHelper()) {
				dbHelper.storeMetrics(results, false);
			} catch (final Exception e) {
				LOGGER.error("could not store results in database");
				LOGGER.log(Level.ERROR, e.getStackTrace());
			}
			for (final Entry<String, String> entry : results.entrySet()) {
				if (!this.allMetricResults.containsKey(entry.getKey())) {
					this.allMetricResults.put(entry.getKey(), new LinkedList<>());
				}
				this.allMetricResults.get(entry.getKey()).add(entry.getValue());
			}
		} else {
			try (FileWriter writer = new FileWriter(this.errorFile, true)) {
				writer.append(vendorName);
				writer.append(',');
				writer.append(productName);
				writer.append(',');
				writer.append(version);
				writer.append(',');
				writer.append(this.errors.stream().collect(Collectors.joining(" - ")));
				writer.append('\n');
			} catch (final IOException e) {
				LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			}
		}
		LOGGER.info("finished writing");
		return success;
	}

	private void processClassMetrics(final String productName, final String vendorName, final String version,
			final Map<String, Map<String, String>> classResults) {
		try (MongoDBHelper helper = new MongoDBHelper(MongoDBHelper.DEFAULT_DATABASE, MongoDBHelper.CLASS_COLLECTION)) {
			helper.storeClassMetrics(productName, vendorName, version, classResults);
		}

	}

	/**
	 * Checks if the results of the metric calculator are plausible
	 *
	 * @param calc The executed metric calculator
	 * @return true iff the results are plausible
	 */
	private boolean plausabilityCheck(final IMetricCalculator calc) {
		for (final String value : calc.getResults().values()) {
			if ((value == null) || value.isEmpty() || Double.toString(Double.NaN).equals(value)) {
				this.errors.add("Values not plausible: " + calc.getClass().getSimpleName());
				return false;
			}
		}
		return true;
	}

	/**
	 * Cleans the repositories folder
	 *
	 * @return true, iff everything has been deleted
	 */
	public static boolean cleanupRepositories() {
		return FileUtils.recursiveDelete(REPOSITORIES);
	}

	/**
	 * Returns the errors of the last run
	 *
	 * @return A Set of error messages
	 */
	public Set<String> getLastErrors() {
		return this.errors;
	}

	/**
	 * Executes the statistic calculation on matching projects stored in the mondodb
	 * database
	 *
	 * @param metrics the list of metrics that are being considered
	 * @return true, if no error occured
	 */
	public boolean performStatistics(final List<String> metrics) {
		final Document existsFilter = new Document();
		existsFilter.append("$exists", 1);
		final Document filterDoc = new Document();
		for (final String metric : metrics) {
			filterDoc.append(metric, existsFilter);
		}
		try (MongoDBHelper mongoHelper = new MongoDBHelper()) {
			mongoHelper.getMetrics(filterDoc);
		}
		return true;
	}

	/**
	 * Executes the statistic calculation on all projects discovered with this
	 * instance
	 *
	 * @return true, iff the results have been stored successfully
	 */
	public boolean performStatistics() {
		final LinkedHashMap<String, List<Double>> newestVersionOnly = new LinkedHashMap<>();

		// Find indexes of all newest versions
		final List<String> productNames = this.allMetricResults
				.get(metric.correlation.analysis.calculation.impl.VersionMetrics.MetricKeysImpl.PRODUCT.toString());
		final List<String> versions = this.allMetricResults
				.get(metric.correlation.analysis.calculation.impl.VersionMetrics.MetricKeysImpl.VERSION.toString());

		final Map<String, Integer> productToNewestIndex = new HashMap<>();
		final Map<String, String> productToNewestVersion = new HashMap<>();

		for (int index = 0; index < versions.size(); index++) {

			final String product = productNames.get(index);
			final String newVersion = versions.get(index);
			if (productToNewestVersion.containsKey(product)) {

				final String prevVersion = productToNewestVersion.get(product);

				if (VersionHelper.compare(prevVersion, newVersion) == -1) {
					productToNewestVersion.put(product, newVersion);
					productToNewestIndex.put(product, index);
				}

			} else {
				productToNewestVersion.put(product, newVersion);
				productToNewestIndex.put(product, index);
			}
		}

		// Remove the version key completely
		// auch hier die andere liste nutzen
		this.allMetricResults.remove(VersionMetrics.MetricKeysImpl.PRODUCT.toString());
		this.allMetricResults.remove(VersionMetrics.MetricKeysImpl.VENDOR.toString());
		this.allMetricResults.remove(VersionMetrics.MetricKeysImpl.VERSION.toString());

		// Add them to newestVersionOnly
		for (final Entry<String, List<String>> entry : this.allMetricResults.entrySet()) {
			final List<Double> newestMetrics = new LinkedList<>();
			for (final String value : entry.getValue()) {
				newestMetrics.add(Double.valueOf(value));
			}
			newestVersionOnly.put(entry.getKey(), newestMetrics);
		}

		try {
			if (newestVersionOnly.size() > 1) {
				new StatisticExecuter().calculateStatistics(newestVersionOnly, this.outputFolder);
			} else {
				LOGGER.warn("Skipped calculation of correlation matrix");
			}
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			return false;
		}
		return true;
	}

	public List<String> getSuccessFullVersions() {
		return this.successFullVersions;
	}

	public List<String> getNotApplicibleVersions() {
		return this.notApplicibleVersions;
	}
}