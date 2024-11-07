package metric.correlation.analysis.calculation.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.os.OperationSystem;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;

import metric.correlation.analysis.GradleBuild;
import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.calculation.MetricCalculatorInitializationException;

import static metric.correlation.analysis.calculation.impl.AndrolyzeMetrics.MetricKeysImpl.*;

public class AndrolyzeMetrics implements IMetricCalculator {

	private static final Logger LOGGER = Logger.getLogger(AndrolyzeMetrics.class);

	public static final String ENV_VAR_ANDROLYZE = "ANDROLYZE";
	public static final String ENV_VAR_MONGOD = "MONGOD";

	private final File androlyzeDir;

	public AndrolyzeMetrics() throws MetricCalculatorInitializationException {
		String andro = System.getenv(ENV_VAR_ANDROLYZE);
		if (andro == null) {
			throw new MetricCalculatorInitializationException("Androlyze environment variable not set!");
		}
		File dir = new File(andro);
		if (!dir.exists()) {
			throw new MetricCalculatorInitializationException(
					"The location \"" + dir.getAbsolutePath() + "\" doesn't exist.");
		}
		if (dir.isFile()) {
			dir = dir.getParentFile();
		}
		if (!new File(dir, "androanalyze").exists()) {
			throw new MetricCalculatorInitializationException(
					"Androlyze executable not found in \"" + dir.getAbsolutePath() + "\".");
		}
		this.androlyzeDir = dir;

		startDatabase();
	}

	private boolean startDatabase() throws MetricCalculatorInitializationException {

		String mongod = System.getenv(ENV_VAR_MONGOD);
		if (mongod == null) {
			throw new MetricCalculatorInitializationException(
					"Environment variable \"" + ENV_VAR_MONGOD + "\" not set.");
		}
		File mongodFile = new File(mongod);
		if (!mongodFile.exists()) {
			throw new MetricCalculatorInitializationException(
					"Location \"" + ENV_VAR_MONGOD + "\" not found.");
		}
		if (mongodFile.isDirectory()) {
			mongodFile = new File(mongodFile, "mongod");
			if (!mongodFile.exists()) {
				throw new MetricCalculatorInitializationException(
						"Location \"" + ENV_VAR_MONGOD + "\" not found.");
			}
		}
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + mongodFile.getAbsolutePath());
				break;
			case LINUX:
				process = run.exec(mongodFile.getAbsolutePath());
				break;
			default:
				throw new MetricCalculatorInitializationException(
						"Program is not compatibel with the Operating System");
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					LOGGER.log(Level.ERROR, "MONGO_DB: " + line);
				}
			}
		} catch (IOException e) {
			throw new MetricCalculatorInitializationException(e);
		}
		return true;
	}

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version,
			final Map<String, String> map) {
		File compiledApk;
		try {
			IProject iproject = project.getProject();
			compiledApk = GradleBuild.buildApk(iproject.getLocation().toFile());
		} catch (UnsupportedOperationSystemException e1) {
			e1.printStackTrace();
			return false;
		}
		String cmd = "cd " + androlyzeDir + " && " + "androlyze.py " + "analyze " + "CodePermissions.py "
				+ "--apks " + compiledApk + " -pm  non-parallel";

		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
				break;
			case LINUX:
				cmd = "./androanalyze scripts_builtin/CodePermissions.py --apks " + compiledApk;
				process = run.exec(cmd, null, androlyzeDir);
				break;
			default:
				return false;
			}

			try (BufferedReader stream = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line;
				while ((line = stream.readLine()) != null) {
					LOGGER.log(Level.ERROR, "ANDROLYZE: " + line);
				}
			}
			process.waitFor();
			process.destroy();

			return true;

		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
	}

	@Override
	public LinkedHashMap<String, String> getResults() {
		File resultsLocation = new File(androlyzeDir, "storage" + File.separator + "res");

		LinkedHashMap<String, String> metricResults = new LinkedHashMap<>();

		int sumPermissions = 0;
		int sumNotUsedPermissions = 0;

		JsonNode jsonNode;
		try {
			jsonNode = JsonLoader.fromFile(resultsLocation);
		} catch (IOException e) {
			metricResults.put(PERMISSIONS.toString(), Double.toString(-1.0));
			return metricResults;
		}
		JsonNode codePermissions = jsonNode.get("code permissions").get("listing");
		Iterator<JsonNode> iterator = codePermissions.iterator();
		while (iterator.hasNext()) {
			sumPermissions++;
			JsonNode next = iterator.next();
			if (next.isArray()) {
				if (!next.elements().hasNext()) {
					sumNotUsedPermissions++;
				}
			} else {
				throw new IllegalStateException("Expected array in Androlize results but parsed: "+next.getNodeType());
			}
		}

		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);

		double permissionMetric;
		if (sumPermissions == 0) {
			permissionMetric = 0;
		} else {
			permissionMetric = (double) sumNotUsedPermissions / (double) sumPermissions;
		}
		metricResults.put(PERMISSIONS.toString(), dFormat.format(permissionMetric));

		LOGGER.log(Level.INFO, "Requested permissions: " + sumPermissions);
		LOGGER.log(Level.INFO, "Unused permissions: " + sumNotUsedPermissions);

		return metricResults;
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toList());
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		return Collections.emptySet();
	}

	/**
	 * The keys of the androlyze metrics
	 * 
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
		PERMISSIONS("PERMISSIONS");

		private String value;

		private MetricKeysImpl(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

}
