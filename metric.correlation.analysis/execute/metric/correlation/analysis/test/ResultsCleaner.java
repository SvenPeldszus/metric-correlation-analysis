package metric.correlation.analysis.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gravity.eclipse.io.FileUtils;

/**
 * Removes emty results
 * 
 * @author speldszus
 *
 */
public class ResultsCleaner {

	private static final Logger LOGGER = Logger.getLogger(ResultsCleaner.class);

	public static void main(String[] args) {
		LOGGER.log(Level.INFO, "Removing emty results");
		for (File result : new File("results").listFiles()){
			try {
				final File file = new File(result, "results.csv");
				if(!file.exists() || Files.readAllLines(file.toPath()).size() <= 1) {
					FileUtils.recursiveDelete(result);
					LOGGER.log(Level.INFO, "Deleted: "+result);
				}
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			}
		}
	}
}
