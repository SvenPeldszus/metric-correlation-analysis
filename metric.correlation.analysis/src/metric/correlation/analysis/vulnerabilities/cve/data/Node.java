package metric.correlation.analysis.vulnerabilities.cve.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Node {
	@SerializedName("cpeMatch")
	@Expose
	private List<CPEMatch> cpeMatches;

	public List<CPEMatch> getCPEMatches() {
		return this.cpeMatches;
	}
}