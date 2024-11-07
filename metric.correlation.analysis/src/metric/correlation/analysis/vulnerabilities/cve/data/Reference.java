package metric.correlation.analysis.vulnerabilities.cve.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Reference {

	@SerializedName("url")
	@Expose
	private String url;

	public String getURL() {
		return this.url;
	}
}
