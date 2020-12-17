package metric.correlation.analysis.project_selection;

public class GradleGithubProjectSelector extends FileBasedGithubprojectSelector {

	public GradleGithubProjectSelector() {
		super("build.gradle");
	}

}
