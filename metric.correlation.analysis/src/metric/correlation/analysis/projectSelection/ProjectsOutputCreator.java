package metric.correlation.analysis.projectSelection;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class ProjectsOutputCreator {

	private static final Logger LOGGER = Logger.getLogger(ProjectsOutputCreator.class);

	/**
	 * The location of the JSON file containing the project information
	 */
	public static final String PROJECTS_DATA_OUTPUT_FILE = "input/projectsReleaseData2.json";
	public static final String PROJECTS_DATA_OUTPUT_FILE_NORMALIZED = "input/projectsReleaseData-normalized.json";
	private static final String PROJECTS = "projects";
	
	private static final int MAX_COMMITS = 1;
	private static final int MAX_PROJECTS = 20;

	/**
	 * @author Antoniya Ivanova - prepares the JSON output for the repository
	 *         search, includes the releases for each repository and what
	 *         version/commit they relate to.
	 *
	 */
	public void getProjectReleases() {

		HashSet<SearchHit> repositoriesWithCVEs = new ProjectSelector().getProjectsWithAtLeastOneVulnerability();

		JsonObject resultJSON = new JsonObject();
		JsonArray resultArray = new JsonArray();
		resultJSON.add(PROJECTS, resultArray);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

			// Iterate the vulnerable projects
			for (SearchHit repository : repositoriesWithCVEs) {
				JsonObject projectJSON = new JsonObject();

				Map<String, Object> map = repository.getSourceAsMap();

				String productName = map.get("Product").toString();
				String vendorName = map.get("Vendor").toString();
				String URL = "http://www.github.com/" + vendorName + "/" + productName + ".git";

				projectJSON.addProperty("productName", productName);
				projectJSON.addProperty("vendorName", vendorName);
				projectJSON.addProperty("url", URL);

				JsonArray commits = getReleaseCommits(httpClient, vendorName, productName);
				projectJSON.add("commits", commits);

				if (commits.size() == 0) {
					continue;
				} else {
					resultArray.add(projectJSON);
				}

			}

			FileUtils.createDirectory("Resources");

			try (FileWriter fileWriter = new FileWriter(PROJECTS_DATA_OUTPUT_FILE)) {
				fileWriter.write(resultJSON.toString());
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

	}

	public JsonArray getReleaseCommits(CloseableHttpClient httpClient, String vendorName, String productName) throws Exception{
		return getReleaseCommits(httpClient, vendorName, productName, MAX_COMMITS );
	}
	public JsonArray getReleaseCommits(CloseableHttpClient httpClient, String vendorName, String productName, Integer commitLimit)
			throws Exception {
		JsonArray commits = new JsonArray();
		// Iterate over the project release pages
		for (int i = 1; i < 100; i++) {
			if (ProjectSelector.GIT_REQUESTS++ % 20 == 0) { 
				TimeUnit.MINUTES.sleep(1);
			}
			// Respect 30 request limit of GitHub API

			String gitURL = "https://api.github.com/repos/" + vendorName + "/" + productName + "/tags?page=" + i
					+ "&per_page=100";

			HttpGet request = new HttpGet(gitURL);
			request.addHeader("Authorization", "Token " + ProjectSelector.oAuthToken);
			request.addHeader("content-type", "application/json");

			HttpResponse result = httpClient.execute(request);
			String json = EntityUtils.toString(result.getEntity(), "UTF-8");
			JsonArray jarray = new JsonParser().parse(json).getAsJsonArray();

			if (jarray.size() != 0) {

				// Iterate over the project releases on the page
				for (int j = 0; j < jarray.size(); j++) {

					JsonObject commit = new JsonObject();
					JsonObject jo = (JsonObject) jarray.get(j);

					commit.addProperty("commitId",
							jo.get("commit").getAsJsonObject().get("sha").toString().replace("\"", ""));
					String version = jo.get("name").toString().replace("\"", "");
					commit.addProperty("version", version);
					if (!version.toLowerCase()
							.matches(".*(\\.|-|_|^)(snapshot|doc|pre|alpha|beta|rc|m|prototype)(?![a-z]).*")) {
						commits.add(commit);
						if (commits.size() >= commitLimit) {
							return commits;
						}
					}
				}
			} else {
				break;
			}
		}
		return commits;
	}

	@Test
	public void testFindProjects() {
		LOGGER.getRootLogger().setLevel(Level.ALL);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonObject resultJSON = new JsonObject();
		JsonArray resultArray = new JsonArray();
		resultJSON.add("projects", resultArray);
		Set<Repository> reps = new ProjectSelector().searchForJavaRepositoryNames(MAX_PROJECTS);
		for (Repository rep : reps) {
			JsonObject projectJSON = createJson(rep);
			if (((JsonArray) projectJSON.get("commits")).size() > 0) {
				resultArray.add(projectJSON);
			}
		}
		try (FileWriter fileWriter = new FileWriter(PROJECTS_DATA_OUTPUT_FILE)) {
			fileWriter.write(gson.toJson(resultJSON));
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	private JsonObject createJson(Repository rep) {
		JsonObject projectJSON = new JsonObject();
		String URL = "http://www.github.com/" + rep.getVendor() + "/" + rep.getProduct() + ".git";
		projectJSON.addProperty("productName", rep.getProduct());
		projectJSON.addProperty("vendorName", rep.getVendor());
		projectJSON.addProperty("url", URL);
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		JsonArray commits;
		try {
			commits = getReleaseCommits(httpClient, rep.getVendor(), rep.getProduct());
			projectJSON.add("commits", commits);
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			projectJSON.add("commits", new JsonArray());
		}
		return projectJSON;
	}

	// @Test
	public void cleanUpProjectVersions() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Reading the unnormalized file
		try (JsonReader reader = new JsonReader(new FileReader(PROJECTS_DATA_OUTPUT_FILE))) {
			JsonObject jsonTree = gson.fromJson(reader, JsonObject.class);
			// Getting all the projects
			JsonArray projects = jsonTree.get("projects").getAsJsonArray();

			// Setting up the normalized file
			JsonObject resultJSON = new JsonObject();
			JsonArray resultArray = new JsonArray();
			resultJSON.add("projects", resultArray);

			// Iterate the unnormalized file
			for (int i = 0; i < projects.size(); i++) {
				// Get a project
				JsonObject jo = (JsonObject) projects.get(i);

				// Create a project object for the new file
				JsonObject projectJSON = new JsonObject();

				// Set the new properties
				projectJSON.addProperty("productName", jo.get("productName").getAsString());
				projectJSON.addProperty("vendorName", jo.get("vendorName").getAsString());
				projectJSON.addProperty("url", jo.get("url").getAsString());

				// New commit data
				JsonArray newCommits = new JsonArray();
				projectJSON.add("commits", newCommits);

				JsonArray commits = jo.get("commits").getAsJsonArray();

				for (int j = 0; j < commits.size(); j++) {
					JsonObject commitAndVersion = (JsonObject) commits.get(j);

					// Get version
					String version = commitAndVersion.get("version").getAsString();

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
							JsonObject newCommit = new JsonObject();
							newCommit.addProperty("commitId", commitAndVersion.get("commitId").getAsString());
							newCommit.addProperty("version", version);

							newCommits.add(newCommit);
						}

					}
				}

				if (newCommits.size() == 0) {
					continue;
				} else {
					resultArray.add(projectJSON);
				}

				// Write the new file
				FileUtils.createDirectory("Resources");
				try (FileWriter fileWriter = new FileWriter(PROJECTS_DATA_OUTPUT_FILE_NORMALIZED)) {
					fileWriter.write(gson.toJson(resultJSON));
				} catch (Exception e) {
					LOGGER.log(Level.ERROR, "Couldn't write normalized file", e);
				}

			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "Could not read old projects output file", e);
		}
	}
}
