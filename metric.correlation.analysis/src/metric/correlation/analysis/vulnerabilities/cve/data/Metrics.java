package metric.correlation.analysis.vulnerabilities.cve.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Metrics {
	@SerializedName("cvssMetricV2")
	@Expose
	private List<CVSSMetricV2> cvssV2;

	@SerializedName("cvssMetricV3")
	@Expose
	private List<CVSSMetricV3> cvssV3;

	public List<CVSSMetricV2> getCVSSv2() {
		return this.cvssV2;
	}

	public List<CVSSMetricV3> getCVSSv3() {
		return this.cvssV3;
	}
}