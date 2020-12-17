package metric.correlation.analysis.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.InitializationError;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;

import metric.correlation.analysis.MetricCalculation;
import metric.correlation.analysis.configuration.ProjectConfiguration;
import metric.correlation.analysis.projectSelection.ProjectsOutputCreator;

@RunWith(Parameterized.class)
public class Execution {

	// BEGIN CONSTANTS -->

	/**
	 * The maximum amount of projects which should be considered
	 */
	private static final int MAX_NUMBER_OF_PROJECTS = 40;

	/**
	 * From which project should be started
	 */
	private static final int OFFSET_FOR_PROJECTS = 0;

	/**
	 * The maximum amount of versions per projects which should be considered
	 */
	private static final int MAX_VERSIONS_OF_PROJECTS = 1;

	/**
	 * If all data should be cleaned after an execution
	 */
	private static final boolean CLEAN = false;

	/**
	 * If the JSON project file should be validated against the given Schema
	 */
	private static final boolean VALIDATE_JSON = false;

	/**
	 * The level at which all loggers should log
	 */
	private static final Level LOG_LEVEL = Level.ERROR;

	/**
	 * Names of projects which should be excluded (maybe overridden by INCLUDES)
	 */
	private static final String[] EXCLUDES = new String[] {//"grpc-grpc-java", //"igniterealtime-Smack", "JabRef-jabref", "apache-groovy", "reactor-reactor-core", 
	// Permanenet excludes:		
	"apache-ignite", "elastic-elasticsearch"};

	/**
	 * Names of projects which should be included in any case (Overrides all
	 * excludes)
	 */
	private static final String[] INCLUDES = new String[] {};

	// BEGIN CONSTANTS -->

	/**
	 * All projects which timed out are stored here
	 */
	private static final File TIMEOUT_FILE = new File("timeout.txt");

	/**
	 * Path to a file containing one exclude per line. Successfully processed
	 * projects are automatically appended to this file!
	 */
	private static final File EXCLUDES_FOLDER = new File("excludes");
	// <-- END CONSTANTS
	//
	// Don't edit below here

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(Execution.class);

	private static MetricCalculation calculator;
	private ProjectConfiguration config;

	public Execution(String projectName, ProjectConfiguration config, Integer index, Collection<String> versions) {
		this.config = config;
	}

	@Parameters(name = "{2} - {0}: versions {3}")
	public static Collection<Object[]> collectProjects() throws IOException, ProcessingException {
		Map<String, Collection<String>> excludedProjectNames = getExcludes();

		File projectsReleaseDataJSON = new File(ProjectsOutputCreator.PROJECTS_DATA_OUTPUT_FILE_NORMALIZED);

		JsonNode projectsJsonData = JsonLoader.fromFile(projectsReleaseDataJSON);
		if (VALIDATE_JSON && !checkDocument(projectsJsonData)) {
			throw new IllegalArgumentException("The given JSON file doesn't comply with the JSON Schema!");
		}

		ArrayNode projects = (ArrayNode) projectsJsonData.get("projects");
		if (projects.size() == 0) {
			throw new IllegalArgumentException("There are no projects in the JSON documnet!");
		}

		int projectCounter = 0;
		int skipedProjects = 0;
		List<Object[]> configs = new ArrayList<>(Math.min(MAX_NUMBER_OF_PROJECTS, projects.size()));
		for (JsonNode project : projects) {
			projectCounter++;
			if (OFFSET_FOR_PROJECTS >= projectCounter) {
				continue;
			}
			if (projectCounter > MAX_NUMBER_OF_PROJECTS + OFFSET_FOR_PROJECTS + skipedProjects) {
				break;
			}

			String productName = project.get("productName").asText();
			String vendorName = project.get("vendorName").asText();

			boolean skip = false;
			Collection<String> excludedVersions = Collections.emptyList();
			String name = vendorName + "-" + productName;
			for (Entry<String, Collection<String>> exclude : excludedProjectNames.entrySet()) {
				if (exclude.getKey().equals(name)) {
					if (exclude.getValue().contains("*")) {
						skip = true;
						break;
					} else {
						excludedVersions = exclude.getValue(); // code missing?
					}
				}
			}

			if (skip) {
				skipedProjects++;
				continue;
			}

			String gitURL = project.get("url").asText();
			ArrayNode commits = (ArrayNode) project.get("commits");

			HashMap<String, String> commitsAndVersions = new HashMap<>();

			for (JsonNode commit : commits) {
				if (commitsAndVersions.size() >= MAX_VERSIONS_OF_PROJECTS) {
					break;
				}
				String commitId = commit.get("commitId").asText();
				String commitVersion = commit.get("version").asText();

				if (excludedVersions.contains(commitVersion)) {
					continue;
				}

				commitsAndVersions.put(commitVersion, commitId);
			}

			if (commitsAndVersions.size() > 0) {
				ProjectConfiguration projectConfiguration = new ProjectConfiguration(productName, vendorName, gitURL,
						commitsAndVersions);
				configs.add(new Object[] { vendorName + "-" + productName, projectConfiguration,
						projectCounter, commitsAndVersions.keySet()});
			}
			else {
				skipedProjects++;
			}
		}
		return configs;

	}

