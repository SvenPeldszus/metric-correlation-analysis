package metric.correlation.analysis.statistic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import flanagan.analysis.Normality;

public class NormalDistribution {

	private static final Logger LOGGER = Logger.getLogger(NormalDistribution.class);

	public static final double SIGNIFICANCE_LEVEL = 0.05;

	private final DecimalFormat dFormat;

	public NormalDistribution() {
		final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		this.dFormat = new DecimalFormat("0.00", dfs);
	}

	public void testAndStoreNormalDistribution(final Map<String, List<Double>> metricValues, final File resultFile) {

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))){
			writer.write(
					"Metric name, W-Value, W-crit, Normal Distribution, P-Value, Significance, Normal Distribution");
			for (final Entry<String, List<Double>> entry : metricValues.entrySet()) {
				final List<Double> list = entry.getValue();
				final double[] doubleArray = new double[list.size()];
				for(int i = 0; i < list.size(); i++) {
					doubleArray[i] = list.get(i);
				}
				final Normality norm = new Normality(doubleArray);
				final double wValue = norm.shapiroWilkWvalue();
				final double wCritical = norm.shapiroWilkCriticalW();
				final boolean normalDistribution = wValue <= wCritical;
				final double pValue = norm.shapiroWilkPvalue();
				final boolean normalDistribution2 = pValue <= SIGNIFICANCE_LEVEL;

				printNextLine(writer, entry.getKey(), wValue, wCritical, normalDistribution, pValue, normalDistribution2);
			}
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Prints the next line
	 *
	 * @param writer The writer
	 * @param metricName
	 * @param wValue
	 * @param wCritical
	 * @param normalDistribution
	 * @param pValue
	 * @param normalDistribution2
	 * @throws IOException
	 */
	public void printNextLine(final BufferedWriter writer, final String metricName, final double wValue, final double wCritical,
			final boolean normalDistribution, final double pValue, final boolean normalDistribution2)
					throws IOException {
		writer.newLine();
		writer.write(metricName);
		writer.write(",");
		writer.write(this.dFormat.format(wValue));
		writer.write(",");
		writer.write(this.dFormat.format(wCritical));
		writer.write(",");
		writer.write(Boolean.toString(normalDistribution));
		writer.write(",");
		writer.write(this.dFormat.format(pValue));
		writer.write(",");
		writer.write(this.dFormat.format(SIGNIFICANCE_LEVEL));
		writer.write(",");
		writer.write(Boolean.toString(normalDistribution2));
	}

	/**
	 *  size of the returned matrix: double[][] = [AnzahlMetriken][Anzahl Apps]
	 *
	 * @param metrics A linked hashmap of metric keys and values
	 * @return a double matrix
	 */
	public double[][] getValues(final LinkedHashMap<String, List<Double>> metrics) {
		final double[][] results = new double[metrics.size()][];
		int metricIndex = 0;
		for(final List<Double> values : metrics.values()) {
			final double[] doubleArray = new double[values.size()];
			for (final Double value : values) {
				doubleArray[metricIndex] = value;
			}
			results[metricIndex++] = doubleArray;
		}

		return results;
	}

}