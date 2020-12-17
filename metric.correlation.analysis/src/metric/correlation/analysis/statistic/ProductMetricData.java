package metric.correlation.analysis.statistic;

import java.util.ArrayList;

public class ProductMetricData {

	String productName;
	String vendor;

	ArrayList<Double> locpcs = new ArrayList<Double>();
	ArrayList<Double> bugs = new ArrayList<Double>();
	ArrayList<Double> secBugs = new ArrayList<Double>();
	ArrayList<String> versions = new ArrayList<String>();
	ArrayList<Double> igams = new ArrayList<Double>();
	ArrayList<Double> bugsKloc = new ArrayList<Double>();
	ArrayList<Double> ldcs = new ArrayList<Double>();
	ArrayList<Double> wmcs = new ArrayList<Double>();
	ArrayList<Double> dits = new ArrayList<Double>();
	ArrayList<Double> lcom5s = new ArrayList<Double>();
	ArrayList<Double> cbos = new ArrayList<Double>();
	ArrayList<Double> igats = new ArrayList<Double>();
	ArrayList<Double> blobAntiPatterns = new ArrayList<Double>();
	ArrayList<Double> avgTime = new ArrayList<Double>();
	ArrayList<Double> llocs = new ArrayList<Double>();

	public ProductMetricData(String productName, String vendor, ArrayList<Double> locpcs, ArrayList<String> versions,
			ArrayList<Double> igams, ArrayList<Double> ldcs, ArrayList<Double> wmcs, ArrayList<Double> dits,
			ArrayList<Double> lcom5s, ArrayList<Double> cbos, ArrayList<Double> igats,
			ArrayList<Double> blobAntiPatterns, ArrayList<Double> llocs) {
		this.productName = productName;
		this.vendor = vendor;
		this.locpcs = locpcs;
		this.versions = versions;
		this.igams = igams;
		this.ldcs = ldcs;
		this.wmcs = wmcs;
		this.dits = dits;
		this.lcom5s = lcom5s;
		this.cbos = cbos;
		this.igats = igats;
		this.blobAntiPatterns = blobAntiPatterns;
		this.llocs = llocs;
	}
	
	public ProductMetricData() {
		
	}

}