	/**
	 * Searches all excluded projects
	 * 
	 * @return The names and commits of the excluded projects
	 * @throws IOException
	 */
	private static HashMap<String, Collection<String>> getExcludes() throws IOException {
		HashMap<String, Collection<String>> excludes = new HashMap<>();
		for (File exclude : EXCLUDES_FOLDER.listFiles()) {
			excludes.put(exclude.getName(), Files.readAllLines(exclude.toPath(), Charset.defaultCharset()));
		}
		excludes.keySet().removeAll(Arrays.asList(INCLUDES));
		for (String include : EXCLUDES) {
			excludes.put(include, Collections.singleton("*"));
		}
		return excludes;
	}

	/**
	 * Adds the current project to the excludes file
	 * 
	 * @return true, iff the project has been added
	 */
	private boolean addExcludes() {
		List<String> success = calculator.getSuccessFullVersions();
		List<String> ignore = calculator.getNotApplicibleVersions();
		Collection<String> versions = new ArrayList<>(success.size() + ignore.size());
		versions.addAll(success);
		versions.addAll(ignore);
		File file = new File(EXCLUDES_FOLDER, getProjectId());
		try {
			Files.write(file.toPath(), versions, file.exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e);
			return false;
		}
		return true;
	}

	/**
	 * Checks the JSON document against the schema used by this application
	 * 
	 * @param projectsJsonData The JSON document the schema
	 * @return true, iff the JSON document complies with the schema
	 * @throws IOException         Iff the JSON Schema cannot be read
	 * @throws ProcessingException Iff processing the va	lidation had an error
	 */
	static boolean checkDocument(JsonNode projectsJsonData) throws IOException, ProcessingException {
		ProcessingReport report = null;
		JsonNode schemaNode = JsonLoader.fromFile(new File("schema.json"));

		JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
		report = validator.validate(schemaNode, projectsJsonData);
		if (!report.isSuccess()) {
			LOGGER.warn("The project configuration is not valid!");
			return false;
		}
		return true;
	}

	@Test(timeout = (long) ( 4*60 * 60 * 1000))
	public void execute() {
		LOGGER.getRootLogger().setLevel(Level.ALL);
		if (config.getGitCommitIds().isEmpty()) {
			fail("No commits available");
		}
		addProjectNameToFile(TIMEOUT_FILE);
		boolean success = calculator.calculate(config);
		//addExcludes();
		if (!success) {
			fail(calculator.getLastErrors().stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]")));
		}
		try {
			Files.write(TIMEOUT_FILE.toPath(), Files.readAllLines(TIMEOUT_FILE.toPath()).stream()
					.filter(line -> !getProjectId().equals(line)).collect(Collectors.joining("\n")).getBytes());
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
		}
		assertTrue(success);
	}

	/**
	 * Adds the current project to the given file
	 * 
	 * @param file The file
	 * @return true, iff the project has been added
	 */
	public boolean addProjectNameToFile(final File file) {
		final String name = getProjectId();
		try {
			Files.write(file.toPath(), (name + '\n').getBytes(),
					file.exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		} catch (IOException e) {
			LOGGER.warn("Couldn't append project to excludes: " + name);
			return false;
		}
		return true;
	}

	/**
	 * Returns an ID consisting out of the name of vendor and product
	 * 
	 * @return
	 */
	private String getProjectId() {
		return config.getVendorName() + "-" + config.getProductName();
	}

	/**
	 * Clean the repository folder before test execution and initialize metric
	 * calculation
	 * 
	 * @throws InitializationError If the cleanup or initialization failed
	 */
	@BeforeClass
	public static void initialize() throws InitializationError {
		Logger.getRootLogger().setLevel(LOG_LEVEL);
		if (CLEAN && !MetricCalculation.cleanupRepositories()) {
			throw new InitializationError("Couldn't clean repositories");
		}
		try {
			calculator = new MetricCalculation();
		} catch (IOException e) {
			throw new InitializationError(e);
		}
	}

	/**
	 * Calculate all statistics and clean the repository folder after test execution
	 * if CLEAN == true
	 * 
	 * @throws InitializationError Iff the repositories couldn't be cleaned
	 */
	@AfterClass
	public static void after() throws InitializationError {
		calculator.performStatistics();
		if (!CLEAN) {
			return;
		}
		if (!MetricCalculation.cleanupRepositories()) {
			throw new InitializationError("Couldn't clean repositories");
		}
	}
}
