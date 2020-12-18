package metric.correlation.analysis.issues;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import metric.correlation.analysis.database.MongoDBHelper;
import metric.correlation.analysis.io.VersionHelper;
import metric.correlation.analysis.issues.Issue.IssueType;
import metric.correlation.analysis.selection.GitHubProjectSelector;
import metric.correlation.analysis.selection.ProjectsOutputCreator;

public class GithubIssueCrawler implements IssueCrawler {
	private static final boolean STORE_ISSUES = false;
	private static final Logger LOGGER = Logger.getLogger(GithubIssueCrawler.class);
	private static final boolean USE_DATABASE = false;
	private HashMap<String, String> releaseCommits;
	private List<String> releases = new ArrayList<>(); // sorted list of release versions
	private final String lastProject = "";
	private LocalDate releaseDate;
	private LocalDate nextReleaseDate;
	private Classifier classifier = new NLPClassifier();
	private final Map<String, List<String>> versionList; // this is used for manual release order, because automated can
	// have
	// some issues
	private static final Double DAYS_PER_MONTH = 30.4;

	public GithubIssueCrawler() {
		this.versionList = new HashMap<>();
		this.versionList.put("antlr4", Arrays.asList("4.0", "4.1", "4.2", "4.2.1", "4.2.2", "4.3", "4.4", "4.5",
				"4.5.1", "4.5.1-1", "4.5.2", "4.5.3", "4.6", "4.7", "4.7.1", "4.7.2", "4.8"));
	}

	@Override
	public List<Issue> getIssues(final String vendor, final String product, final String version) throws IOException {
		if (!this.lastProject.equals(product)) {
			getReleases(vendor, product); // fetches release data
		}
		try {
			this.releaseDate = getReleaseDate(vendor, product, version);
			List<Issue> issues;
			if (USE_DATABASE) {
				issues = fetchIssuesFromDatabase(product);
			} else {
				issues = getIssuesAfterDate(vendor, product, this.releaseDate); // github api only has afterDate filter
			}
			this.nextReleaseDate = getNextReleaseDate(vendor, product, version); // today if latest release
			LOGGER.info("release: " + this.releaseDate);
			LOGGER.info("until: " + this.nextReleaseDate);
			return filterIssues(issues);
		} catch (final IOException e) {
			LOGGER.error("Error while trying to collect relevant issues", e);
			return null;
		}
	}

	private List<Issue> fetchIssuesFromDatabase(final String product) {
		final List<Issue> issues = new LinkedList<>();
		try (MongoDBHelper mongo = new MongoDBHelper(MongoDBHelper.DEFAULT_DATABASE, product + "issues")) {
			final List<Document> maps = mongo.getDocuments(new HashMap<>());
			for (final Document doc : maps) {
				final Issue issue = new Issue();
				issue.fromDocument(doc);
				issues.add(issue);
			}
		}
		return issues;
	}

	private LocalDate getNextReleaseDate(final String vendor, final String product, final String version)
			throws IOException {
		if (product.equals("Activiti")) {
			return LocalDate.now(); // they have old releases with higher version number...
		}
		if (product.equals("jib")) {
			return LocalDate.of(2020, 8, 7);
		}
		final int pos = this.releases.indexOf(version);
		if (pos == (this.releases.size() - 1)) {
			return LocalDate.now(); // we are at the latest release
		}
		return getReleaseDate(vendor, product, this.releases.get(pos + 1)); // date of next release
	}

	@Override
	public double getReleaseDurationInMonths() {
		return ChronoUnit.DAYS.between(this.releaseDate, this.nextReleaseDate) / DAYS_PER_MONTH;
	}

	private List<Issue> getIssuesAfterDate(final String vendor, final String product, final LocalDate releaseDate2)
			throws IOException {
		final List<Issue> issues = new ArrayList<>();
		final List<Map<String, Object>> tmpList = new ArrayList<>();
		for (int i = 1; i < 150; i++) {
			tmpList.clear();
			LOGGER.info("at page " + i);
			final String path = "https://api.github.com/repos/" + vendor + "/" + product + "/issues?since="
					+ this.releaseDate.toString() + "&per_page=100&page=" + i + "&state=all";
			final JsonArray jArray = getJsonFromURL(path).getAsJsonArray();
			if (jArray.size() == 0) {
				break;
			}
			for (final JsonElement elem : jArray) {
				final JsonObject issueJsonObject = (JsonObject) elem;
				final JsonElement pr = issueJsonObject.get("pull_request");
				if (pr == null) { // we don't want pull requests, which are also issues
					final Issue issue = parseJsonIssue(issueJsonObject);
					issues.add(issue);
					tmpList.add(issue.asMap());
					LOGGER.info("finished issue " + issues.size());
				}
			}
			if (STORE_ISSUES) {
				try (MongoDBHelper db = new MongoDBHelper("metric_correlation", product + "-issues")) {
					db.storeMany(tmpList);
				}
			}

		}
		return issues;
	}

