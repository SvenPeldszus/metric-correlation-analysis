package metric.correlation.analysis.selection;

public interface IGithubProjectSelector {
	boolean accept(String projectname, String oAuthToken);
}
