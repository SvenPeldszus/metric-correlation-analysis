package metric.correlation.analysis.vulnerabilities.cve.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Represents an NVD vulnerability page.
 *
 * @author Antoniya Ivanova
 * @author Sven Pelszus
 *
 */

public class CVEPage {

	@SerializedName("format")
	@Expose
	private String CVEDataFormat;

	@SerializedName("version")
	@Expose
	private String CVEDataVersion;

	@SerializedName("timestamp")
	@Expose
	private String CVEDataTimestamp;

	@SerializedName("resultsPerPage")
	@Expose
	private int resultsPerPage;

	@SerializedName("totalResults")
	@Expose
	private int totalResults;

	@SerializedName("vulnerabilities")
	@Expose
	private final List<Vulnerability> vulnerabilities = null;

	public List<Vulnerability> getVulnerabilities() {
		return this.vulnerabilities;
	}

	public String getCVEDataFormat() {
		return this.CVEDataFormat;
	}

	public int getResultsPerPage() {
		return this.resultsPerPage;
	}

	public int getTotalResults() {
		return this.totalResults;
	}
}