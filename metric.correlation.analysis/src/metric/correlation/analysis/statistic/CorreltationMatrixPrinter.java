package metric.correlation.analysis.statistic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class CorreltationMatrixPrinter {

	private static final Logger LOGGER = Logger.getLogger(CorreltationMatrixPrinter.class);

	private CorreltationMatrixPrinter() {
		// This class only provides static methods and should not be instantiated
	}
	
	/**
	 * 
	 * @param matrix
	 * @param metricNames
	 * @param resultFile
	 * @throws IOException
	 */
	public static void storeMatrix(RealMatrix matrix, List<String> metricNames, File resultFile) throws IOException {
		if (!resultFile.createNewFile()) {
			throw new IOException("The results file \"" + resultFile + "\" exists already!");
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))) {
			printMatrix(matrix, metricNames, writer);
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Prints the matrix to the writer
	 * 
	 * @param matrix
	 * @param metricNames
	 * @param writer
	 * @throws IOException
	 */
	public static void printMatrix(RealMatrix matrix, List<String> metricNames, Writer writer)
			throws IOException {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);
		
		writer.write(","); //Upper left corner is empty
		for (int j = 0; j < metricNames.size(); j++) {
			writer.write(metricNames.get(j));
			writer.write(",");
		}
		for (int i = 0; i < metricNames.size(); i++) {
			double[] row = matrix.getRow(i);
			writer.write(System.lineSeparator());
			writer.write(metricNames.get(i));
			for (double r : row) {
				writer.write(", ");
				writer.write(dFormat.format(r));
			}
		}
	}

}
