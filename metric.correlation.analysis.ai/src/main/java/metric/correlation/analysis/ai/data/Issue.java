package metric.correlation.analysis.ai.data;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public record Issue(
		@SerializedName("id") String issueID,
		@SerializedName("issueUrl") String issueUrl,
		@SerializedName("repoUrl") String repoUrl,
		@SerializedName("title") String title,
		@SerializedName("description") String description,
		@SerializedName("labels") List<String> labels) {

}
