package metric.correlation.analysis.vulnerabilities.cve.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CVSSMetricV2 {
	@SerializedName("cvssData")
	@Expose
	private CVSSData cvssData;

	public CVSSData getCVSSData() {
		return this.cvssData;
	}
}