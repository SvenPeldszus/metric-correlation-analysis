package metric.correlation.analysis.issues;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import org.apache.log4j.Level;
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
import metric.correlation.analysis.projectSelection.ProjectSelector;
import metric.correlation.analysis.projectSelection.ProjectsOutputCreator;

public class GithubIssueCrawler implements IssueCrawler {
	private static final boolean STORE_ISSUES = false;
	private static final Logger LOGGER = Logger.getLogger(GithubIssueCrawler.class);
	protected static String oAuthToken = "1a35c67d8e36c5f52eb14fc47d67a5cceb9a5299 ";
	private static final boolean USE_DATABASE = false;
	private HashMap<String, String> releaseCommits;
	private List<String> releases = new ArrayList<>(); // sorted list of release versions
	private String lastProject = "";
	private LocalDate releaseDate;
	private LocalDate nextReleaseDate;
	private Classifier classifier = new NLPClassifier();
	private Map<String, List<String>> versionList; // this is used for manual release order, because automated can have
													// some issues
	private static final Double DAYS_PER_MONTH = 30.4;
	public GithubIssueCrawler() {
		versionList = new HashMap<>();
		versionList.put("antlr4", Arrays.asList(new String[] { "4.0", "4.1", "4.2", "4.2.1", "4.2.2", "4.3", "4.4",
				"4.5", "4.5.1", "4.5.1-1", "4.5.2", "4.5.3", "4.6", "4.7", "4.7.1", "4.7.2", "4.8" }));
	}

	@Override
	public List<Issue> getIssues(String vendor, String product, String version) {
		List<Issue> issues;
		if (!lastProject.equals(product)) {
			getReleases(vendor, product); // fetches release data
		}
		try {
			releaseDate = getReleaseDate(vendor, product, version);
			if (USE_DATABASE) {
				issues = fetchIssuesFromDatabase(product);
			} else {
				issues = getIssuesAfterDate(vendor, product, releaseDate); // github api only has afterDate filter
			}
			nextReleaseDate = getNextReleaseDate(vendor, product, version); // today if latest release
			issues = filterIssues(issues);
		} catch (Exception e) {
			LOGGER.error("Error while trying to collect relevant issues");
			return null;
		}
		System.out.println("release: " + releaseDate);
		System.out.println("until: " + nextReleaseDate);
		return issues;
	}

	private List<Issue> fetchIssuesFromDatabase(String product) {
		List<Issue> issues = new LinkedList<>();
		try (MongoDBHelper mongo = new MongoDBHelper(MongoDBHelper.DEFAULT_DATABASE, product + "issues")) {
			List<Document> maps = mongo.getDocuments(new HashMap<>());
			for (Document doc : maps) {
				Issue issue = new Issue();
				issue.fromDocument(doc);
				issues.add(issue);
			}
		}
		return issues;
	}

	private LocalDate getNextReleaseDate(String vendor, String product, String version) {
		if (product.equals("Activiti")) {
			return LocalDate.now(); // they have old releases with higher version number...
		}
		if (product.equals("jib")) {
			return LocalDate.of(2020, 8, 7);
		}
		int pos = releases.indexOf(version);
		if (pos == releases.size() - 1) {
			return LocalDate.now(); // we are at the latest release
		}
		try {
			return getReleaseDate(vendor, product, releases.get(pos + 1)); // date of next release
		} catch (Exception e) {
			LOGGER.error("Could not get release date of " + product + ": " + version, e);
			throw new RuntimeException();
		}
	}

	public double getReleaseDurationInMonths() {
		return ChronoUnit.DAYS.between(releaseDate, nextReleaseDate) / DAYS_PER_MONTH;
	}

