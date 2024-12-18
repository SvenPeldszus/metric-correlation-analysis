package metric.correlation.analysis.ai;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import metric.correlation.analysis.ai.data.Issue;

public class GitHubIssueCrawler {

	private static final int RESULTS_PER_PAGE = 100;
	public static final String OAuthToken = System.getenv("GITHUB_OAUTH");

	public static void main(final String[] args) throws IOException {

		final var securityFeatureRequests = searchIssuesWithLabel(new String[] { "security", "enhancement" },
				new String[] {}, -1);
		final var featureRequests = searchIssuesWithLabel(new String[] { "enhancement" }, new String[] { "security" },
				securityFeatureRequests);
		System.out.println("security: " + securityFeatureRequests + ", other: " + featureRequests);
	}

	private static int searchIssuesWithLabel(final String[] searchLabels, final String[] excludeLabels,
			final int maxIssues)
			throws IOException {
		final var targetFolder = new File(new File("issues"), String.join("_", searchLabels));
		targetFolder.mkdirs();

		try (var httpClient = HttpClient.newHttpClient();) {
			var issues = 0;
			var page = 1;

			final var today = Calendar.getInstance();
			final var status = new File(".year");
			var year = getStartYear(status);
			var month = 01;

			while (true) {
				var url = "https://api.github.com/search/issues?q=";
				for (final var label : searchLabels) {
					url += "label:" + label + "+";
				}
				url += "created:" + year + "-" + String.format("%02d", month)
						+ "&page=" + page
						+ "&per_page=" + RESULTS_PER_PAGE;
				System.out.print("GET " + url);
				final var request = HttpRequest.newBuilder().uri(URI.create(url))
						.header("content-type", "application/json").header("Authorization", "Token " + OAuthToken)
						.build();

				HttpResponse<String> result;
				try {
					result = httpClient.send(request, BodyHandlers.ofString());
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
					System.out.println("Retry");
					continue;
				}
				System.out.println(" " + result.statusCode());
				sleep(100);

				final var statusCode = result.statusCode();
				if (statusCode == 403) {

					final var reset = Long.parseLong(result.headers().firstValue("X-RateLimit-Reset").get()) * 1000L;
					final var sleep = reset - System.currentTimeMillis() + 1000;
					sleep(sleep);
					continue;
				}
				if (statusCode == 422) {
					System.err.println("More than 1,000 issues as search result!");
				}

				final var string = result.body();
				final var jobject = new JsonParser().parse(string).getAsJsonObject();

				System.out.println(issues + " + " + RESULTS_PER_PAGE + " / " + jobject.get("total_count"));

				final var jarray = jobject.getAsJsonArray("items");
				if (jarray == null || jarray.size() == 0) {
					// no further results
					if (year == today.get(Calendar.YEAR) && month == 1 + today.get(Calendar.MONTH)) {
						return issues;
					}
					if (month == 12) {
						year++;
						Files.write(status.toPath(), Integer.toString(year).getBytes());
						month = 01;
					} else {
						month++;
					}
					page = 1;
					continue;
				}

				for (final var issue : jarray) {
					final var issueObject = issue.getAsJsonObject();
					final var issueID = issueObject.get("id").getAsString();
					final var issueUrl = issueObject.get("url").getAsString();
					final var repoUrl = issueObject.get("repository_url").getAsString();
					final var title = issueObject.get("title").getAsString();
					final var jsonDescription = issueObject.get("body");
					final var description = jsonDescription.isJsonNull() ? "" : jsonDescription.getAsString();
					final var labels = new ArrayList<String>();
					for (final var label : issueObject.get("labels").getAsJsonArray()) {
						final var labelName = label.getAsJsonObject().get("name").getAsString();
						labels.add(labelName);
					}
					if (Stream.of(excludeLabels).anyMatch(labels::contains)) {
						continue;
					}

					try (var writer = (new FileWriter(new File(targetFolder, issueID + ".json")))) {
						final var json = new Gson()
								.toJson(new Issue(issueID, issueUrl, repoUrl, title, description, labels));
						writer.write(json);
					}
					issues++;
					if (issues >= maxIssues) {
						return issues;
					}
				}
				page++;
			}
		}
	}

	private static int getStartYear(final File status) {
		var year = 2000;
		if (status.exists()) {
			try {
				year = Integer.parseInt(Files.readString(status.toPath()));
			} catch (NumberFormatException | IOException e) {
				e.printStackTrace();
			}
		}
		return year;
	}

	private static void sleep(final long sleepMIllis) {
		System.out.println("Sleep " + sleepMIllis + "ms");
		try {
			Thread.sleep(sleepMIllis);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}
}
