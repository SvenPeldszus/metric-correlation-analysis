package metric.correlation.analysis.vulnerabilities.cve.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Configuration {
	@SerializedName("nodes")
	@Expose
	private List<Node> nodes;

	public List<Node> getNodes() {
		return this.nodes;
	}
}