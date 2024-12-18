package metric.correlation.analysis.selection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import metric.correlation.analysis.github.GitHubCrawler;

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
		try (var httpClient = HttpClient.newHttpClient()){
			
			searchUrl = "https://github.com/" + repositoryName + "/blob/master/" + this.fileName;
			
			final var request = HttpRequest.newBuilder().uri(URI.create(searchUrl))
					.header("content-type", "application/json").header("Authorization", "Token " + GitHubCrawler.OAuthToken)
					.build();

			var result = httpClient.send(request, BodyHandlers.ofString());
			while (GitHubProjectSelector.rateLimit(result)) {
				result = httpClient.send(request, BodyHandlers.ofString());
			}

			if (result.statusCode() != 404) {
				return true;
			}
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, "Could not check if repository is a Gradle repository.");
			LOGGER.log(Level.INFO, e.getStackTrace());
		}

		return false;
	}

}
