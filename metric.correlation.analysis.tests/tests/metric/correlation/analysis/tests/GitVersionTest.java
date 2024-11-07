package metric.correlation.analysis.tests;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.gravity.eclipse.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class GitVersionTest {

	private static final File PATH = new File("repositories/junit4");
	private static final String URL = "https://github.com/junit-team/junit4.git";

	@Before
	public void cloneRepo() {
		if (PATH.exists()) {
			FileUtils.recursiveDelete(PATH);
		}
		try {
			Git.cloneRepository().setURI(URL).setDirectory(PATH).call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testChange() throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		if (!PATH.exists()) {
			// Skip test
			return;
		}
		String id = "b51fa17fc6a750a17436f9f38c139a7b5228171f";
		try (Git git = Git.open(PATH)) {
			git.reset().setMode(ResetType.HARD).setRef(id).call();
			assertFalse(git.status().call().hasUncommittedChanges());
		}

	}
}
