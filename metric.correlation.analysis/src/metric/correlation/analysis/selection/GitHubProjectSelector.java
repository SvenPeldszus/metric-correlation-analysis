package metric.correlation.analysis.selection;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import metric.correlation.analysis.database.ElasticSearchHelper;
import metric.correlation.analysis.vulnerabilities.VulnerabilityDataQueryHandler;

/**
 * @author Antoniya Ivanova Searches via the GitHub API for usable projects for
 *         the metric correlation analysis tool.
 *
 */

public class GitHubProjectSelector implements Closeable {

	private static final String LOCALHOST = "localhost";

	private static final String STARS = "Stars";
	private static final String PRODUCT = "Product";
	private static final String VENDOR = "Vendor";
	private static final String URL = "URL";
	private static final String OPEN_ISSUES = "OpenIssues";
	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(GitHubProjectSelector.class);

	private static final int RESULTS_PER_PAGE = 100;
	public static final int GIT_REQUESTS_PER_HOUR = 5000;

	private static final int MIN_SIZE = 1000;
	private static final int MIN_STARS = 10;
	private static final int MIN_OPEN_ISSUES = 0;

	/**
	 * Selectors for checking if the project has an supported build nature
	 */
	private static final IGithubProjectSelector[] BUILD_NATURE_SELECTORS = {
			new GradleGithubProjectSelector(), new MavenGithubProjectSelector() };

	private final RestHighLevelClient elasticClient;

	// Change this to your own OAuthToken
	public static final String OAuthToken = System.getenv("GITHUB_OAUTH");

	public static GitHubProjectSelector INSTANCE;

	public final static String repositoryDatabaseName = "repositories_database_extended";

	public static void main(final String[] args) throws IOException {
		final var consoleAppender = new ConsoleAppender();
		final var PATTERN = "%d - %m%n";
		consoleAppender.setLayout(new PatternLayout(PATTERN));
		consoleAppender.setThreshold(Level.INFO);
		consoleAppender.activateOptions();
		Logger.getRootLogger().addAppender(consoleAppender);

		try (var initializer = new GitHubProjectSelector()) {
			initializer.initializeProjectElasticDatabase(-1);
		}
	}

	public GitHubProjectSelector() {
		INSTANCE = this;
		this.elasticClient = ElasticSearchHelper.getElasticSearchClient();
	}

	/**
	 * Searches for Java + Gradle repositories on GitHub.
	 *
	 * @return a HashSet of {@link Repository} results, which are Java and Gradle
	 *         projects.
	 */
	public void initializeProjectElasticDatabase(final int maxProjects) {
		var matchedProjectCount = 0;
		var totalCnt = 0;

		var acceptError = 0;
		var issueError = 0;

		final var today = Calendar.getInstance();
		var year = 2008;
		final var status = new File(".year");
		if (status.exists()) {
			try {
				year = Integer.parseInt(Files.readString(status.toPath()));
			} catch (NumberFormatException | IOException e) {
				LOGGER.error(e);
			}
		}
		var month = 01;

		try (final var httpClient = HttpClientBuilder.create().build()) {
			// Requests per page x 100
			var i = 1;
			var respositoryResults = new HashSet<Repository>();
			while (maxProjects == -1 || matchedProjectCount < maxProjects) {
				final var jobject = this.getPage(httpClient, i, year, month);
				final var jarray = jobject.getAsJsonArray("items");
				if (jarray == null || jarray.size() == 0) {
					// no further results
					if (year == today.get(Calendar.YEAR) && month == 1 + today.get(Calendar.MONTH)) {
						this.addDocumentsToElastic(respositoryResults);
						break;
					}
					if (month == 12) {
						year++;
						this.addDocumentsToElastic(respositoryResults);
						respositoryResults = new HashSet<>();
						Files.write(status.toPath(), Integer.toString(year).getBytes());
						month = 01;
					} else {
						month++;
					}
					i = 1;
//					try {
//						Thread.sleep(1500);
//					} catch (final InterruptedException e) {
//						LOGGER.error(e);
//						Thread.currentThread().interrupt();
//					}
					continue;
				}

				for (var j = 0; (j < jarray.size()) && (maxProjects == -1 || matchedProjectCount < maxProjects); j++) {
					totalCnt++;
					final var jo = (JsonObject) jarray.get(j);
					final var fullName = jo.get("full_name").toString().replace("\"", "");

					final var stars = Integer.parseInt(jo.get("stargazers_count").toString());

					final var openIssues = Integer.parseInt(jo.get("open_issues").toString());
					if (openIssues < MIN_OPEN_ISSUES) {
						issueError++;
						continue;
					}
					var accept = false;
					for (final IGithubProjectSelector b : BUILD_NATURE_SELECTORS) {
						accept |= b.accept(fullName, OAuthToken);
					}
					if (accept) {
						matchedProjectCount++;
						LOGGER.log(Level.INFO,
								totalCnt + " MATCH " + fullName + " (" + matchedProjectCount + " projects)"
										+ " -- repo " + (j + 1) + " page " + i);
						final var url = jo.get("html_url").toString().replace("\"", "");
						final var product = jo.get("name").toString().replace("\"", "");

						final var owner = (JsonObject) jo.get("owner");
						final var vendor = owner.get("login").toString().replace("\"", "");

						respositoryResults.add(new Repository(url, vendor, product, stars, openIssues));
					} else {
						acceptError++;
						LOGGER.error(totalCnt + " NO MATCH " + fullName + " -- repo " + (j + 1) + " page " + i);
					}
				}
				if ((maxProjects > 0 && matchedProjectCount >= maxProjects) || (jarray.size() == 0)) {
					this.addDocumentsToElastic(respositoryResults);
					break;
				}
				i++;
			}
		} catch (final IOException e) {
			LOGGER.error(e);
		}
		LOGGER.info("Total Count : " + totalCnt);
		LOGGER.info("Disregarded for issues: " + issueError);
		LOGGER.info("Disregarded for lack of mvn/ gradle: " + acceptError);
		LOGGER.info("Matched projects: " + matchedProjectCount);
	}

