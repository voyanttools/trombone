package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentLocationToken;
import org.voyanttools.trombone.nlp.GeonamesAnnotator;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class GeonamesTest {

	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {

		String text1 = "Most of London, Ontario and Montreal and most of London, England and Montreal and London and Montreal.";
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"string="+text1,"includeCities=true"});
		Geonames geonamesTool = new Geonames(storage, parameters);
		geonamesTool.run();
		assertEquals(4, geonamesTool.citiesCountList.size());
		assertEquals(3, (int) geonamesTool.citiesCountList.get(0).getValue());
		assertEquals(1, (int) geonamesTool.citiesCountList.get(2).getValue());
		assertEquals(4, geonamesTool.connectionsCount.size());
		assertEquals("6077243-2643743", geonamesTool.connectionsCount.get(0).getKey());
		assertEquals(2, geonamesTool.connectionsCount.get(0).getValue().get());
		assertEquals("6058560-6077243", geonamesTool.connectionsCount.get(2).getKey());
		assertEquals(1, geonamesTool.connectionsCount.get(2).getValue().get());
		assertEquals(6, geonamesTool.connectionOccurrences.size());
		storage.destroy();
		
	}
	
	@Test
	public void testGeonamesAnnotator() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			testGeonamesAnnotator(storage);
		}
	}
	
	public void testGeonamesAnnotator(Storage storage) throws IOException {
		String text1 = "Most of London, Ontario and Montreal and most of London, England and Montreal and London and Montreal, London.";
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"string="+text1,"includeCities=true"});
//		CorpusCreator creator = new CorpusCreator(storage, parameters);
//		creator.run();
		Corpus corpus = CorpusManager.getCorpus(storage, parameters);
		CorpusMapper corpusMapper = new CorpusMapper(storage, corpus);
		GeonamesAnnotator annotator = new GeonamesAnnotator(storage, parameters);
		List<DocumentLocationToken> tokens = annotator.getDocumentLocationTokens(corpusMapper, parameters);
		assertEquals(7, tokens.size());
		assertEquals("London, Ontario", tokens.get(0).getLocation().getPlaces().get(0));
		assertTrue(tokens.get(2).getLocation().getPlaces().get(0).startsWith("London, United"));
		storage.destroy();
		
	}

}
