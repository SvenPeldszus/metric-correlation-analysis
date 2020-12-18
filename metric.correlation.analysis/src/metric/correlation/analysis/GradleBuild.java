package metric.correlation.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gravity.eclipse.os.OperationSystem;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

public class GradleBuild {

	private static final Logger LOGGER = Logger.getLogger(GradleBuild.class);

	private GradleBuild() {
		// This class should not be instantiated
	}

	/**
	 * @param src - The downloaded apk src
	 * @return the apk for this application
	 * @throws UnsupportedOperationSystemException when not on Windows/Linux
	 */
	public static File buildApk(final File src) throws UnsupportedOperationSystemException {

		if (new File(src, "build").exists()) {
			LOGGER.warn("Build already exists!");
			return getApk(src);
		}

		final String cmd = "cd " + src.getPath() + " && gradlew assembleDebug";
		final Runtime run = Runtime.getRuntime();
		Process process;

		try {
			switch (OperationSystem.getCurrentOS()) {

			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
				break;

			case LINUX:
				process = run.exec("./gradlew assembleDebug", null, src);
				break;

			default:
				throw new UnsupportedOperationSystemException();
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;

				while ((line = reader.readLine()) != null) {
					LOGGER.log(Level.INFO, "> " + line); //$NON-NLS-1$
				}

				process.waitFor();
				process.destroy();
			}

		} catch (final InterruptedException e) {
			LOGGER.warn(e.getMessage(), e);
			Thread.currentThread().interrupt();
		} catch (final IOException e) {
			LOGGER.warn(e.getMessage(), e);
		}
		return getApk(src);
	}

	/**
	 * Searches for the build apk file
	 *
	 * @param src the directory of the application
	 * @return a compiled apk file
	 */
	private static File getApk(final File src) {
		final File[] list = src.listFiles();

		if (list == null) {
			throw new IllegalArgumentException("Directory is empty!");
		}

		for (final File file : list) {

			if (file.isDirectory()) {
				return getApk(file);

			} else {
				final File[] apklist = file.getParentFile().listFiles((FilenameFilter) (dir, name) -> name.endsWith(".apk"));

				if (apklist.length > 0) {
					return apklist[0];
				}
			}
		}
		return null;
	}

	/**
	 * UnsupportedOperationSystemException
	 *
	 * @param src the src to clean
	 * @return true if it worked, else false
	 * @throws UnsupportedOperationSystemException
	 */
	public static boolean cleanBuild(final File src) throws UnsupportedOperationSystemException {
		final String cmd = "cd " + src.getPath() + " && gradlew clean";
		final Runtime run = Runtime.getRuntime();

		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
				break;
			case LINUX:
				process = run.exec("./gradlew clean", null, src);
				break;
			default:
				throw new UnsupportedOperationSystemException();
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					LOGGER.log(Level.INFO, "> " + line); //$NON-NLS-1$
				}

				process.waitFor();
				process.destroy();
			}
			return true;

		} catch (final IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		} catch (final InterruptedException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			Thread.currentThread().interrupt();
		}

		return false;
	}

}