	private JsonObject getPage(final CloseableHttpClient httpClient, final int page, final int year, final int month)
			throws IOException, ClientProtocolException {
		final var url = "https://api.github.com/search/repositories?q=language%3Ajava"
				+ "+created%3A" + year + "-" + String.format("%02d", month)
				+ "+size%3A%3E" + MIN_SIZE
				+ "+stars%3A%3E" + MIN_STARS
				+ "&page=" + page
				+ "&per_page=" + RESULTS_PER_PAGE;
		System.out.println("GET " + url);
		final var request = new HttpGet(url);
		request.addHeader("content-type", "application/json");
		request.addHeader("Authorization", "Token " + OAuthToken);

		try {
			Thread.sleep(50);
		} catch (final InterruptedException e) {
			LOGGER.error(e);
			Thread.currentThread().interrupt();
		}
		HttpResponse result = httpClient.execute(request);
		while (GitHubProjectSelector.rateLimit(result)) {
			System.out.println("retry");
			result = httpClient.execute(request);
		}

		final var string = EntityUtils.toString(result.getEntity(), "UTF-8");
		return new JsonParser().parse(string).getAsJsonObject();
	}

	public static boolean rateLimit(final HttpResponse result) {
		if (result.getStatusLine().getStatusCode() == 403) {
			final var xRatelimitReset = Instant
					.ofEpochSecond(Long.parseLong(result.getFirstHeader("x-ratelimit-reset").getValue()))
					.toEpochMilli();
			final var now = Instant.now().toEpochMilli();
			var sleep = xRatelimitReset - now + 1000;
			if (sleep <= 0) {
				sleep = 1000;
			}

			LOGGER.info("Rate limit: Sleep for " + (sleep / 1000) + "s until " + new Date(xRatelimitReset)
					+ " + 1s");
			try {
				Thread.sleep(sleep);
			} catch (final InterruptedException e) {
				LOGGER.error(e);
				Thread.currentThread().interrupt();
			}
			return true;
		}
		return false;
	}

	/**
	 * Adds the found repositories to an Elasticsearch DB.
	 *
	 * @param repositoriesSet repositories to be added to the Elasticsearch database
	 *                        (index).
	 */
	private void addDocumentsToElastic(final Set<Repository> repositoriesSet) {
		final var repositories = new ArrayList<HashMap<String, Object>>();

		// Build the repository document
		for (final Repository repositoryResult : repositoriesSet) {
			final var repository = new HashMap<String, Object>();
			repository.put(VENDOR, repositoryResult.getVendor());
			repository.put(PRODUCT, repositoryResult.getProduct());
			repository.put(STARS, repositoryResult.getStars());
			repository.put(URL, repositoryResult.getUrl());
			repository.put(OPEN_ISSUES, repositoryResult.getOpenIssues());
			repositories.add(repository);
		}

		IndexRequest indexRequest = null;

		for (final var iterator = (repositories).iterator(); iterator.hasNext();) {
			indexRequest = new IndexRequest(repositoryDatabaseName, "_doc").source(iterator.next());

			try {
				this.elasticClient.index(indexRequest, RequestOptions.DEFAULT);
			} catch (final Exception e) {
				LOGGER.log(Level.ERROR, "Could not index document " + iterator.toString());
				LOGGER.log(Level.ERROR, e.getMessage(), e);
			}
		}

		LOGGER.log(Level.INFO, "Inserting " + repositories.size() + " documents into index.");
	}

