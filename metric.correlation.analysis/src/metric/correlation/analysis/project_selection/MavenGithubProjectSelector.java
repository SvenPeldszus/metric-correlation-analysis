package metric.correlation.analysis.project_selection;

public class MavenGithubProjectSelector extends FileBasedGithubprojectSelector {

	public MavenGithubProjectSelector() {
		super("pom.xml");
	}

}
