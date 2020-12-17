package metric.correlation.analysis.project_selection;

public interface IGithubProjectSelector {
	boolean accept(String projectname, String oAuthToken);
}
