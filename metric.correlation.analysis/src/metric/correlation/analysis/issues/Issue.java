package metric.correlation.analysis.issues;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

public class Issue {
	private String url;
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	private List<String> labels;
	private List<String> comments;
	private int number;
	private String title;
	private String body;
	private boolean closed;
	private LocalDate creationDate;
	private LocalDate closingDate;
	private IssueType type;
	private List<String> commits;

	public Issue() {
		labels = new ArrayList<>();
		comments = new ArrayList<>();
		commits = new ArrayList<>();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void addLabel(String label) {
		labels.add(label);
	}

	public List<String> getComments() {
		return comments;
	}

	public void addComment(String comment) {
		comments.add(comment);
	}

	public void addCommit(String commit) {
		commits.add(commit);
	}

	public List<String> getCommits() {
		return commits;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public LocalDate getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}

	public LocalDate getClosingDate() {
		return closingDate;
	}

	public void setClosingDate(LocalDate closingDate) {
		this.closingDate = closingDate;
	}

	public IssueType getType() {
		return type;
	}

	public void setType(IssueType type) {
		this.type = type;
	}

	public enum IssueType {
		BUG("BUG"), SECURITY_BUG("SECURITY_BUG"), SECURITY_REQUEST("SECURITY_REQUEST"),
		FEATURE_REQUEST("FEATURE_REQUEST");

		private String value;

		private IssueType(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	// create map with issue-data for storing as json
	public Map<String, Object> asMap() { // for db storing
		Map<String, Object> result = new HashMap<>();
		result.put("url", url);
		result.put("id", id);
		result.put("comments", comments);
		result.put("labels", labels);
		result.put("number", number);
		result.put("title", title);
		result.put("body", body);
		result.put("closed", closed);
		result.put("creationDate", creationDate.toString());
		result.put("closingDate", closed ? closingDate.toString() : null);
		result.put("commits", commits);
		if (type != null) {
			result.put("type", type.toString());
		}
		return result;
	}

	// needs testing and finishing
	public void fromDocument(Document doc) {
		url = (String) doc.get("url");
		id = (String) doc.get("id");
		number = (Integer) doc.get("number");
		title = (String) doc.get("title");
		body = (String) doc.get("body");
		closed = (boolean) doc.get("closed");
		creationDate = LocalDate.parse((String) doc.get("creationDate"), DateTimeFormatter.ISO_DATE);
		if (closed) {
			closingDate = LocalDate.parse((String) doc.get("closingDate"), DateTimeFormatter.ISO_DATE);
		}
		type = IssueType.valueOf((String) doc.get("type"));
		labels = (List<String>) doc.get("labels");
		comments = (List<String>) doc.get("comments");
		commits = (List<String>) doc.get("commits");
	}
}
