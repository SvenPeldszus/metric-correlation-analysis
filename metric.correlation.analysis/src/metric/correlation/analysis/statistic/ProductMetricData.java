package metric.correlation.analysis.statistic;

import java.util.ArrayList;
import java.util.List;

public class ProductMetricData {

	private final String productName;
	private final String vendor;

	private final List<Double> locpcs;
	private final List<Double> bugs;
	private final List<Double> secBugs;
	private final List<String> versions;
	private final List<Double> igams;
	private final List<Double> bugsKloc;
	private final List<Double> ldcs;
	private final List<Double> wmcs;
	private final List<Double> dits;
	private final List<Double> lcom5s;
	private final List<Double> cbos;
	private final List<Double> igats;
	private final List<Double> blobAntiPatterns;
	private final List<Double> avgTime;
	private final List<Double> llocs;

	public ProductMetricData(final String productName, final String vendor, final List<Double> locpcs,
			final List<String> versions, final List<Double> igams, final List<Double> ldcs, final List<Double> wmcs,
			final List<Double> dits, final List<Double> lcom5s, final List<Double> cbos, final List<Double> igats,
			final List<Double> blobAntiPatterns, final List<Double> llocs) {
		this.productName = productName;
		this.vendor = vendor;

		this.avgTime = new ArrayList<>();
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
		this.bugs = new ArrayList<>();
		this.secBugs = new ArrayList<>();
		this.bugsKloc = new ArrayList<>();
	}

	public ProductMetricData(final String productName, final String vendor) {
		this.productName = productName;
		this.vendor = vendor;

		this.avgTime = new ArrayList<>();
		this.locpcs = new ArrayList<>();
		this.bugs = new ArrayList<>();
		this.bugsKloc = new ArrayList<>();
		this.secBugs = new ArrayList<>();
		this.versions = new ArrayList<>();
		this.igams = new ArrayList<>();
		this.ldcs = new ArrayList<>();
		this.wmcs = new ArrayList<>();
		this.dits = new ArrayList<>();
		this.lcom5s = new ArrayList<>();
		this.cbos = new ArrayList<>();
		this.igats = new ArrayList<>();
		this.blobAntiPatterns = new ArrayList<>();
		this.llocs = new ArrayList<>();
	}

	public List<String> getVersions() {
		return versions;
	}

	public String getProductName() {
		return this.productName;
	}

	public void addLocpc(final Double value) {
		this.getLOCpCS().add(value);
	}

	public List<Double> getLOCpCS() {
		return locpcs;
	}

	public void addVersion(final String value) {
		this.getVersions().add(value);
	}

	public void addBugKLOC(final Double value) {
		this.getBugsKLOC().add(value);
	}

	public List<Double> getBugsKLOC() {
		return bugsKloc;
	}

	public void addBug(final Double value) {
		this.getBugs().add(value);
	}

	public List<Double> getBugs() {
		return bugs;
	}

	public void addLDC(final Double value) {
		this.getLDCs().add(value);
	}

	public List<Double> getLDCs() {
		return ldcs;
	}

	public void addWMC(final Double value) {
		this.getWMCs().add(value);
	}

	public List<Double> getWMCs() {
		return wmcs;
	}

	public void addDIT(final Double value) {
		this.getDITs().add(value);
	}

	public List<Double> getDITs() {
		return dits;
	}

	public void addLCOM5(final Double value) {
		this.getLCOM5s().add(value);
	}

	public List<Double> getLCOM5s() {
		return lcom5s;
	}

	public void addCBO(final Double value) {
		this.getCBOs().add(value);
	}

	public List<Double> getCBOs() {
		return cbos;
	}

	public void addLLOC(final Double value) {
		this.getLLOCs().add(value);
	}

	public List<Double> getLLOCs() {
		return llocs;
	}

}
