package org.voyanttools.trombone.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.CorpusManager;
import org.voyanttools.trombone.tool.corpus.CorpusTerms;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class CategoriesTest {

	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			testCategories(storage);
		}
	}
	
	private void testCategories(Storage storage) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource("udhr/udhr-en.txt")});
		Corpus corpus = CorpusManager.getCorpus(storage, parameters);
	
		Categories categories;
		
		CorpusTerms corpusTerms;

		categories = Categories.getCategories(storage, corpus, "");
		assertTrue(categories.isEmpty());

		categories = Categories.getCategories(storage, corpus, "auto");
		assertTrue(categories.hasCategory("positive"));
		
		categories = Categories.getCategories(storage, corpus, "en");
		assertTrue(categories.hasCategory("positive"));
		
		categories = Categories.getCategories(storage, corpus, "categories.h_posneg");
		assertTrue(categories.hasCategory("Positiv"));
		
		categories = Categories.getCategories(storage, corpus, "categories.en");
		assertTrue(categories.hasCategory("positive"));
		
		parameters.setParameter("categories", "auto");
		parameters.setParameter("query", "@positive");
		corpusTerms = new CorpusTerms(storage, parameters);
		corpusTerms.run();
		assertEquals(1, corpusTerms.getTotal());
		for (CorpusTerm term : corpusTerms) {
			assertEquals("@positive", term.getTerm());
			assertEquals(62, term.getRawFrequency());
		}
		
		parameters.setParameter("categories", "auto");
		parameters.setParameter("query", "^@positive");
		corpusTerms = new CorpusTerms(storage, parameters);
		corpusTerms.run();
		assertEquals(18, corpusTerms.getTotal());
		for (CorpusTerm term : corpusTerms) {
			assertEquals("right", term.getTerm());
			assertEquals(27, term.getRawFrequency());
			break;
		}
		
		String id = storage.storeString("{\"categories\":{\"human\":[\".*man\"]}}", Storage.Location.object);
		categories = Categories.getCategories(storage, corpus, id);
		assertTrue(categories.hasCategory("human"));
		
		parameters.setParameter("categories", id);
		parameters.setParameter("query", "@human");
		corpusTerms = new CorpusTerms(storage, parameters);
		corpusTerms.run();
		assertEquals(1, corpusTerms.getTotal());
		for (CorpusTerm term : corpusTerms) {
			assertEquals(5, term.getRawFrequency());
		}
		
	}

}
