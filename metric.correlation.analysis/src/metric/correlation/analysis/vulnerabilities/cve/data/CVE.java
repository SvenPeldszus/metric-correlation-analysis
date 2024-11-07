package metric.correlation.analysis.vulnerabilities.cve.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CVE {
	@SerializedName("id")
	@Expose
	private String id;

	@SerializedName("vulnStatus")
	@Expose
	private String vulnStatus;

	@SerializedName("descriptions")
	@Expose
	private List<Description> descriptions;

	@SerializedName("metrics")
	@Expose
	private Metrics metrics;

	@SerializedName("configurations")
	@Expose
	private List<Configuration> configurations;

	@SerializedName("references")
	@Expose
	private List<Reference> references;

	public String getID() {
		return this.id;
	}

	public List<Configuration> getConfigurations() {
		return this.configurations;
	}

	public List<Description> getDescriptions() {
		return this.descriptions;
	}

	public Metrics getMetrics() {
		return this.metrics;
	}

	public String getVulnerabilityStatus() {
		return this.vulnStatus;
	}

	public List<Reference> getReferences() {
		return references;
	}
}