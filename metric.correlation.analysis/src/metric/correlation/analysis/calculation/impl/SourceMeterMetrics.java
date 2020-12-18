package metric.correlation.analysis.calculation.impl;

import static metric.correlation.analysis.calculation.impl.SourceMeterMetrics.MetricKeysImpl.DIT;
import static metric.correlation.analysis.calculation.impl.SourceMeterMetrics.MetricKeysImpl.DIT_MAX;
import static metric.correlation.analysis.calculation.impl.SourceMeterMetrics.MetricKeysImpl.LLOC;
import static metric.correlation.analysis.calculation.impl.SourceMeterMetrics.MetricKeysImpl.LOC_PER_CLASS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.IMetricClassCalculator;
import metric.correlation.analysis.calculation.MetricCalculatorInitializationException;
import metric.correlation.analysis.commands.CommandExecuter;
import metric.correlation.analysis.database.MongoDBHelper;

public class SourceMeterMetrics implements IMetricClassCalculator {

	public static final String ENV_VARIABLE_NAME = "SOURCE_METER_JAVA"; //$NON-NLS-1$

	private static final Logger LOGGER = Logger.getLogger(SourceMeterMetrics.class);

	private final File sourceMeterExecutable;
	private final File tmpResultDir;

	private String lastProjectName;
	private LinkedHashMap<String, String> lastResults;
	private final Map<String, Map<String, String>> classResults = new HashMap<>();
	private static final boolean USE_DATABASE = true;

	public SourceMeterMetrics() throws MetricCalculatorInitializationException {
		final String sourcemeter = System.getenv(ENV_VARIABLE_NAME);
		if (sourcemeter == null) {
			throw new MetricCalculatorInitializationException("SourceMeterJava environment variable not set!");
		} else {
			this.sourceMeterExecutable = new File(sourcemeter);
			try {
				this.sourceMeterExecutable.toPath().getFileSystem().provider()
				.checkAccess(this.sourceMeterExecutable.toPath(), AccessMode.EXECUTE);
			} catch (final IOException e) {
				throw new MetricCalculatorInitializationException(e);
			}
		}
		try {
			this.tmpResultDir = Files.createTempDirectory("SourceMeter").toFile();
		} catch (final IOException e1) {
			throw new MetricCalculatorInitializationException(e1);
		}
	}

	@Override
	public boolean calculateMetric(final IJavaProject project, final String productName, final String vendorName,
			final String version, final Map<String, String> map) {
		if (USE_DATABASE) {
			this.lastResults = new LinkedHashMap<>();
			this.lastProjectName = project.getProject().getName();
			try (MongoDBHelper helper = new MongoDBHelper()) {
				final Map<String, Object> filter = new HashMap<>();
				filter.put("product", productName);
				filter.put("vendor", vendorName);
				filter.put("version", version);
				final List<Map<String, String>> storedResultsList = helper.getMetrics(filter);
				if (storedResultsList.size() > 1) {
					LOGGER.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				}
				final Map<String, String> storedResults = storedResultsList.get(0);
				for (final String key : getMetricKeys()) {
					this.lastResults.put(key, storedResults.get(key));
				}
			}
			return true;
		}
		final String projectLocation = project.getProject().getLocation().toFile().getParentFile().getParentFile()
				.getAbsolutePath() + File.separator + "repositories" + File.separator + productName;
		this.lastProjectName = project.getProject().getName();
		final String cmd = this.sourceMeterExecutable + " -projectName=" + this.lastProjectName + //$NON-NLS-1$
				" -projectBaseDir=" + projectLocation + //$NON-NLS-1$
				" -resultsDir=" + this.tmpResultDir.getAbsolutePath()
				+ " -runAndroidHunter=false -runVulnerabilityHunter=false "
				+ "-runFB=false -runRTEHunter=false -runMetricHunter=false"; //$NON-NLS-1$

		try {
			if (!CommandExecuter.executeCommand(new File(projectLocation), cmd)) {
				return false;
			}
		} catch (final UnsupportedOperationSystemException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			return false;
		}
		return calculateResults();

	}

	@Override
	public LinkedHashMap<String, String> getResults() {
		if (this.lastResults == null) {
			throw new IllegalStateException("The calculateMetrics() operation hasn't been executed or failed!");
		}
		LOGGER.info("results returned");
		return this.lastResults;
	}

