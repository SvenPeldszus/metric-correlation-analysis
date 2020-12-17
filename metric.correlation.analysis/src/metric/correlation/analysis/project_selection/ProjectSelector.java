package metric.correlation.analysis.project_selection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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

public class ProjectSelector {

	private static final Logger LOGGER = Logger.getLogger(ProjectSelector.class);
	private static final int RESULTS_PER_PAGE = 100;
	private static final int NUMBER_OF_PAGES = 10;
	/**
	 * Selectors for checking if the project has an supported build nature
	 */
	private static final IGithubProjectSelector[] BUILD_NATURE_SELECTORS = new IGithubProjectSelector[] {
			new GradleGithubProjectSelector() };

	private RestHighLevelClient elasticClient;
	// Change this to your own OAuthToken
	protected static String oAuthToken = "f74e2f293379718e29568b05121547fe959ffb42";
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
	public HashSet<Repository> searchForJavaRepositoryNames(int maxProjects) {
		HashSet<Repository> respositoryResults = new HashSet<Repository>();
		String url;
		int matchedProjectCount = 0;
		int checkedProjectCount = 0;
		try {
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();

			// Requests per page x 100
			for (int i = 1; i <= NUMBER_OF_PAGES; i++) {
				if (i % 30 == 0) { 
					TimeUnit.MINUTES.sleep(1);
				}
				url = "https://api.github.com/search/repositories?q=language:java&sort=stars&order=desc"
						 + "&page=" + i + "&per_page=" + RESULTS_PER_PAGE;

				HttpGet request = new HttpGet(url);
				request.addHeader("content-type", "application/json");
				request.addHeader("Authorization", "token " + oAuthToken);
				HttpResponse result = httpClient.execute(request);

				String json = EntityUtils.toString(result.getEntity(), "UTF-8");

				JsonElement jelement = new JsonParser().parse(json);
				JsonObject jobject = jelement.getAsJsonObject();
				JsonArray jarray = jobject.getAsJsonArray("items");

				for (int j = 0; j < jarray.size() && matchedProjectCount < maxProjects; j++) {
					checkedProjectCount++;
					JsonObject jo = (JsonObject) jarray.get(j);
					String fullName = jo.get("full_name").toString().replace("\"", "");
					int stars = Integer.parseInt(jo.get("stargazers_count").toString());
					int openIssues = Integer.parseInt(jo.get("open_issues").toString());
					boolean accept = false;
					for (IGithubProjectSelector b : BUILD_NATURE_SELECTORS) {
						accept |= b.accept(fullName, oAuthToken);
					}
					if (accept) {
						matchedProjectCount++;
						LOGGER.log(Level.INFO, "MATCH : " + fullName);

						String product = jo.get("name").toString().replace("\"", "");

						JsonObject owner = (JsonObject) jo.get("owner");
						String vendor = owner.get("login").toString().replace("\"", "");

						respositoryResults.add(new Repository(vendor, product, stars, openIssues));
					}

					LOGGER.log(Level.INFO, j);

				}
				if (matchedProjectCount >= maxProjects) {
					break;
				}

				// addDocumentsToElastic(respositoryResults);
				// respositoryResults.clear();
			}

			httpClient.close();

		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getStackTrace());
		}
		LOGGER.log(Level.DEBUG, String.format("Checked %d projcts in total, out of which %d projects matched at least one selector", checkedProjectCount, matchedProjectCount));
		return respositoryResults;
	}

	/**
	 * Adds the found repositories to an Elasticsearch DB.
	 * 
	 * @param repositoriesSet repositories to be added to the Elasticsearch database
	 *                        (index).
	 */
	private void addDocumentsToElastic(HashSet<Repository> repositoriesSet) {
		ArrayList<HashMap<String, Object>> repositories = new ArrayList<HashMap<String, Object>>();

		// Build the repository document
		for (Repository repositoryResult : repositoriesSet) {
			HashMap<String, Object> repository = new HashMap<String, Object>();
			repository.put("Vendor", repositoryResult.getVendor());
			repository.put("Product", repositoryResult.getProduct());
			repository.put("Stars", repositoryResult.getStars());
			repositories.add(repository);
		}

		elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));

		IndexRequest indexRequest = null;

		for (Iterator<HashMap<String, Object>> iterator = (repositories).iterator(); iterator.hasNext();) {
			indexRequest = new IndexRequest(repositoryDatabaseName, "doc").source(iterator.next());

			try {
				elasticClient.index(indexRequest);
			} catch (Exception e) {
				LOGGER.log(Level.ERROR, "Could not index document " + iterator.toString());
				LOGGER.log(Level.ERROR, e.getMessage(), e);
			}
		}

		LOGGER.log(Level.INFO, "Inserting " + repositories.size() + " documents into index.");

		try {
			elasticClient.close();
		} catch (IOException e) {
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
		HashSet<SearchHit> results = new HashSet<SearchHit>();

		float numberOfRepositoriesWithVulnerabilities = 0;
		float percentageOfRepositoriesWithVulnerabilities = 0;
		float totalNumberOfProjects = 0;

		elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
		VulnerabilityDataQueryHandler vulnerabilityDataQueryHandler = new VulnerabilityDataQueryHandler();

		SearchRequest searchRequest = new SearchRequest(repositoryDatabaseName);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.size(1000);
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());

		searchRequest.source(searchSourceBuilder);

		try {
			SearchResponse searchResponse = elasticClient.search(searchRequest);
			totalNumberOfProjects = searchResponse.getHits().getTotalHits();

			SearchHits repositoryHits = searchResponse.getHits();
			SearchHit[] repositorySearchHits = repositoryHits.getHits();

			for (SearchHit repository : repositorySearchHits) {

				Map<String, Object> map = repository.getSourceAsMap();

				String product = map.get("Product").toString();
				String vendor = map.get("Vendor").toString();

				HashSet<SearchHit> vulnerabilities = vulnerabilityDataQueryHandler.getVulnerabilities(product, vendor,
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

			elasticClient.close();
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return results;
	}

	/**
	 * Gets the average number of stars in the Elasticsearch repository database.
	 */
	public void getAverageNumberOfStars() {
		elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));

		SearchRequest searchRequest = new SearchRequest(repositoryDatabaseName);
		AvgAggregationBuilder avgAB = AggregationBuilders.avg("avg_stars").field("Stars");

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchAllQuery()).aggregation(avgAB);
		searchRequest.source(searchSourceBuilder);

		try {
			SearchResponse searchResponse = elasticClient.search(searchRequest);
			Aggregations aggregations = searchResponse.getAggregations();

			Avg avgStars = aggregations.get("avg_stars");

			double avg = avgStars.getValue();

			LOGGER.log(Level.INFO, avg);
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Could not get average number of stars.");
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		try {
			elasticClient.close();
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Could not close RestHighLevelClient!");
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Gets the average number of discovered vulnerabilities for the repositories in
	 * the Elasticsearch repository database.
	 */
	public double getAverageNumberOfDiscoveredVulnerabilities() {
		elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
		VulnerabilityDataQueryHandler vulnerabilityDataQueryHandler = new VulnerabilityDataQueryHandler();
		long totalNumberOfProjects = 0;
		long totalNumberOfVulnerabilites = 0;
		double averageVulnerabilitiesPerProject = 0;

		SearchRequest searchRequest = new SearchRequest(repositoryDatabaseName);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.size(1000);
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchRequest.source(searchSourceBuilder);

		try {
			SearchResponse searchResponse = elasticClient.search(searchRequest);
			totalNumberOfProjects = searchResponse.getHits().getTotalHits();

			SearchHits repositoryHits = searchResponse.getHits();
			SearchHit[] searchHits = repositoryHits.getHits();

			for (SearchHit searchHit : searchHits) {
				Map<String, Object> map = searchHit.getSourceAsMap();
				String product = map.get("Product").toString();
				String vendor = map.get("Vendor").toString();
				HashSet<SearchHit> vulnerabilities = vulnerabilityDataQueryHandler.getVulnerabilities(product, vendor,
						"", "TWO");
				totalNumberOfVulnerabilites += vulnerabilities.size();
			}

			averageVulnerabilitiesPerProject = totalNumberOfVulnerabilites / (double)totalNumberOfProjects;

			LOGGER.log(Level.INFO,
					"The average number of discovered vulnerabilities is : " + averageVulnerabilitiesPerProject);
			LOGGER.log(Level.INFO,
					"The total number of discovered vulnerabilities is : " + totalNumberOfVulnerabilites);

			elasticClient.close();
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return averageVulnerabilitiesPerProject;
	}

}
