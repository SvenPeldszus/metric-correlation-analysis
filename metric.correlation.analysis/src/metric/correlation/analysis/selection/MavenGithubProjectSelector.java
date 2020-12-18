package metric.correlation.analysis.selection;

public class MavenGithubProjectSelector extends FileBasedGithubprojectSelector {

	public MavenGithubProjectSelector() {
		super("pom.xml");
	}

}
