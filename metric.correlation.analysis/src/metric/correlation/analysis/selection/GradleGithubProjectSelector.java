package metric.correlation.analysis.selection;

public class GradleGithubProjectSelector extends FileBasedGithubprojectSelector {

	public GradleGithubProjectSelector() {
		super("build.gradle");
	}

}
