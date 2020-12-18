package metric.correlation.analysis.selection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import metric.correlation.analysis.vulnerabilities.VulnerabilityDataQueryHandler;

/**
 * @author Antoniya Ivanova Searches via the GitHub API for usable projects for
 *         the metric correlation analysis tool.
 *
 */

public class GitHubProjectSelector {

	private static final String LOCALHOST = "localhost";

	private static final String STARS = "Stars";

	private static final String PRODUCT = "Product";

	private static final String VENDOR = "Vendor";

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(GitHubProjectSelector.class);

	private static final int RESULTS_PER_PAGE = 100;
	private static final int NUMBER_OF_PAGES = 10;
	private static final int MAX_SIZE = 150000;
	private static final int MIN_OPEN_ISSUES = 20;
	public static final int GIT_REQUESTS_PER_MINUTE = 50;

	public static int gitRequests = 1;

	/**
	 * Selectors for checking if the project has an supported build nature
	 */
	private static final IGithubProjectSelector[] BUILD_NATURE_SELECTORS = new IGithubProjectSelector[] {
			new GradleGithubProjectSelector(), new MavenGithubProjectSelector() };

	private RestHighLevelClient elasticClient;
	// Change this to your own OAuthToken
	public static final String OAuthToken = System.getenv("GITHUB_OAUTH");
	protected static String repositoryDatabaseName = "repositories_database_extended";

	public void initializeProjectElasticDatabase() {
		addDocumentsToElastic(searchForJavaRepositoryNames(100));
	}

	/**
	 * Searches for Java + Gradle repositories on GitHub.
	 *
	 * @return a HashSet of {@link Repository} results, which are Java and Gradle
	 *         projects.
	 */
	public Set<Repository> searchForJavaRepositoryNames(final int maxProjects) {
		final HashSet<Repository> respositoryResults = new HashSet<>();
		String url;
		int matchedProjectCount = 0;
		int sizeError = 0;
		int issueError = 0;
		int totalCnt = 0;
		int acceptError = 0;
		try {
			final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

			// Requests per page x 100
			for (int i = 1; i <= NUMBER_OF_PAGES; i++) {
				if ((gitRequests++ % GIT_REQUESTS_PER_MINUTE) == 0) {
					TimeUnit.MINUTES.sleep(1);
				}
				url = "https://api.github.com/search/repositories?q=language:java&sort=stars&order=desc"
						+ "&page=" + i + "&per_page=" + RESULTS_PER_PAGE;

				final HttpGet request = new HttpGet(url);
				request.addHeader("content-type", "application/json");
				request.addHeader("Authorization", "Token " + OAuthToken);
				final HttpResponse result = httpClient.execute(request);

				final String json = EntityUtils.toString(result.getEntity(), "UTF-8");

				final JsonElement jelement = new JsonParser().parse(json);
				final JsonObject jobject = jelement.getAsJsonObject();
				final JsonArray jarray = jobject.getAsJsonArray("items");

				for (int j = 0; (j < jarray.size()) && (matchedProjectCount < maxProjects); j++) {
					totalCnt++;
					final JsonObject jo = (JsonObject) jarray.get(j);
					final String fullName = jo.get("full_name").toString().replace("\"", "");
					final int stars = Integer.parseInt(jo.get("stargazers_count").toString());
					final int openIssues = Integer.parseInt(jo.get("open_issues").toString());
					final int size = Integer.parseInt(jo.get("size").toString());
					if ((size < MAX_SIZE) || (size > (2*MAX_SIZE))) {
						sizeError++;
						continue;
					}
					if (openIssues < MIN_OPEN_ISSUES) {
						issueError++;
						continue;
					}
					boolean accept = false;
					for (final IGithubProjectSelector b : BUILD_NATURE_SELECTORS) {
						accept |= b.accept(fullName, OAuthToken);
					}
					if (accept) {
						matchedProjectCount++;
						LOGGER.log(Level.INFO, "MATCH : " + fullName);
						final String product = jo.get("name").toString().replace("\"", "");

						final JsonObject owner = (JsonObject) jo.get("owner");
						final String vendor = owner.get("login").toString().replace("\"", "");

						respositoryResults.add(new Repository(vendor, product, stars, openIssues));
					}
					else {
						acceptError++;
						LOGGER.error("NO MATCH " + fullName);
					}

					LOGGER.log(Level.INFO, j);

				}
				if ((matchedProjectCount >= maxProjects) || (jarray.size() == 0)) {
					break;
				}

				// addDocumentsToElastic(respositoryResults);
				// respositoryResults.clear();
			}

			httpClient.close();

		} catch (final Exception e) {
			LOGGER.log(Level.INFO, e.getStackTrace());
		}
		LOGGER.error("Total Count : " + totalCnt);
		LOGGER.error("Disregarded for size: " + sizeError);
		LOGGER.error("Disregarded for issues: " + issueError);
		LOGGER.error("Disregarded for lack of mvn/ gradle: " + acceptError);
		LOGGER.error("Matched projects: " + matchedProjectCount);
		return respositoryResults;
	}

