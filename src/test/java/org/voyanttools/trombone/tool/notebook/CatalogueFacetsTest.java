package org.voyanttools.trombone.tool.notebook;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.facet.LabelAndValue;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;


public class CatalogueFacetsTest {

	@Test
	public void test() throws IOException {
		FileStorage storage = new FileStorage(TestHelper.getTemporaryTestStorageDirectory());
		
		GitNotebookManagerTest.reindex(storage);
		test(storage);
		
		storage.destroy();
	}
	
	private void test(Storage storage) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.setParameter("query", "facet.author=Andrew");
		CatalogueFacets cf = new CatalogueFacets(storage, parameters);
		cf.run();
		Map<String, LabelAndValue[]> facetResults = cf.getFacetResults();
		
		for (Entry<String, LabelAndValue[]> facetResult : facetResults.entrySet()) {
			if (facetResult.getKey().equals("facet.author")) {
				LabelAndValue[] lav = facetResult.getValue();
				assertEquals("Andrew", lav[0].label);
			}
		}
	}

}
