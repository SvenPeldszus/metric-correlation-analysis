package metric.correlation.analysis.database;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticSearchHelper {

	private static final String ES_USERNAME = System.getenv("ELASTIC_USER");
	private static final String ES_PASSWORD = System.getenv("ELASTIC_PW");

	private static final String HOST = "localhost";
	private static final int PORT = 9200;

	public static RestHighLevelClient getElasticSearchClient() {
		final CredentialsProvider credentialProvider = new BasicCredentialsProvider();
		credentialProvider.setCredentials(
				AuthScope.ANY,
				new UsernamePasswordCredentials(
						ES_USERNAME,
						ES_PASSWORD));

		return new RestHighLevelClient(
				RestClient.builder(new HttpHost(HOST, PORT, "http")).setHttpClientConfigCallback(
						httpAsyncClientBuilder -> {
							httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialProvider);
							return httpAsyncClientBuilder;
						}));
	}
}
