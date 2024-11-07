package metric.correlation.analysis.vulnerabilities.cve.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CPEMatch {
	@SerializedName("vulnerable")
	@Expose
	private boolean vulnerable;

	@SerializedName("criteria")
	@Expose
	private String criteria;

	public boolean getVulnerable() {
		return this.vulnerable;
	}

	public String getCriteria() {
		return this.criteria;
	}
}