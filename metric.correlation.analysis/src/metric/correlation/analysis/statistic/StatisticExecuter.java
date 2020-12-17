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
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.Test;

import metric.correlation.analysis.calculation.impl.IssueMetrics;
//import metric.correlation.analysis.calculation.impl.HulkMetrics;
import metric.correlation.analysis.calculation.impl.SourceMeterMetrics;
import metric.correlation.analysis.calculation.impl.VersionMetrics;

public class StatisticExecuter {

	private static final String INPUT_SERIES = "results";
	private static final File DATA_FILE = new File(new File("results", INPUT_SERIES), "results.csv");

	private static final Logger LOGGER = Logger.getLogger(StatisticExecuter.class);

	private static final File RESULTS = new File("statistics");

	public static void main(String[] args) {
		StatisticExecuter executer = new StatisticExecuter();
		try {
			executer.calculateStatistics(DATA_FILE, new File(RESULTS, INPUT_SERIES));
		} catch (IOException e) {
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
	public void calculateStatistics(File in, File out) throws IOException {
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
	public void calculateStatistics(final LinkedHashMap<String, List<Double>> map, File out) throws IOException {
		final Set<String> keySet = map.keySet();
		final ArrayList<String> metricNames = new ArrayList<>(keySet);

		RealMatrix matrix = createMatrix(map);

//		RealMatrix pearsonMatrix = new PearsonsCorrelation().computeCorrelationMatrix(matrix);
//		CorreltationMatrixPrinter.storeMatrix(pearsonMatrix, metricNames, new File(out, "PearsonCorrelationMatrix.csv"));

		RealMatrix spearmanMatrix = new SpearmansCorrelation().computeCorrelationMatrix(matrix);
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

			String xMetric = metricNames.get(i);
			List<Double> xValues = map.get(xMetric);

			for (int j = i + 1; j < keySet.size(); j++) {
				String yMetric = metricNames.get(j);
				List<Double> yValues = map.get(yMetric);

				XYSeriesCollection scatterPlotResult = new XYSeriesCollection();
				XYSeries ySeries = new XYSeries(yMetric);

				for (int counter = 0; counter < xValues.size(); counter++) {
					ySeries.add(xValues.get(counter), yValues.get(counter));
				}

				scatterPlotResult.addSeries(ySeries);
				JFreeChart chart = ChartFactory.createScatterPlot((xMetric + " vs " + yMetric), xMetric, yMetric,
						scatterPlotResult);
				BufferedImage chartImage = chart.createBufferedImage(600, 400);
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
	private LinkedHashMap<String, List<Double>> getMetricMap(File dataFile) throws IOException {
		List<String> lines = Files.readAllLines(dataFile.toPath());
		String[] keys = lines.get(0).split(",");
		LinkedHashMap<String, List<Double>> metrics = new LinkedHashMap<>(keys.length - 1);
		Set<Integer> skipIndex = new HashSet<>();
		for (int i = 0; i < keys.length; i++) {
			String value = keys[i];
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
		for (String line : lines.subList(1, lines.size())) {
			String[] values = line.split(",");

			boolean valid = true;
			for (int i = 0; i < values.length; i++) {
				if (skipIndex.contains(i)) {
					continue;
				}
				String s = values[i];
				if (s == null || "null".equals(s) || "NaN".equals(s)) {
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

	public RealMatrix createMatrix(Map<String, List<Double>> metricValues) {
		double[][] results = new double[metricValues.size()][];
		int col = 0;
		for (List<Double> s : metricValues.values()) {
			double[] d = new double[s.size()];
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

	private ArrayList<ProductMetricData> getProductMetricData(File dataFile) throws IOException {
		List<String> lines = Files.readAllLines(dataFile.toPath());
		List<String> keys = Arrays.asList(lines.get(0).split(","));

		// if one of these wont be found (return -1), it would crash later when we would
		// subscribt a list with -1 index
		int idx_version = keys.indexOf(VersionMetrics.MetricKeysImpl.VERSION.toString());
		int idx_vendor = keys.indexOf(VersionMetrics.MetricKeysImpl.VENDOR.toString());
		int idx_product = keys.indexOf(VersionMetrics.MetricKeysImpl.PRODUCT.toString());
		int idx_bugsKloc = keys.indexOf(IssueMetrics.MetricKeysImpl.BUG_ISSUES_KLOC_TIME.toString());
		int idx_bugs = keys.indexOf(IssueMetrics.MetricKeysImpl.BUG_ISSUES_TIME.toString());
		int idx_avgTime = keys.indexOf(IssueMetrics.MetricKeysImpl.AVG_OPEN_TIME_DAYS.toString());
		int idx_locpc = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.LOC_PER_CLASS.toString());
		int idx_ldc = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.LDC.toString());
		int idx_wmc = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.WMC.toString());
		int idx_dit = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.DIT.toString());
		int idx_lcom5 = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.LCOM.toString());
		int idx_cbo = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.CBO.toString());
		int idx_lloc = keys.indexOf(SourceMeterMetrics.MetricKeysImpl.LLOC.toString());
		lines.remove(0); // remove column keys row

		ArrayList<String> productNames = new ArrayList<String>();
		ArrayList<String> vendors = new ArrayList<String>();

		ArrayList<Double> locpcs = new ArrayList<Double>();
		ArrayList<String> versions = new ArrayList<String>();
		 ArrayList<Double> bugs = new ArrayList<Double>();
		ArrayList<Double> ldcs = new ArrayList<Double>();
		ArrayList<Double> wmcs = new ArrayList<Double>();
		ArrayList<Double> dits = new ArrayList<Double>();
		ArrayList<Double> lcom5s = new ArrayList<Double>();
		ArrayList<Double> cbos = new ArrayList<Double>();
		 ArrayList<Double> bugsKloc = new ArrayList<Double>();
		ArrayList<Double> llocs = new ArrayList<Double>();

		for (String line : lines) {
			String[] split = line.split(",");
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

		ArrayList<ProductMetricData> metrics = new ArrayList<ProductMetricData>();
		ProductMetricData metric = new ProductMetricData();
		metric.productName = productNames.get(0);
		metric.vendor = vendors.get(0);
		metrics.add(metric);
		for (int i = 0; i < lines.size(); i++) {
			String product = productNames.get(i);
			if (!product.equals(metric.productName)) {
				metric = new ProductMetricData();
				metrics.add(metric);
				metric.productName = product;
				metric.vendor = vendors.get(i);
			}
			metric.locpcs.add(locpcs.get(i));
			metric.versions.add(versions.get(i));
			metric.bugsKloc.add(bugsKloc.get(i));
			metric.bugs.add(bugs.get(i));
			metric.ldcs.add(ldcs.get(i));
			metric.wmcs.add(wmcs.get(i));
			metric.dits.add(dits.get(i));
			metric.lcom5s.add(lcom5s.get(i));
			metric.cbos.add(cbos.get(i));
			metric.llocs.add(llocs.get(i));
		}
		return metrics;
	}

	private Double pmdDiff(Double previous, Double next) {
		return Precision.round((((next - previous) / previous) * 100), 2);
	}

	String[] columnNamesInVersionsFile = { "LOCpC", "BUG_ISSUES_KLOC_TIME","BUG_ISSUES_TIME", "LDC", "WMC", "DIT", "LCCM3", "CBO", "LLOC" };

	//@Test
	public void writeVersionsCSVFile() {
		try {
			for (ProductMetricData metric : getProductMetricData(new File("input/versions-results.csv"))) {

				ArrayList<ArrayList<Double>> columns = new ArrayList<ArrayList<Double>>();
				columns.add(metric.locpcs);
				 columns.add(metric.bugsKloc);
				 columns.add(metric.bugs);
				columns.add(metric.ldcs);
				columns.add(metric.wmcs);
				columns.add(metric.dits);
				columns.add(metric.lcom5s);
				columns.add(metric.cbos);
				// columns.add(metric.avgTime);
				columns.add(metric.llocs);

				String columnNames = "Version,";

				for (String columnName : columnNamesInVersionsFile) {
					columnNames += columnName + ",";
				}

				columnNames = columnNames.substring(0, columnNames.length() - 1);
				columnNames += "\n";

				FileWriter writer = new FileWriter("input/" + metric.productName + "-versionGraphData.csv");
				writer.write(columnNames);

				for (int i = 1; i < metric.versions.size(); i++) {
					String line = metric.versions.get(i - 1) + "->" + metric.versions.get(i);

					for (ArrayList<Double> column : columns) {
						line = line + "," + pmdDiff(column.get(i - 1), column.get(i));
					}

					line += "\n";
					writer.write(line);
				}
				writer.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	 @Test
	public void createVersionGraphs() {
		ArrayList<String> projectNames = new ArrayList<>();
		File[] versionGraphCSVs = new File("input").listFiles();

		// Get all versionGraph files
		for (File file : versionGraphCSVs) {
			if (file.getName().contains("versionGraphData")) {
				projectNames.add(file.getName());
			}
		}

		for (String projectName : projectNames) {
			DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			File currentProject = new File("input/" + projectName);

			String line = "";
			String cvsSplitBy = ",";

			try (BufferedReader br = new BufferedReader(new FileReader(currentProject))) {

				// ignore title
				br.readLine();
				while ((line = br.readLine()) != null) {

					// use comma as separator
					String[] data = line.split(cvsSplitBy);

					for (int i = 1; i < columnNamesInVersionsFile.length; i++) {
						dataset.addValue(Double.parseDouble(data[i]), columnNamesInVersionsFile[i - 1], data[0]);
					}

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			String chartTitle = "Metric changes for project " + projectName.replace("-versionGraphData.csv", "");
			String categoryAxisLabel = "Version";
			String valueAxisLabel = "Metric value";

			JFreeChart chart = ChartFactory.createLineChart(chartTitle, categoryAxisLabel, valueAxisLabel, dataset);

			// Styling
			chart.getPlot().setBackgroundPaint(Color.WHITE);
			chart.getPlot().setOutlineStroke(new BasicStroke(3.0f));
			CategoryPlot plot = chart.getCategoryPlot();
			//plot.getRangeAxis().setRange(-100, 100);
			// Thicken the plot lines
			for (int i = 0; i < columnNamesInVersionsFile.length - 1; i++) {
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(3.0f));
			}
			
			CategoryAxis domainAxis = plot.getDomainAxis();
			domainAxis.setLowerMargin(0);
			domainAxis.setUpperMargin(0);
			CategoryAxis xAxis = plot.getDomainAxis();
			xAxis.setLowerMargin(0);
			xAxis.setUpperMargin(0);
			xAxis.setMaximumCategoryLabelLines(3);

			// GENERATE SEVERAL SIZES
//		    int width = 1280;    /* Width of the image */
//		    int height = 720;   /* Height of the image */ 
			int width = 800; /* Width of the image */
			int height = 600; /* Height of the image */

			File lineChart = new File(
					"Resources/LineChart-" + projectName.replace(".csv", "") + "-" + width + "x" + height + ".jpeg");

			try {
				ChartUtils.saveChartAsJPEG(lineChart, chart, width, height);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}
