package metric.correlation.analysis.vulnerabilities;

public class NoAffectedProductException extends Exception {

	private static final long serialVersionUID = 9213661398745731763L;

	private final String cve;

	public NoAffectedProductException(final String cve) {
		this.cve = cve;
	}

	@Override
	public String toString() {
		return super.toString() + '(' + this.cve + ')';
	}
}