	private List<Issue> getIssuesAfterDate(String vendor, String product, LocalDate releaseDate2) throws Exception {
		List<Issue> issues = new ArrayList<>();
		List<Map<String, Object>> tmpList = new ArrayList<>();
		for (int i = 1; i < 150; i++) {
			tmpList.clear();
			System.out.println("at page " + i);
			String path = "https://api.github.com/repos/" + vendor + "/" + product + "/issues?since="
					+ releaseDate.toString() + "&per_page=100&page=" + i + "&state=all";
			JsonArray jArray = getJsonFromURL(path).getAsJsonArray();
			if (jArray.size() == 0) {
				break;
			}
			for (JsonElement elem : jArray) {
				JsonObject issueJsonObject = (JsonObject) elem;
				JsonElement pr = issueJsonObject.get("pull_request");
				if (pr == null) { // we don't want pull requests, which are also issues
					Issue issue = parseJsonIssue(issueJsonObject);
					issues.add(issue);
					tmpList.add(issue.asMap());
					System.out.println("finished issue " + issues.size());
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

	private Issue parseJsonIssue(JsonObject issueJsonObject) throws Exception {
		Issue issue = new Issue();
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
		for (JsonElement elem : issueJsonObject.get("labels").getAsJsonArray()) {
			String labelName = elem.getAsJsonObject().get("name").getAsString().toLowerCase();
			issue.addLabel(labelName);
		}
		String comments_url = issueJsonObject.get("comments_url").getAsJsonPrimitive().getAsString();
		JsonElement commentsElem = getJsonFromURL(comments_url);
		for (JsonElement elem : commentsElem.getAsJsonArray()) {
			String comment = elem.getAsJsonObject().get("body").getAsJsonPrimitive().getAsString();
			issue.addComment(comment);
		}
		IssueType type = getIssueType(issue);
		if (type != null) {
			issue.setType(type);
		}
		return issue;
	}

	private IssueType getIssueType(Issue issue) {
		boolean bug = false;
		boolean sec = false;
		boolean request = false;
		for (String labelName : issue.getLabels()) {
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
		if (classifier != null) {
			return classifier.classify(issue);
		}
		return null;
	}

	// TODO: use regex
	private boolean isRequestLabel(String labelName) {
		return labelName.contains("request") || labelName.contains("enhancement") || labelName.contains("proposal")
				|| labelName.contains("question") || labelName.contains("addition") || labelName.contains("feature");
	}

	private boolean isSecurityLabel(String labelName) {
		return labelName.contains("security") || labelName.contains("authorization")
				|| labelName.contains("authentication") || labelName.contains("oidc") || labelName.contains("saml"); // might
																														// make
																														// it
																														// more
																														// sophisticated
	}

	private boolean isBugLabel(String labelName) {
		return labelName.contains("bug") || labelName.contains("defect"); // might make it more sophisticated
	}

	private LocalDate parseDate(String dateStr) {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()); // ISO_INSTANT
		return LocalDate.parse(dateStr, formatter);
	}

	/**
	 * creates a github api request from the given path
	 * 
	 * @param path path for the request
	 * @return json response for the request
	 * @throws Exception when request fails
	 */
	public static JsonElement getJsonFromURL(String path) throws Exception {
		if (ProjectSelector.GIT_REQUESTS++ % ProjectSelector.GIT_REQUESTS_PER_MINUTE == 0) {
			System.out.println("waiting for api");
			TimeUnit.SECONDS.sleep(5);
		}
		JsonElement jelement = null;
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpGet request = new HttpGet(path);
			request.addHeader("content-type", "application/json");
			request.addHeader("Authorization", "Token " + oAuthToken);
			HttpResponse result = httpClient.execute(request);
			String json = EntityUtils.toString(result.getEntity(), "UTF-8");
			jelement = new JsonParser().parse(json);
		}
		return jelement;
	}

	// returns the release date of the given version
	private LocalDate getReleaseDate(String vendor, String product, String version) throws Exception {
		String commit = releaseCommits.get(version);
		String url = "https://api.github.com/repos/" + vendor + "/" + product + "/commits/" + commit;
		LocalDate date = null;
		try {
			JsonObject jobject = getJsonFromURL(url).getAsJsonObject();
			JsonObject commitObject = (JsonObject) jobject.get("commit");
			String dateStr = ((JsonObject) commitObject.get("author")).get("date").getAsJsonPrimitive().getAsString();
			date = parseDate(dateStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (date == null) {
			throw new NullPointerException("Date could not be retrieved");
		}
		return date;
	}

	private void getReleases(String vendor, String product) {
		boolean setOnlyCommits = false;
		if (versionList.containsKey(product)) {
			releases = versionList.get(product); // use pre-defined release list
			setOnlyCommits = true;
		} else {
			releases = new ArrayList<>();
		}
		releaseCommits = new HashMap<>();
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			ProjectsOutputCreator poc = new ProjectsOutputCreator();
			JsonArray commits = poc.getReleaseCommits(httpClient, vendor, product, Integer.MAX_VALUE);
			for (JsonElement jo : commits) {
				String version = ((JsonObject) jo).get("version").getAsString();
				String commit = ((JsonObject) jo).get("commitId").getAsString();
				if (version.matches(".*\\d.*")) { // version needs a number for comparisons
					releaseCommits.put(version, commit);
				}
			}
			if (!setOnlyCommits) {
				releaseCommits.keySet().forEach(key -> releases.add(key));
				sortReleases();
			} else { // make sure every commit id is set
				for (String version : releases) {
					if (!releaseCommits.containsKey(version)) {
						throw new Exception("Mising commit id for " + version);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

	}

	/**
	 * sort releases by name // MAYBE ADD SORTING BY DATE
	 */
	private void sortReleases() {
		releases.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return VersionHelper.compare(o1, o2);
			}
		});
	}

	// @Test
	public void test() {
		GithubIssueCrawler crawler = new GithubIssueCrawler();
		List<Issue> issues = crawler.getIssues("quarkusio", "quarkus", "0.0.1");
		try (MongoDBHelper db = new MongoDBHelper("metric_correlation", "issues")) {
			List<Map<String, Object>> test = new ArrayList<>();
			for (Issue issue : issues) {
				test.add(issue.asMap());
			}
			db.storeMany(test);
		}
	}

	// This was used to get issues with a specific label for the classification
	// training
	@Test
	public void getLabelData() throws Exception {
		List<Map<String, Object>> tmpList = new ArrayList<>();
		int pages = 200;
		String label = "%3Eenhancement";
		for (int i = 1; i < pages; i++) {
			System.out.println("at page " + i);
			tmpList.clear();
			String url = "https://api.github.com/search/issues?q=language:java+label:" + label
					+ "+repo:elastic/elasticsearch+type:issue&per_page=100&page=" + i;
			JsonObject ob = getJsonFromURL(url).getAsJsonObject();
			JsonArray ar = ob.get("items").getAsJsonArray();
			if (ar.size() == 0) {
				break;
			}
			for (JsonElement elem : ar) {
				JsonObject issueJsonObject = (JsonObject) elem;
				JsonElement pr = issueJsonObject.get("pull_request");
				if (pr == null) { // we don't want pull requests, which are also issues
					try {
						Issue issue = parseJsonIssue(issueJsonObject);
						tmpList.add(issue.asMap());
					} catch (Exception e) {

					}

				}
			}
			try (MongoDBHelper db = new MongoDBHelper("test", "issues")) {
				db.storeMany(tmpList);
			}
		}
	}

	// @Test
	public void testStoring() {
		try (MongoDBHelper db = new MongoDBHelper("metric_correlation", "issues")) {
		}
	}

	// filter issues from a release until next one
	private List<Issue> filterIssues(List<Issue> issues) {
		return issues.stream()
				.filter(i -> i.getCreationDate().isAfter(releaseDate) && i.getCreationDate().isBefore(nextReleaseDate))
				.collect(Collectors.toList());
	}

	@Override
	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
	}

}
