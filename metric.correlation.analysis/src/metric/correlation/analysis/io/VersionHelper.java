package metric.correlation.analysis.io;

import javax.annotation.Nonnull;

public class VersionHelper {

	private static final String numbersOrDots = "[^\\d|\\.]";
	/**
	 * Compares one version string to another version string by dotted ordinals.
	 * eg. "1.0" > "0.09" ; "0.9.5" < "0.10",
	 * also "1.0" < "1.0.0" but "1.0" == "01.00"
	 *
	 * @param left  the left hand version string
	 * @param right the right hand version string
	 * @return 0 if equal, -1 if thisVersion &lt; comparedVersion and 1 otherwise.
	 */
	public static int compare(@Nonnull String left, @Nonnull String right) {
		left = left.replaceAll(numbersOrDots, "");
		while (left.startsWith(".")) {
			left = left.substring(1);
		}
		right = right.replaceAll(numbersOrDots, "");
		while (right.startsWith(".")) {
			right = right.substring(1);
		}
		if (left.equals(right)) {
			return 0;
		}
		int leftStart = 0;
		int rightStart = 0;
		int result;
		do {
			final int leftEnd = left.indexOf('.', leftStart);
			final int rightEnd = right.indexOf('.', rightStart);
			final Integer leftValue = Integer.parseInt(leftEnd < 0
					? left.substring(leftStart)
							: left.substring(leftStart, leftEnd));
			final Integer rightValue = Integer.parseInt(rightEnd < 0
					? right.substring(rightStart)
							: right.substring(rightStart, rightEnd));
			result = leftValue.compareTo(rightValue);
			leftStart = leftEnd + 1;
			rightStart = rightEnd + 1;
		} while ((result == 0) && (leftStart > 0) && (rightStart > 0));
		if (result == 0) {
			if (leftStart > rightStart) {
				return containsNonZeroValue(left, leftStart) ? 1 : 0;
			}
			if (leftStart < rightStart) {
				return containsNonZeroValue(right, rightStart) ? -1 : 0;
			}
		}
		return result;
	}

	private static boolean containsNonZeroValue(final String str, final int beginIndex) {
		for (int i = beginIndex; i < str.length(); i++) {
			final char c = str.charAt(i);
			if ((c != '0') && (c != '.')) {
				return true;
			}
		}
		return false;
	}
}