	/**
	 * Adds the found repositories to an Elasticsearch DB.
	 *
	 * @param repositoriesSet repositories to be added to the Elasticsearch database
	 *                        (index).
	 */
	private void addDocumentsToElastic(final Set<Repository> repositoriesSet) {
		final ArrayList<HashMap<String, Object>> repositories = new ArrayList<>();

		// Build the repository document
		for (final Repository repositoryResult : repositoriesSet) {
			final HashMap<String, Object> repository = new HashMap<>();
			repository.put(VENDOR, repositoryResult.getVendor());
			repository.put(PRODUCT, repositoryResult.getProduct());
			repository.put(STARS, repositoryResult.getStars());
			repositories.add(repository);
		}

		this.elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost(LOCALHOST, 9200, "http")));

		IndexRequest indexRequest = null;

		for (final Iterator<HashMap<String, Object>> iterator = (repositories).iterator(); iterator.hasNext();) {
			indexRequest = new IndexRequest(repositoryDatabaseName, "doc").source(iterator.next());

			try {
				this.elasticClient.index(indexRequest);
			} catch (final Exception e) {
				LOGGER.log(Level.ERROR, "Could not index document " + iterator.toString());
				LOGGER.log(Level.ERROR, e.getMessage(), e);
			}
		}

		LOGGER.log(Level.INFO, "Inserting " + repositories.size() + " documents into index.");

		try {
			this.elasticClient.close();
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Gets the projects with at least one vulnerability in the local Elasticsearch
	 * database.
	 *
	 * @return repositoriesWithVulnerabilities as HashSet of SearchHits
	 */
	public HashSet<SearchHit> getProjectsWithAtLeastOneVulnerability() {
		final HashSet<SearchHit> results = new HashSet<>();

		float numberOfRepositoriesWithVulnerabilities = 0;
		float percentageOfRepositoriesWithVulnerabilities = 0;
		float totalNumberOfProjects = 0;

		this.elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost(LOCALHOST, 9200, "http")));
		final VulnerabilityDataQueryHandler vulnerabilityDataQueryHandler = new VulnerabilityDataQueryHandler();

		final SearchRequest searchRequest = new SearchRequest(repositoryDatabaseName);

		final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.size(1000);
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());

		searchRequest.source(searchSourceBuilder);

		try {
			final SearchResponse searchResponse = this.elasticClient.search(searchRequest);
			totalNumberOfProjects = searchResponse.getHits().getTotalHits();

			final SearchHits repositoryHits = searchResponse.getHits();
			final SearchHit[] repositorySearchHits = repositoryHits.getHits();

			for (final SearchHit repository : repositorySearchHits) {

				final Map<String, Object> map = repository.getSourceAsMap();

				final String product = map.get(PRODUCT).toString();
				final String vendor = map.get(VENDOR).toString();

				final HashSet<SearchHit> vulnerabilities = vulnerabilityDataQueryHandler.getVulnerabilities(product, vendor,
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

			this.elasticClient.close();
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return results;
	}

	/**
	 * Gets the average number of stars in the Elasticsearch repository database.
	 */
	public void getAverageNumberOfStars() {
		this.elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost(LOCALHOST, 9200, "http")));

		final SearchRequest searchRequest = new SearchRequest(repositoryDatabaseName);
		final AvgAggregationBuilder avgAB = AggregationBuilders.avg("avg_stars").field(STARS);

		final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchAllQuery()).aggregation(avgAB);
		searchRequest.source(searchSourceBuilder);

		try {
			final SearchResponse searchResponse = this.elasticClient.search(searchRequest);
			final Aggregations aggregations = searchResponse.getAggregations();

			final Avg avgStars = aggregations.get("avg_stars");

			final double avg = avgStars.getValue();

			LOGGER.log(Level.INFO, avg);
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, "Could not get average number of stars.");
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
		try {
			this.elasticClient.close();
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, "Could not close RestHighLevelClient!");
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Gets the average number of discovered vulnerabilities for the repositories in
	 * the Elasticsearch repository database.
	 */
	public double getAverageNumberOfDiscoveredVulnerabilities() {
		this.elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost(LOCALHOST, 9200, "http")));
		final VulnerabilityDataQueryHandler vulnerabilityDataQueryHandler = new VulnerabilityDataQueryHandler();
		long totalNumberOfProjects = 0;
		long totalNumberOfVulnerabilites = 0;
		double averageVulnerabilitiesPerProject = 0;

		final SearchRequest searchRequest = new SearchRequest(repositoryDatabaseName);
		final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.size(1000);
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchRequest.source(searchSourceBuilder);

		try {
			final SearchResponse searchResponse = this.elasticClient.search(searchRequest);
			totalNumberOfProjects = searchResponse.getHits().getTotalHits();

			final SearchHits repositoryHits = searchResponse.getHits();
			final SearchHit[] searchHits = repositoryHits.getHits();

			for (final SearchHit searchHit : searchHits) {
				final Map<String, Object> map = searchHit.getSourceAsMap();
				final String product = map.get(PRODUCT).toString();
				final String vendor = map.get(VENDOR).toString();
				final HashSet<SearchHit> vulnerabilities = vulnerabilityDataQueryHandler.getVulnerabilities(product, vendor,
						"", "TWO");
				totalNumberOfVulnerabilites += vulnerabilities.size();
			}

			averageVulnerabilitiesPerProject = totalNumberOfVulnerabilites / (double) totalNumberOfProjects;

			LOGGER.log(Level.INFO,
					"The average number of discovered vulnerabilities is : " + averageVulnerabilitiesPerProject);
			LOGGER.log(Level.INFO,
					"The total number of discovered vulnerabilities is : " + totalNumberOfVulnerabilites);

			this.elasticClient.close();
		} catch (final Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return averageVulnerabilitiesPerProject;
	}

}