	private Issue parseJsonIssue(final JsonObject issueJsonObject) throws IOException {
		final Issue issue = new Issue();
		issue.setUrl(issueJsonObject.get("url").getAsJsonPrimitive().getAsString());
		issue.setId(issueJsonObject.get("id").getAsJsonPrimitive().getAsString());
		issue.setNumber(Integer.parseInt(issueJsonObject.get("number").getAsNumber().toString()));
		issue.setCreationDate(parseDate(issueJsonObject.get("created_at").getAsJsonPrimitive().getAsString()));
		if (issueJsonObject.get("state").getAsString().equals("closed")) {
			issue.setClosed(true);
			issue.setClosingDate(parseDate(issueJsonObject.get("closed_at").getAsJsonPrimitive().getAsString()));
		}
		issue.setTitle(issueJsonObject.get("title").getAsJsonPrimitive().getAsString());
		issue.setBody(issueJsonObject.get("body").getAsString());
		for (final JsonElement elem : issueJsonObject.get("labels").getAsJsonArray()) {
			final String labelName = elem.getAsJsonObject().get("name").getAsString().toLowerCase();
			issue.addLabel(labelName);
		}
		final String commentsUrl = issueJsonObject.get("comments_url").getAsJsonPrimitive().getAsString();
		final JsonElement commentsElem = getJsonFromURL(commentsUrl);
		for (final JsonElement elem : commentsElem.getAsJsonArray()) {
			final String comment = elem.getAsJsonObject().get("body").getAsJsonPrimitive().getAsString();
			issue.addComment(comment);
		}
		final IssueType type = getIssueType(issue);
		if (type != null) {
			issue.setType(type);
		}
		return issue;
	}

	private IssueType getIssueType(final Issue issue) {
		boolean bug = false;
		boolean sec = false;
		boolean request = false;
		for (final String labelName : issue.getLabels()) {
			if (isBugLabel(labelName)) {
				bug = true;
			}
			if (isSecurityLabel(labelName)) {
				sec = true;
			}
			if (isRequestLabel(labelName)) {
				request = true;
			}
		}
		if (bug) {
			if (sec) {
				return IssueType.SECURITY_BUG;
			}
			return IssueType.BUG;
		}
		if (request) {
			if (sec) {
				return IssueType.SECURITY_REQUEST;
			}
			return IssueType.FEATURE_REQUEST;
		}
		if (this.classifier != null) {
			return this.classifier.classify(issue);
		}
		return null;
	}

	// TODO: use regex
	private boolean isRequestLabel(final String labelName) {
		return labelName.contains("request") || labelName.contains("enhancement") || labelName.contains("proposal")
				|| labelName.contains("question") || labelName.contains("addition") || labelName.contains("feature");
	}

	private boolean isSecurityLabel(final String labelName) {
		return labelName.contains("security") || labelName.contains("authorization")
				|| labelName.contains("authentication") || labelName.contains("oidc") || labelName.contains("saml"); // might
		// make
		// it
		// more
		// sophisticated
	}

	private boolean isBugLabel(final String labelName) {
		return labelName.contains("bug") || labelName.contains("defect"); // might make it more sophisticated
	}

