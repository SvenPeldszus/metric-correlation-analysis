package metric.correlation.analysis.vulnerabilities;

public class ProjectRecallPrecisionTriple<projectName, recall, precision> {

	/**
	 * @author Antoniya Ivanova A class to store a triple of project, recall,
	 *         precision
	 *
	 */

	private final String projectName;
	private Float recall;
	private Float precision;

	public ProjectRecallPrecisionTriple(final String projectName) {
		this.projectName = projectName;
	}

	public void setRecall(final Float recall) {
		this.recall = recall;
	}

	public void setPrecision(final Float precision) {
		this.precision = precision;
	}

	public String getProjectName() {
		return this.projectName;
	}

	public Float getRecall() {
		return this.recall;
	}

	public Float getPrecision() {
		return this.precision;
	}

}
