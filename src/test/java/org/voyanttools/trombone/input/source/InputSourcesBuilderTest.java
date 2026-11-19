package org.voyanttools.trombone.input.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.voyanttools.trombone.util.TestHelper;

public class InputSourcesBuilderTest {

	@Test
	public void testBlocksPathTraversalPayloads() throws Exception {
		Path tempRoot = TestHelper.getTemporaryTestStorageDirectory().toPath();

		Files.createDirectory(tempRoot);
		Path baseDir = Files.createDirectory(tempRoot.resolve("uploads"));
		Path siblingDir = Files.createDirectory(tempRoot.resolve("uploads_evil"));
		Path outsideSecret = Files.writeString(tempRoot.resolve("secret.txt"), "top-secret");
		Files.writeString(siblingDir.resolve("evil.txt"), "sibling-secret");

		String[] maliciousInputs = {
			"../secret.txt",
			"../../secret.txt",
			"..\\secret.txt",
			"subdir/../../secret.txt",
			"../uploads_evil/evil.txt",
			outsideSecret.toAbsolutePath().toString() // Absolute path escape
		};

		for (String payload : maliciousInputs) {
			assertThrows(
				"Failed to block payload: " + payload,
				SecurityException.class,
				() -> InputSourcesBuilder.resolveSafeFile(baseDir, payload)
				
			);
		}
	}

	@Test
	public void testAllowsValidFileInsideBaseDir() throws Exception {
		Path tempRoot = TestHelper.getTemporaryTestStorageDirectory().toPath();

		Files.createDirectory(tempRoot);
		Path baseDir = Files.createDirectory(tempRoot.resolve("uploads"));
		Path validFile = Files.writeString(baseDir.resolve("document.txt"), "hello");

		File resolved = InputSourcesBuilder.resolveSafeFile(baseDir, "document.txt");

		assertEquals(validFile.toRealPath().toFile(), resolved.getCanonicalFile());
	}

}
