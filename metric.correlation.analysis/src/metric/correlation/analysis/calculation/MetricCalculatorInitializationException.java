package metric.correlation.analysis.calculation;

public class MetricCalculatorInitializationException extends Exception {

	private static final long serialVersionUID = -4294520150849345235L;

	public MetricCalculatorInitializationException(Throwable causedBy) {
		super(causedBy);
	}

	public MetricCalculatorInitializationException(String description) {
		super(description);
	}

}
