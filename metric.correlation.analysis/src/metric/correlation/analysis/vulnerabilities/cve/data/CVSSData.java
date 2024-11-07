package metric.correlation.analysis.vulnerabilities.cve.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CVSSData {
	@SerializedName("baseScore")
	@Expose
	private double baseScore;

	public double getBaseScore() {
		return this.baseScore;
	}
}