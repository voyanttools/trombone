package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusVectorsTest {
	
	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {
		//  add the testing file to the storage
		FlexibleParameters parameters;
		if (storage instanceof FileStorage) {
			File file = TestHelper.copyFileToTemporaryTestStorageDirectory((FileStorage)storage, TestHelper.getResource("udhr/udhr-en.txt").getAbsolutePath());
			parameters = new FlexibleParameters(new String[]{"file="+file});
		} else {
			parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr/udhr-en.txt")});
		}
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		String id = creator.getStoredId();
		
		parameters.clear();
		parameters.setParameter("corpus", id);
		CorpusVectors corpusVectors = new CorpusVectors(storage, parameters);
		corpusVectors.run();

		// TODO no actual test performed
	}

}
