package metric.correlation.analysis.statistic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.util.Precision;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.Test;

import metric.correlation.analysis.calculation.impl.IssueMetrics;
import metric.correlation.analysis.calculation.impl.SourceMeterMetrics;
import metric.correlation.analysis.calculation.impl.VersionMetrics;

public class StatisticExecuter {

	private static final String INPUT_SERIES = "results";
	private static final File DATA_FILE = new File(new File("results", INPUT_SERIES), "results.csv");

	private static final Logger LOGGER = Logger.getLogger(StatisticExecuter.class);

	private static final File RESULTS = new File("statistics");

	public static void main(final String[] args) {
		final StatisticExecuter executer = new StatisticExecuter();
		try {
			executer.calculateStatistics(DATA_FILE, new File(RESULTS, INPUT_SERIES));
		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * Calculates correlations for the given metric values and saves them at the
	 * given location
	 *
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public void calculateStatistics(final File in, final File out) throws IOException {
		if (!out.exists()) {
			out.mkdirs();
		}
		calculateStatistics(getMetricMap(in), out);
	}

	/**
	 * Calculates correlations for the given metric values and saves them at the
	 * given location
	 *
	 * @param map The mapping from metric names to values
	 * @param out The output file
	 * @throws IOException
	 */
	public void calculateStatistics(final LinkedHashMap<String, List<Double>> map, final File out) throws IOException {
		final Set<String> keySet = map.keySet();
		final ArrayList<String> metricNames = new ArrayList<>(keySet);

		final RealMatrix matrix = createMatrix(map);

		//		RealMatrix pearsonMatrix = new PearsonsCorrelation().computeCorrelationMatrix(matrix);
		//		CorreltationMatrixPrinter.storeMatrix(pearsonMatrix, metricNames, new File(out, "PearsonCorrelationMatrix.csv"));

		final RealMatrix spearmanMatrix = new SpearmansCorrelation().computeCorrelationMatrix(matrix);
		CorreltationMatrixPrinter.storeMatrix(spearmanMatrix, metricNames,
				new File(out, "SpearmanCorrelationMatrix.csv"));

		//	    XYSeries series = new XYSeries("Random");
		//	    for (int i = 0; i <= 100; i++) {
		//	        double x = r.nextDouble();
		//	        double y = r.nextDouble();
		//	        series.add(x, y);
		//	    }
		//	    result.addSeries(series);
		//	    return result;

		for (int i = 0; i < keySet.size(); i++) {

			final String xMetric = metricNames.get(i);
			final List<Double> xValues = map.get(xMetric);

			for (int j = i + 1; j < keySet.size(); j++) {
				final String yMetric = metricNames.get(j);
				final List<Double> yValues = map.get(yMetric);

				final XYSeriesCollection scatterPlotResult = new XYSeriesCollection();
				final XYSeries ySeries = new XYSeries(yMetric);

				for (int counter = 0; counter < xValues.size(); counter++) {
					ySeries.add(xValues.get(counter), yValues.get(counter));
				}

				scatterPlotResult.addSeries(ySeries);
				final JFreeChart chart = ChartFactory.createScatterPlot((xMetric + " vs " + yMetric), xMetric, yMetric,
						scatterPlotResult);
				final BufferedImage chartImage = chart.createBufferedImage(600, 400);
				ImageIO.write(chartImage, "png",
						new FileOutputStream(new File(out, xMetric + "vs" + yMetric + ".png")));
			}
		}

		new NormalDistribution().testAndStoreNormalDistribution(map, new File(out, "shapiroWilkTestAll.csv"));
	}

	/**
	 * Creates a map from a stored metric csv file
	 *
	 * @param dataFile The file
	 * @return The map
	 * @throws IOException If there is an exception reading the file
	 */
	private LinkedHashMap<String, List<Double>> getMetricMap(final File dataFile) throws IOException {
		final List<String> lines = Files.readAllLines(dataFile.toPath());
		final String[] keys = lines.get(0).split(",");
		final LinkedHashMap<String, List<Double>> metrics = new LinkedHashMap<>(keys.length - 1);
		final Set<Integer> skipIndex = new HashSet<>();
		for (int i = 0; i < keys.length; i++) {
			final String value = keys[i];
			if (VersionMetrics.MetricKeysImpl.VENDOR.toString().equals(value)
					|| VersionMetrics.MetricKeysImpl.PRODUCT.toString().equals(value)
					|| VersionMetrics.MetricKeysImpl.VERSION.toString().equals(value)) {
				skipIndex.add(i);
			} else {
				metrics.put(value, new ArrayList<>(lines.size() - 1));
			}
		}
		if (skipIndex.isEmpty()) {
			throw new IllegalStateException("Project name not found");
		}
		for (final String line : lines.subList(1, lines.size())) {
			final String[] values = line.split(",");

			boolean valid = true;
			for (int i = 0; i < values.length; i++) {
				if (skipIndex.contains(i)) {
					continue;
				}
				final String s = values[i];
				if ((s == null) || "null".equals(s) || "NaN".equals(s)) {
					valid = false;
					break;
				}
			}

			if (valid) {
				for (int i = 0; i < values.length; i++) {
					if (skipIndex.contains(i)) {
						continue;
					}
					metrics.get(keys[i]).add(Double.parseDouble(values[i]));
				}
			}
		}
		return metrics;
	}

	public RealMatrix createMatrix(final Map<String, List<Double>> metricValues) {
		final double[][] results = new double[metricValues.size()][];
		int col = 0;
		for (final List<Double> s : metricValues.values()) {
			final double[] d = new double[s.size()];
			for (int i = 0; i < s.size(); i++) {
				d[i] = s.get(i);
			}
			results[col++] = d;
		}
		return new Array2DRowRealMatrix(results).transpose();
	}

	/*
	 * Version statistics
	 */

	private ArrayList<ProductMetricData> getProductMetricData(final File dataFile) throws IOException {
		final List<String> lines = Files.readAllLines(dataFile.toPath());
		final List<String> keys = Arrays.asList(lines.get(0).split(","));

		// if one of these wont be found (return -1), it would crash later when we would
		// subscribt a list with -1 index
		final int idx_version = keys.indexOf(VersionMetrics.MetricKeysImpl.VERSION.toString());
		final int idx_vendor = keys.indexOf(VersionMetrics.MetricKeysImpl.VENDOR.toString());
		final int idx_product = keys.indexOf(VersionMetrics.MetricKeysImpl.PRODUCT.toString());
		final int idx_bugsKloc = keys.indexOf(IssueMetrics.MetricKeysImpl.BUG_ISSUES_KLOC_TIME.toString());
		final int idx_bugs = keys.indexOf(IssueMetrics.MetricKeysImpl.BUG_ISSUES_TIME.toString());
		final int idx_avgTime = keys.indexOf(IssueMetrics.MetricKeysImpl.AVG_OPEN_TIME_DAYS.toString());
		final int idx_locpc = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.LOC_PER_CLASS.toString());
		final int idx_ldc = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.LDC.toString());
		final int idx_wmc = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.WMC.toString());
		final int idx_dit = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.DIT.toString());
		final int idx_lcom5 = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.LCOM.toString());
		final int idx_cbo = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.CBO.toString());
		final int idx_lloc = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.LLOC.toString());
		lines.remove(0); // remove column keys row

		final ArrayList<String> productNames = new ArrayList<>();
		final ArrayList<String> vendors = new ArrayList<>();

		final ArrayList<Double> locpcs = new ArrayList<>();
		final ArrayList<String> versions = new ArrayList<>();
		final ArrayList<Double> bugs = new ArrayList<>();
		final ArrayList<Double> ldcs = new ArrayList<>();
		final ArrayList<Double> wmcs = new ArrayList<>();
		final ArrayList<Double> dits = new ArrayList<>();
		final ArrayList<Double> lcom5s = new ArrayList<>();
		final ArrayList<Double> cbos = new ArrayList<>();
		final ArrayList<Double> bugsKloc = new ArrayList<>();
		final ArrayList<Double> llocs = new ArrayList<>();

		for (final String line : lines) {
			final String[] split = line.split(",");
			locpcs.add(Double.valueOf(split[idx_locpc]));
			productNames.add(split[idx_product]);
			vendors.add(split[idx_vendor]);
			versions.add(split[idx_version]);
			bugs.add(Double.valueOf(split[idx_bugs]));
			ldcs.add(Double.valueOf(split[idx_ldc]));
			wmcs.add(Double.valueOf(split[idx_wmc]));
			dits.add(Double.valueOf(split[idx_dit]));
			lcom5s.add(Double.valueOf(split[idx_lcom5]));
			cbos.add(Double.valueOf(split[idx_cbo]));
			bugsKloc.add(Double.valueOf(split[idx_bugsKloc]));
			llocs.add(Double.valueOf(split[idx_lloc]));
		}

		ProductMetricData metric = new ProductMetricData(productNames.get(0), vendors.get(0));
		final ArrayList<ProductMetricData> metrics = new ArrayList<>();
		metrics.add(metric);
		for (int i = 0; i < lines.size(); i++) {
			final String product = productNames.get(i);
			if (!product.equals(metric.getProductName())) {
				metric = new ProductMetricData(product, vendors.get(i));
				metrics.add(metric);
			}
			metric.addLocpc(locpcs.get(i));
			metric.addVersion(versions.get(i));
			metric.addBugKLOC(bugsKloc.get(i));
			metric.addBug(bugs.get(i));
			metric.addLDC(ldcs.get(i));
			metric.addWMC(wmcs.get(i));
			metric.addDIT(dits.get(i));
			metric.addLCOM5(lcom5s.get(i));
			metric.addCBO(cbos.get(i));
			metric.addLLOC(llocs.get(i));
		}
		return metrics;
	}

	private Double pmdDiff(final Double previous, final Double next) {
		return Precision.round((((next - previous) / previous) * 100), 2);
	}

	String[] columnNamesInVersionsFile = { "LOCpC", "BUG_ISSUES_KLOC_TIME", "BUG_ISSUES_TIME", "LDC", "WMC", "DIT",
			"LCCM3", "CBO", "LLOC" };

	// @Test
	public void writeVersionsCSVFile() {
		try {
			for (final ProductMetricData metric : getProductMetricData(new File("input/versions-results.csv"))) {

				final List<List<Double>> columns = new ArrayList<>();
				columns.add(metric.getLOCpCS());
				columns.add(metric.getBugsKLOC());
				columns.add(metric.getBugs());
				columns.add(metric.getLDCs());
				columns.add(metric.getWMCs());
				columns.add(metric.getDITs());
				columns.add(metric.getLCOM5s());
				columns.add(metric.getCBOs());
				// columns.add(metric.avgTime);
				columns.add(metric.getLLOCs());

				String columnNames = "Version,";

				for (final String columnName : this.columnNamesInVersionsFile) {
					columnNames += columnName + ",";
				}

				columnNames = columnNames.substring(0, columnNames.length() - 1);
				columnNames += "\n";

				try (FileWriter writer = new FileWriter("input/" + metric.getProductName() + "-versionGraphData.csv")) {
					writer.write(columnNames);

					for (int i = 1; i < metric.getVersions().size(); i++) {
						final StringBuilder line = new StringBuilder(metric.getVersions().get(i - 1)).append("->").append(metric.getVersions().get(i));

						for (final List<Double> column : columns) {
							line.append(",").append(pmdDiff(column.get(i - 1), column.get(i)));
						}

						line.append("\n");
						writer.write(line.toString());
					}
				}
			}
		} catch (final IOException e1) {
			e1.printStackTrace();
		}
	}

	@Test
	public void createVersionGraphs() throws IOException {
		final ArrayList<String> projectNames = new ArrayList<>();
		final File[] versionGraphCSVs = new File("input").listFiles();

		// Get all versionGraph files
		for (final File file : versionGraphCSVs) {
			if (file.getName().contains("versionGraphData")) {
				projectNames.add(file.getName());
			}
		}

		for (final String projectName : projectNames) {
			final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			final File currentProject = new File("input/" + projectName);

			String line = "";
			final String cvsSplitBy = ",";

			try (BufferedReader br = new BufferedReader(new FileReader(currentProject))) {

				// ignore title
				br.readLine();
				while ((line = br.readLine()) != null) {

					// use comma as separator
					final String[] data = line.split(cvsSplitBy);

					for (int i = 1; i < this.columnNamesInVersionsFile.length; i++) {
						dataset.addValue(Double.parseDouble(data[i]), this.columnNamesInVersionsFile[i - 1], data[0]);
					}

				}

			} catch (final IOException e) {
				e.printStackTrace();
			}

			final String chartTitle = "Metric changes for project " + projectName.replace("-versionGraphData.csv", "");
			final String categoryAxisLabel = "Version";
			final String valueAxisLabel = "Metric value";

			final JFreeChart chart = ChartFactory.createLineChart(chartTitle, categoryAxisLabel, valueAxisLabel, dataset);

			// Styling
			chart.getPlot().setBackgroundPaint(Color.WHITE);
			chart.getPlot().setOutlineStroke(new BasicStroke(3.0f));
			final CategoryPlot plot = chart.getCategoryPlot();
			// plot.getRangeAxis().setRange(-100, 100);
			// Thicken the plot lines
			for (int i = 0; i < (this.columnNamesInVersionsFile.length - 1); i++) {
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(3.0f));
			}

			final CategoryAxis domainAxis = plot.getDomainAxis();
			domainAxis.setLowerMargin(0);
			domainAxis.setUpperMargin(0);
			final CategoryAxis xAxis = plot.getDomainAxis();
			xAxis.setLowerMargin(0);
			xAxis.setUpperMargin(0);
			xAxis.setMaximumCategoryLabelLines(3);

			// GENERATE SEVERAL SIZES
			final int width = 800; /* Width of the image */
			final int height = 600; /* Height of the image */

			final File lineChart = new File(
					"Resources/LineChart-" + projectName.replace(".csv", "") + "-" + width + "x" + height + ".jpeg");

			ChartUtils.saveChartAsJPEG(lineChart, chart, width, height);

		}
	}

}
