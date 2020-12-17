package metric.correlation.analysis.project_selection;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class FileBasedGithubprojectSelector implements IGithubProjectSelector {

	private static final Logger LOGGER = Logger.getLogger(FileBasedGithubprojectSelector.class);

	private final String fileName;
	private static final int MAX_PAGES = 100;

	public FileBasedGithubprojectSelector(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Tests if a repository has the specified path in its root directory
	 * 
	 * @param repositoryName the name of the repository to be tested
	 * @param OAuthToken
	 * @return true if it contains the file in its root directory
	 */
	@Override
	public boolean accept(String repositoryName, String OAuthToken) {
		String searchUrl;
		try {
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			for (int i = 1; i < MAX_PAGES; i++) {
				searchUrl = "https://api.github.com/search/code?q=repo:" + repositoryName + "+filename:" + fileName
						+ "&access_token=" + OAuthToken + "&per_page=100&page=" + i;

				HttpGet requestGradleRepository = new HttpGet(searchUrl);
				requestGradleRepository.addHeader("content-type", "application/json");

				HttpResponse resultResponse = httpClient.execute(requestGradleRepository);
				String json = EntityUtils.toString(resultResponse.getEntity(), "UTF-8");

				JsonElement jelement = new JsonParser().parse(json);
				JsonObject jobject = jelement.getAsJsonObject();
				JsonArray jarray = jobject.getAsJsonArray("items");
				if (jarray.size() == 0) {
					return false;
				}
				for (JsonElement elem : jarray) {
					String path = ((JsonObject) elem).get("path").toString().replace("\"","");
					if (path.equals(fileName)) {
						httpClient.close();
						return true;
					}
				}
			}
			httpClient.close();
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Could not check if repository is a Gradle repository.");
			LOGGER.log(Level.INFO, e.getStackTrace());
		}

		return false;
	}

}
