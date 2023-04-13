package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.model.CorpusCollocate;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusCollocatesTest {

	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {
		// add the testing files
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr/udhr-en.txt")});
		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		
		
		parameters.setParameter("corpus", creator.getStoredId());
		parameters.setParameter("query", "human");
		
		CorpusCollocates corpusCollocates;
		List<CorpusCollocate> corpusCollocatesList;
		
		parameters.removeParameter("limit"); // make sure no limit
		corpusCollocates = new CorpusCollocates(storage, parameters);
		corpusCollocates.run();
		corpusCollocatesList = corpusCollocates.getCorpusCollocates();
		assertEquals("should", corpusCollocatesList.get(0).getContextTerm());
		
		parameters.setParameter("limit", 10); // try with limit
		corpusCollocates = new CorpusCollocates(storage, parameters);
		corpusCollocates.run();
		corpusCollocatesList = corpusCollocates.getCorpusCollocates();
		assertEquals("should", corpusCollocatesList.get(0).getContextTerm());
		
		parameters.setParameter("start", 10);
		corpusCollocates = new CorpusCollocates(storage, parameters);
		corpusCollocates.run();
		assertEquals(10, corpusCollocates.getCorpusCollocates().size());
		
		int total = corpusCollocates.total;
		parameters.setParameter("start", total-5);
		corpusCollocates = new CorpusCollocates(storage, parameters);
		corpusCollocates.run();
		assertEquals(5, corpusCollocates.getCorpusCollocates().size());
		
		parameters.setParameter("start", 0);
		parameters.setParameter("sort", "contextTerm");
		parameters.setParameter("dir", "ASC");
		corpusCollocates = new CorpusCollocates(storage, parameters);
		corpusCollocates.run();
		corpusCollocatesList = corpusCollocates.getCorpusCollocates();
		assertEquals("allow", corpusCollocatesList.get(1).getContextTerm());
		
	}

}
