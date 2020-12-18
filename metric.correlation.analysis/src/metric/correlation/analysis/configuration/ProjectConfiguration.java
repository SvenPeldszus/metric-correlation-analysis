package metric.correlation.analysis.configuration;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A data class describing a relevant project
 */
public class ProjectConfiguration {

	private final String productName;
	private final String vendorName;
	private final String gitUrl;
	private final Map<String, String> versionGitCommitIdMapping;

	/**
	 * Constructs a new configuration
	 *
	 * @param productName               The name of the product
	 * @param vendorName                The vendor of the product
	 * @param gitUrl                    The git url of the product
	 * @param versionGitCommitIdMapping A mapping from project versions to commit
	 *                                  ids
	 */
	public ProjectConfiguration(final String productName, final String vendorName, final String gitUrl,
			final Map<String, String> versionGitCommitIdMapping) {
		this.productName = productName;
		this.vendorName = vendorName;
		this.gitUrl = gitUrl;
		this.versionGitCommitIdMapping = versionGitCommitIdMapping;
	}

	/**
	 * A getter for the name of the product
	 *
	 * @return The product name
	 */
	public String getProductName() {
		return this.productName;
	}

	/**
	 * A getter for the name of the product vendor
	 *
	 * @return The vendor name
	 */
	public String getVendorName() {
		return this.vendorName;
	}

	/**
	 * A getter for the url of the git repository
	 *
	 * @return The git url
	 */
	public String getGitUrl() {
		return this.gitUrl;
	}

	/**
	 * Returns a collection of all relevant commit IDs for this product
	 *
	 * @return The commit ids
	 */
	public Collection<String> getGitCommitIds() {
		return this.versionGitCommitIdMapping.values();
	}

	/**
	 * Returns a set of all relevant versions of this product
	 *
	 * @return the product versions
	 */
	public Set<String> getProjectVersions() {
		return this.versionGitCommitIdMapping.keySet();
	}

	/**
	 * Returns the commit ID of a product version
	 *
	 * @param version The product version
	 * @return The commit ID or null if this configuration contains not the
	 *         requested version
	 */
	public String getCommitId(final String version) {
		return this.versionGitCommitIdMapping.get(version);
	}

	/**
	 * Returns all pairs of product version and git commit stored in this
	 * configuration as entries (@see java.util.Map.Entry) with the version as key
	 * and the commit ID as value.
	 *
	 * @return A set of the pairs as entries @see java.util.Map.Entry
	 */
	public Set<Entry<String, String>> getVersionCommitIdPairs() {
		return this.versionGitCommitIdMapping.entrySet();
	}
}
