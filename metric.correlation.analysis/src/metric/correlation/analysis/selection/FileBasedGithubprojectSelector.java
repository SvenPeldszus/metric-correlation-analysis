package metric.correlation.analysis.selection;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public abstract class FileBasedGithubprojectSelector implements IGithubProjectSelector {

	private static final Logger LOGGER = Logger.getLogger(FileBasedGithubprojectSelector.class);

	private final String fileName;

	public FileBasedGithubprojectSelector(final String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Tests if a repository has the specified path in its root directory
	 *
	 * @param repositoryName the name of the repository to be tested
	 * @param oAuthToken
	 * @return true if it contains the file in its root directory
	 */
	@Override
	public boolean accept(final String repositoryName, final String oAuthToken) {
		String searchUrl;
		try {
			final var httpClient = HttpClientBuilder.create().build();

			searchUrl = "https://github.com/" + repositoryName + "/blob/master/" + this.fileName;
			final var request = new HttpGet(searchUrl);
			request.addHeader("content-type", "application/json");
			request.addHeader("Authorization", "Token " + GitHubProjectSelector.OAuthToken);

			HttpResponse result = httpClient.execute(request);
			while (GitHubProjectSelector.rateLimit(result)) {
				result = httpClient.execute(request);
			}

			if (result.getStatusLine().getStatusCode() != 404) {
				return true;
			}
			httpClient.close();
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, "Could not check if repository is a Gradle repository.");
			LOGGER.log(Level.INFO, e.getStackTrace());
		}

		return false;
	}

}
