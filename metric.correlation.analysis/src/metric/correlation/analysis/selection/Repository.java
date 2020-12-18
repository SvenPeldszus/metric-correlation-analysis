package metric.correlation.analysis.selection;

/**
 * @author Antoniya Ivanova Represents a GitHub repository.
 *
 */

public class Repository {

	private final String vendor;
	private final String product;
	private final int stars;
	private final int openIssues;

	/**
	 * Build a new GitHub repository representation with
	 *
	 * @param vendor     - the Vendor
	 * @param product    - the Repository/Product name
	 * @param stars      - the number of stars it has
	 * @param openIssues - number of open issues
	 */
	public Repository(final String vendor, final String product, final int stars, final int openIssues) {
		this.vendor = vendor;
		this.product = product;
		this.stars = stars;
		this.openIssues = openIssues;
	}

	public String getVendor() {
		return this.vendor;
	}

	public String getProduct() {
		return this.product;
	}

	public int getStars() {
		return this.stars;
	}

	public int getOpenIssues() {
		return this.openIssues;
	}

	@Override
	public int hashCode() {
		return this.vendor.hashCode() ^ this.product.hashCode() ^ (this.stars);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Repository)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		return getVendor().equals(((Repository) obj).getVendor())
				&& getProduct().equals(((Repository) obj).getProduct())
				&& (getStars() == ((Repository) obj).getStars());
	}

	@Override
	public String toString() {
		return this.vendor + " " + this.product + " " + this.stars;
	}

}