	private LocalDate parseDate(final String dateStr) {
		final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()); // ISO_INSTANT
		return LocalDate.parse(dateStr, formatter);
	}

	/**
	 * creates a github api request from the given path
	 *
	 * @param path path for the request
	 * @return json response for the request
	 * @throws IOException when request fails
	 */
	public static JsonElement getJsonFromURL(final String path) throws IOException {
		if ((GitHubProjectSelector.gitRequests++ % GitHubProjectSelector.GIT_REQUESTS_PER_MINUTE) == 0) {
			LOGGER.info("waiting for api");
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (final InterruptedException e) {
				throw new IOException("Couldn't read due to interruption at waiting.", e);
			}
		}
		JsonElement jelement = null;
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			final HttpGet request = new HttpGet(path);
			request.addHeader("content-type", "application/json");
			request.addHeader("Authorization", "Token " + GitHubProjectSelector.oAuthToken);
			final HttpResponse result = httpClient.execute(request);
			final String json = EntityUtils.toString(result.getEntity(), "UTF-8");
			jelement = new JsonParser().parse(json);
		}
		return jelement;
	}

	// returns the release date of the given version
	private LocalDate getReleaseDate(final String vendor, final String product, final String version)
			throws IOException {
		final String commit = this.releaseCommits.get(version);
		if (commit == null) {
			throw new IOException("Cannot read a commit for version " + version + '.');
		}
		final String url = "https://api.github.com/repos/" + vendor + "/" + product + "/commits/" + commit;
		final JsonObject jobject = getJsonFromURL(url).getAsJsonObject();
		final JsonObject commitObject = (JsonObject) jobject.get("commit");
		final String dateStr = ((JsonObject) commitObject.get("author")).get("date").getAsJsonPrimitive().getAsString();
		final LocalDate date = parseDate(dateStr);
		if (date == null) {
			throw new NullPointerException("Date could not be retrieved");
		}
		return date;
	}

	private void getReleases(final String vendor, final String product) throws IOException {
		boolean setOnlyCommits = false;
		if (this.versionList.containsKey(product)) {
			this.releases = this.versionList.get(product); // use pre-defined release list
			setOnlyCommits = true;
		} else {
			this.releases = new ArrayList<>();
		}
		this.releaseCommits = new HashMap<>();
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			final ProjectsOutputCreator poc = new ProjectsOutputCreator();
			final JsonArray commits = poc.getReleaseCommits(httpClient, vendor, product, Integer.MAX_VALUE);
			for (final JsonElement jo : commits) {
				final String version = ((JsonObject) jo).get("version").getAsString();
				final String commit = ((JsonObject) jo).get("commitId").getAsString();
				if (version.matches(".*\\d.*")) { // version needs a number for comparisons
					this.releaseCommits.put(version, commit);
				}
			}
			if (!setOnlyCommits) {
				this.releaseCommits.keySet().forEach(key -> this.releases.add(key));
				sortReleases();
			} else { // make sure every commit id is set
				for (final String version : this.releases) {
					if (!this.releaseCommits.containsKey(version)) {
						throw new IOException("Mising commit id for " + version);
					}
				}
			}
		}
	}

	/**
	 * sort releases by name // MAYBE ADD SORTING BY DATE
	 */
	private void sortReleases() {
		this.releases.sort(VersionHelper::compare);
	}

	//@Test
	public void test() throws IOException {
		final GithubIssueCrawler crawler = new GithubIssueCrawler();
		final List<Issue> issues = crawler.getIssues("quarkusio", "quarkus", "0.0.1");
		try (MongoDBHelper db = new MongoDBHelper("metric_correlation", "issues")) {
			final List<Map<String, Object>> test = new ArrayList<>();
			for (final Issue issue : issues) {
				test.add(issue.asMap());
			}
			db.storeMany(test);
		}
	}

	// This was used to get issues with a specific label for the classification
	// training
	@Test
	public void getLabelData() throws IOException {
		final List<Map<String, Object>> tmpList = new ArrayList<>();
		final int pages = 200;
		final String label = "%3Eenhancement";
		for (int i = 1; i < pages; i++) {
			LOGGER.info("at page " + i);
			tmpList.clear();
			final String url = "https://api.github.com/search/issues?q=language:java+label:" + label
					+ "+repo:elastic/elasticsearch+type:issue&per_page=100&page=" + i;
			final JsonObject ob = getJsonFromURL(url).getAsJsonObject();
			final JsonArray ar = ob.get("items").getAsJsonArray();
			if (ar.size() == 0) {
				break;
			}
			for (final JsonElement elem : ar) {
				final JsonObject issueJsonObject = (JsonObject) elem;
				final JsonElement pr = issueJsonObject.get("pull_request");
				if (pr == null) { // we don't want pull requests, which are also issues
					try {
						final Issue issue = parseJsonIssue(issueJsonObject);
						tmpList.add(issue.asMap());
					} catch (final Exception e) {
						LOGGER.error(e.getMessage(), e);
					}

				}
			}
			try (MongoDBHelper db = new MongoDBHelper("test", "issues")) {
				db.storeMany(tmpList);
			}
		}
	}

	// filter issues from a release until next one
	private List<Issue> filterIssues(final List<Issue> issues) {
		return issues.stream().filter(i -> i.getCreationDate().isAfter(this.releaseDate)
				&& i.getCreationDate().isBefore(this.nextReleaseDate)).collect(Collectors.toList());
	}

	@Override
	public void setClassifier(final Classifier classifier) {
		this.classifier = classifier;
	}

}
