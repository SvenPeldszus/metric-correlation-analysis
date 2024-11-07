package metric.correlation.analysis.vulnerabilities.cve.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Description {
	@SerializedName("lang")
	@Expose
	private String language;

	@SerializedName("value")
	@Expose
	private String value;

	public String getLanguage() {
		return this.language;
	}

	public String getValue() {
		return this.value;
	}
}