	private boolean calculateResults() {
		final File sourceMeterOutputFolder = getOutputFolder();
		final DecimalFormat dFormat = getFormatter();

		this.lastResults = new LinkedHashMap<>();
		final List<Map<String, String>> classContent = new LinkedList<>();
		try {
			parseMetricFile(new File(sourceMeterOutputFolder, this.lastProjectName + "-Class.csv"), classContent);
			parseMetricFile(new File(sourceMeterOutputFolder, this.lastProjectName + "-Enum.csv"), classContent);
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
		final Collection<? extends String> metricKeys = getMetricKeys();
		for (final String metricName : metricKeys) {
			if (LOC_PER_CLASS.toString().equals(metricName) || DIT_MAX.toString().equals(metricName)) {
				// LOC_PER_CLASS accesses the same entry as LLOC
				continue;
			}
			double sum = 0;
			double max = Double.MIN_VALUE;

			for (final Map<String, String> valueMap : classContent) {
				final double v = Double.parseDouble(valueMap.get(metricName));
				sum += v;
				if (v > max) {
					max = v;
				}
			}
			final double average = Double.parseDouble(dFormat.format(sum / classContent.size()));
			if (metricName.equals(DIT.toString())) {
				this.lastResults.put(DIT_MAX.toString(), Double.toString(max));
			}
			if (metricName.equals(LLOC.toString())) {
				this.lastResults.put(LLOC.toString(), dFormat.format(sum));
				this.lastResults.put(LOC_PER_CLASS.toString(), Double.toString(average));
			} else {
				this.lastResults.put(metricName, Double.toString(average));
			}
		}
		return true;
	}

	private void parseMetricFile(final File metrics, final List<Map<String, String>> content) throws IOException {
		final Map<String, Integer> metricIndex = new HashMap<>();
		if (!metrics.exists()) {
			throw new IllegalStateException("File to parse does not exist: " + metrics.getAbsolutePath());
		}
		try (final BufferedReader fileReader = new BufferedReader(new FileReader(metrics))) {
			initIndex(metricIndex, fileReader.readLine());
			String mLine;
			while ((mLine = fileReader.readLine()) != null) {
				final String[] cvsValues = getCsvValues(mLine);
				final Map<String, String> metricMap = new HashMap<>();
				for (final Entry<String, Integer> entry : metricIndex.entrySet()) {
					metricMap.put(entry.getKey(), cvsValues[entry.getValue()]);
				}
				final String className = metricMap.get("LongName");
				metricMap.remove(className);
				this.classResults.put(className, metricMap);
				content.add(metricMap);
			}
		}
	}

	private void initIndex(final Map<String, Integer> metricIndex, final String line) {
		final Collection<String> metricKeys = getMetricKeys();
		final String[] sourceMeterKeys = getCsvValues(line);
		for (int i = 0; i < sourceMeterKeys.length; i++) {
			if (metricKeys.contains(sourceMeterKeys[i]) || sourceMeterKeys[i].equals("LongName")) {
				metricIndex.put(sourceMeterKeys[i], i);
			}
		}
	}

	private String[] getCsvValues(final String line) {
		return line.substring(1, line.length() - 1).split("\",\"");
	}

	private File getOutputFolder() {
		final File[] outputFolderDir = new File(new File(this.tmpResultDir.getAbsolutePath(), this.lastProjectName),
				"java") //$NON-NLS-1$
				.listFiles();
		if ((outputFolderDir == null) || (outputFolderDir.length == 0)) {
			throw new IllegalStateException("There are no output files!");
		}
		return outputFolderDir[0];
	}

	public static DecimalFormat getFormatter() {
		final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		return new DecimalFormat("0.00", dfs);
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toSet());
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		return Collections.emptySet();
	}

	/**
	 * The keys of the SourceMeter metrics
	 *
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
		LDC("LDC"), DIT("DIT"), DIT_MAX("DIT_MAX"), LCOM("LCOM5"), RFC("RFC"), NOC("NOC"), CBO("CBO"), WMC("WMC"),
		LLOC("LLOC"), LOC_PER_CLASS("LOCpC");

		private String value;

		MetricKeysImpl(final String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}

	@Override
	public Map<String, Map<String, String>> getClassResults() {
		return this.classResults;
	}
}
