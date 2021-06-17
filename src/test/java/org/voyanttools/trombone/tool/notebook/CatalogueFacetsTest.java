package org.voyanttools.trombone.tool.notebook;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
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
		
		index(storage);
		test(storage);
		
		storage.destroy();
	}

	private void index(FileStorage storage) throws IOException {
		File notebookFile = TestHelper.getResource("html/notebook.html");
		File notebookStorage = new File(storage.storageLocation, Storage.Location.notebook.toString());
		FileUtils.copyFileToDirectory(notebookFile, notebookStorage);
		
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.setParameter("action", "reindex");
		GitNotebookManager gnm = new GitNotebookManager(storage, parameters);
		gnm.run();
	}
	
	private void test(Storage storage) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters();
		parameters.setParameter("query", "facet.author=Andrew");
		CatalogueFacets cf = new CatalogueFacets(storage, parameters);
		cf.run();
		Map<String, LabelAndValue[]> facetResults = cf.getFacetResults();
		
		for (Entry<String, LabelAndValue[]> facetResult : facetResults.entrySet()) {
			if (facetResult.getKey().equals("facet.author")) {
				assertEquals("Andrew", facetResult.getValue()[0].label);
			}
		}
	}

}
