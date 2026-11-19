package org.voyanttools.trombone.tool.corpus;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusFacetsTest {

	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}
	
	public void test(Storage storage) throws IOException {
		FlexibleParameters parameters;
		if (storage instanceof FileStorage) {
			File file = TestHelper.copyFileToTemporaryTestStorageDirectory((FileStorage)storage, TestHelper.getResource("xml/rss.xml").getAbsolutePath());
			parameters = new FlexibleParameters(new String[]{"file="+file});
		} else {
			parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("xml/rss.xml")});
		}
		parameters.setParameter("inputFormat", "RSS");
		parameters.setParameter("splitDocuments", "true");
		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		parameters.clear();
		parameters.setParameter("corpus", creator.getStoredId());
		
		CorpusFacets corpusFacets;
		parameters.setParameter("facet", "facet.author");
		corpusFacets = new CorpusFacets(storage, parameters);
		corpusFacets.run();

		parameters.setParameter("query", "Joe");
		corpusFacets = new CorpusFacets(storage, parameters);
		corpusFacets.run();
		
		storage.destroy();

	}

}
