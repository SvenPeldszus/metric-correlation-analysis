package metric.correlation.analysis.selection;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.search.SearchHit;
import org.gravity.eclipse.io.FileUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class ProjectsOutputCreator {

	/**
	 * Frequently used identifiers
	 */
	private static final String COMMITS = "commits";
	private static final String URL = "url";
	private static final String VENDOR_NAME = "vendorName";
	private static final String PRODUCT_NAME = "productName";
	private static final String PROJECTS = "projects";
	private static final String NAME = "name";
	private static final String COMMIT = "commit";
	private static final String VERSION = "version";
	private static final String COMMIT_ID = "commitId";

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(ProjectsOutputCreator.class);

	/**
	 * The location of the JSON file containing the project information
	 */
	public static final String PROJECTS_DATA_OUTPUT_FILE = "input/projectsReleaseData2.json";
	public static final String PROJECTS_DATA_OUTPUT_FILE_NORMALIZED = "input/projectsReleaseData-normalized.json";

	private static final int MAX_COMMITS = 1;
	private static final int MAX_PROJECTS = 20;

	/**
	 * @author Antoniya Ivanova - prepares the JSON output for the repository
	 *         search, includes the releases for each repository and what
	 *         version/commit they relate to.
	 *
	 */
	public void getProjectReleases() {

		final Set<SearchHit> repositoriesWithCVEs = new GitHubProjectSelector().getProjectsWithAtLeastOneVulnerability();

		final JsonObject resultJSON = new JsonObject();
		final JsonArray resultArray = new JsonArray();
		resultJSON.add(PROJECTS, resultArray);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

			// Iterate the vulnerable projects
			for (final SearchHit repository : repositoriesWithCVEs) {
				final JsonObject projectJSON = new JsonObject();

				final Map<String, Object> map = repository.getSourceAsMap();

				final String productName = map.get("Product").toString();
				final String vendorName = map.get("Vendor").toString();
				final String url = "http://www.github.com/" + vendorName + "/" + productName + ".git";

				projectJSON.addProperty(PRODUCT_NAME, productName);
				projectJSON.addProperty(VENDOR_NAME, vendorName);
				projectJSON.addProperty(URL, url);

				final JsonArray commits = getReleaseCommits(httpClient, vendorName, productName);
				projectJSON.add(COMMITS, commits);

				if (commits.size() != 0) {
					resultArray.add(projectJSON);
				}

			}

			FileUtils.createDirectory("Resources");

			try (FileWriter fileWriter = new FileWriter(PROJECTS_DATA_OUTPUT_FILE)) {
				fileWriter.write(resultJSON.toString());
			}
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

	}

	public JsonArray getReleaseCommits(final CloseableHttpClient httpClient, final String vendorName,
			final String productName) throws IOException {
		return getReleaseCommits(httpClient, vendorName, productName, MAX_COMMITS);
	}

	public JsonArray getReleaseCommits(final CloseableHttpClient httpClient, final String vendorName,
			final String productName, final Integer commitLimit) throws IOException {
		final JsonArray commits = new JsonArray();
		// Iterate over the project release pages
		for (int i = 1; i < 100; i++) {
			if ((GitHubProjectSelector.gitRequests++ % 20) == 0) {
				try {
					TimeUnit.MINUTES.sleep(1);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Couldn't read release commits due to interruption at waiting.", e);
				}
			}
			// Respect 30 request limit of GitHub API

			final String gitURL = "https://api.github.com/repos/" + vendorName + "/" + productName + "/tags?page=" + i
					+ "&per_page=100";

			final HttpGet request = new HttpGet(gitURL);
			request.addHeader("Authorization", "Token " + GitHubProjectSelector.OAuthToken);
			request.addHeader("content-type", "application/json");

			final HttpResponse result = httpClient.execute(request);
			if(result.getStatusLine().getStatusCode() != 200) {
				throw new IOException(result.getStatusLine().toString());
			}
			final String json = EntityUtils.toString(result.getEntity(), "UTF-8");
			final JsonElement jsonObject = new JsonParser().parse(json);
			if (jsonObject.isJsonObject()) {
				throw new IOException(((JsonObject) jsonObject).get("message").toString());
			}
			final JsonArray jarray = jsonObject.getAsJsonArray();

			if ((jarray.size() == 0) || getCommitsForPage(commits, jarray, commitLimit)) {
				break;
			}
		}
		return commits;
	}

	/**
	 *
	 * @param commits
	 * @param jarray
	 * @param limit
	 * @return returns true, if the limit of commits has been reached
	 */
	private boolean getCommitsForPage(final JsonArray commits, final JsonArray jarray, final Integer limit) {
		// Iterate over the project releases on the page
		for (int j = 0; j < jarray.size(); j++) {

			final JsonObject commit = new JsonObject();
			final JsonObject jo = (JsonObject) jarray.get(j);

			commit.addProperty(COMMIT_ID,
					jo.get(COMMIT).getAsJsonObject().get("sha").toString().replace("\"", ""));
			final String version = jo.get(NAME).toString().replace("\"", "");
			commit.addProperty(VERSION, version);
			if (!version.toLowerCase()
					.matches(".*(\\.|-|_|^)(snapshot|doc|pre|alpha|beta|rc|m|prototype)(?![a-z]).*")) {
				commits.add(commit);
				if (commits.size() >= limit) {
					return true;
				}
			}
		}
		return false;
	}

	@Test
	public void testFindProjects() {
		Logger.getRootLogger().setLevel(Level.ALL);
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final JsonObject resultJSON = new JsonObject();
		final JsonArray resultArray = new JsonArray();
		resultJSON.add(PROJECTS, resultArray);
		final Set<Repository> reps = new GitHubProjectSelector().searchForJavaRepositoryNames(MAX_PROJECTS);
		for (final Repository rep : reps) {
			final JsonObject projectJSON = createJson(rep);
			if (((JsonArray) projectJSON.get(COMMITS)).size() > 0) {
				resultArray.add(projectJSON);
			}
		}
		try (FileWriter fileWriter = new FileWriter(PROJECTS_DATA_OUTPUT_FILE)) {
			fileWriter.write(gson.toJson(resultJSON));
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	private JsonObject createJson(final Repository rep) {
		final JsonObject projectJSON = new JsonObject();
		final String URL = "http://www.github.com/" + rep.getVendor() + "/" + rep.getProduct() + ".git";
		projectJSON.addProperty(PRODUCT_NAME, rep.getProduct());
		projectJSON.addProperty(VENDOR_NAME, rep.getVendor());
		projectJSON.addProperty(URL, URL);
		final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JsonArray commits;
		try {
			commits = getReleaseCommits(httpClient, rep.getVendor(), rep.getProduct());
			projectJSON.add(COMMITS, commits);
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			projectJSON.add(COMMITS, new JsonArray());
		}
		return projectJSON;
	}

	// @Test
	public void cleanUpProjectVersions() {
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Reading the unnormalized file
		try (JsonReader reader = new JsonReader(new FileReader(PROJECTS_DATA_OUTPUT_FILE))) {
			final JsonObject jsonTree = gson.fromJson(reader, JsonObject.class);
			// Getting all the projects
			final JsonArray projects = jsonTree.get(PROJECTS).getAsJsonArray();

			// Setting up the normalized file
			final JsonObject resultJSON = new JsonObject();
			final JsonArray resultArray = new JsonArray();
			resultJSON.add(PROJECTS, resultArray);

			// Iterate the unnormalized file
			for (int i = 0; i < projects.size(); i++) {
				// Get a project
				final JsonObject jo = (JsonObject) projects.get(i);

				// Create a project object for the new file
				final JsonObject projectJSON = new JsonObject();

				// Set the new properties
				projectJSON.addProperty(PRODUCT_NAME, jo.get(PRODUCT_NAME).getAsString());
				projectJSON.addProperty(VENDOR_NAME, jo.get(VENDOR_NAME).getAsString());
				projectJSON.addProperty(URL, jo.get(URL).getAsString());

				// New commit data
				final JsonArray newCommits = new JsonArray();
				projectJSON.add(COMMITS, newCommits);

				final JsonArray commits = jo.get(COMMITS).getAsJsonArray();

				for (int j = 0; j < commits.size(); j++) {
					final JsonObject commitAndVersion = (JsonObject) commits.get(j);

					// Get version
					String version = commitAndVersion.get(VERSION).getAsString();

					// Normalize the version
					version = version.toLowerCase();

					if (version.matches("(\\.|-|_)(snapshot|pre|alpha|beta|rc|m|prototype)(?![a-z])(\\.|-|_)?[0-9]*")) {
						break;
					}

					else {
						// Add it to the new json
						final Pattern p = Pattern.compile("[0-9]+((\\.|_|-)[0-9]+)+");
						final Matcher m = p.matcher(version);
						while (m.find()) {
							version = m.group();
							version = version.replaceAll("(_|-)", ".");
							final JsonObject newCommit = new JsonObject();
							newCommit.addProperty(COMMIT_ID, commitAndVersion.get(COMMIT_ID).getAsString());
							newCommit.addProperty(VERSION, version);

							newCommits.add(newCommit);
						}

					}
				}

				if (newCommits.size() == 0) {
					continue;
				} else {
					resultArray.add(projectJSON);
				}

				write(gson, resultJSON);

			}
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, "Could not read old projects output file", e);
		}
	}

	private void write(final Gson gson, final JsonObject resultJSON) {
		// Write the new file
		FileUtils.createDirectory("Resources");
		try (FileWriter fileWriter = new FileWriter(PROJECTS_DATA_OUTPUT_FILE_NORMALIZED)) {
			fileWriter.write(gson.toJson(resultJSON));
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, "Couldn't write normalized file", e);
		}
	}
}