	/**
	 * Gets the projects with at least one vulnerability in the local Elasticsearch
	 * database.
	 *
	 * @return repositoriesWithVulnerabilities as HashSet of SearchHits
	 */
	public Set<SearchHit> getProjectsWithAtLeastOneVulnerability() {
		final var results = new HashSet<SearchHit>();

		var numberOfRepositoriesWithVulnerabilities = 0F;
		var percentageOfRepositoriesWithVulnerabilities = 0F;
		var totalNumberOfProjects = 0F;

		final var searchRequest = new SearchRequest(repositoryDatabaseName);

		final var searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.size(1000);
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());

		searchRequest.source(searchSourceBuilder);

		try {
			final var searchResponse = this.elasticClient.search(searchRequest, RequestOptions.DEFAULT);
			totalNumberOfProjects = searchResponse.getHits().getHits().length;

			final var repositoryHits = searchResponse.getHits();
			final var repositorySearchHits = repositoryHits.getHits();

			for (final SearchHit repository : repositorySearchHits) {

				final var map = repository.getSourceAsMap();

				final var product = map.get(PRODUCT).toString();
				final var vendor = map.get(VENDOR).toString();

				final var vulnerabilities = VulnerabilityDataQueryHandler.getVulnerabilities(product, vendor,
						"", "TWO");

				if (!vulnerabilities.isEmpty()) {
					numberOfRepositoriesWithVulnerabilities++;
					results.add(repository);
				}
			}

			percentageOfRepositoriesWithVulnerabilities = (numberOfRepositoriesWithVulnerabilities
					/ totalNumberOfProjects) * 100;

			LOGGER.log(Level.INFO, "The percentage of repositories with a vulnerability is : "
					+ percentageOfRepositoriesWithVulnerabilities + "%");
			LOGGER.log(Level.INFO,
					"Repositories with at least one vulnerability : " + numberOfRepositoriesWithVulnerabilities);

		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return results;
	}

	/**
	 * Gets the average number of stars in the Elasticsearch repository database.
	 */
	public void getAverageNumberOfStars() {
		final var searchRequest = new SearchRequest(repositoryDatabaseName);
		final var avgAB = AggregationBuilders.avg("avg_stars").field(STARS);

		final var searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchAllQuery()).aggregation(avgAB);
		searchRequest.source(searchSourceBuilder);

		try {
			final var searchResponse = this.elasticClient.search(searchRequest, RequestOptions.DEFAULT);
			final var aggregations = searchResponse.getAggregations();

			final var avgStars = aggregations.get("avg_stars");

			final var avg = ((Avg) avgStars).getValue();

			LOGGER.log(Level.INFO, avg);
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, "Could not get average number of stars.");
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Gets the average number of discovered vulnerabilities for the repositories in
	 * the Elasticsearch repository database.
	 */
	public double getAverageNumberOfDiscoveredVulnerabilities() {
		final var vulnerabilityDataQueryHandler = new VulnerabilityDataQueryHandler();
		var totalNumberOfProjects = 0L;
		var totalNumberOfVulnerabilites = 0L;
		var averageVulnerabilitiesPerProject = 0D;

		final var searchRequest = new SearchRequest(repositoryDatabaseName);
		final var searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.size(1000);
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchRequest.source(searchSourceBuilder);

		try {
			final var searchResponse = this.elasticClient.search(searchRequest, RequestOptions.DEFAULT);
			totalNumberOfProjects = searchResponse.getHits().getHits().length;

			final var repositoryHits = searchResponse.getHits();
			final var searchHits = repositoryHits.getHits();

			for (final SearchHit searchHit : searchHits) {
				final var map = searchHit.getSourceAsMap();
				final var product = map.get(PRODUCT).toString();
				final var vendor = map.get(VENDOR).toString();
				final var vulnerabilities = VulnerabilityDataQueryHandler.getVulnerabilities(product, vendor,
						"", "TWO");
				totalNumberOfVulnerabilites += vulnerabilities.size();
			}

			averageVulnerabilitiesPerProject = totalNumberOfVulnerabilites / (double) totalNumberOfProjects;

			LOGGER.log(Level.INFO,
					"The average number of discovered vulnerabilities is : " + averageVulnerabilitiesPerProject);
			LOGGER.log(Level.INFO,
					"The total number of discovered vulnerabilities is : " + totalNumberOfVulnerabilites);
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return averageVulnerabilitiesPerProject;
	}

	@Override
	public void close() throws IOException {
		this.elasticClient.close();
	}

	public Set<Repository> searchForJavaRepositoryNames(final int maxProjects) {
		// TODO Auto-generated method stub
		return null;
	}

}
