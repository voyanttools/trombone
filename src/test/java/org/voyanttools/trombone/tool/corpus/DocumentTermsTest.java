package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.junit.Test;
import org.voyanttools.trombone.lucene.LuceneManager;
import org.voyanttools.trombone.model.DocumentTerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class DocumentTermsTest {

	@Test
	public void test() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}

	public void test(Storage storage) throws IOException {
		
		// add an additional document to the corpus
		Document document = new Document();
		document.add(new TextField("lexical", "dark and stormy night in document one", Field.Store.YES));
		storage.getLuceneManager().addDocument(RandomStringUtils.randomAlphabetic(10), document);
		
		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "It was a dark and stormy night.");
		parameters.addParameter("string", "It was the best of times it was the worst of times.");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");
		parameters.addParameter("noCache", 1);

		RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		
		parameters.setParameter("tool", "DocumentTermFrequencies");
		
		DocumentTerms documentTermsTool = new DocumentTerms(storage, parameters);
		documentTermsTool.run();
		List<DocumentTerm> documentTerms = documentTermsTool.getDocumentTerms();
		assertEquals(14, documentTerms.size());
		
		parameters.setParameter("docIndex", 0);
		documentTermsTool = new DocumentTerms(storage, parameters);
		documentTermsTool.run();
		documentTerms = documentTermsTool.getDocumentTerms();
		assertEquals(7, documentTerms.size());
		
		parameters.setParameter("query", "it");
		documentTermsTool = new DocumentTerms(storage, parameters);
		documentTermsTool.run();
		documentTerms = documentTermsTool.getDocumentTerms();
		assertEquals(1, documentTerms.size());
		parameters.setParameter("query", "it");
		
		parameters.removeParameter("docIndex");
		documentTermsTool = new DocumentTerms(storage, parameters);
		documentTermsTool.run();
		documentTerms = documentTermsTool.getDocumentTerms();
		assertEquals(2, documentTerms.size());
		
		storage.destroy();
	}
	
	@Test
	public void test2() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test2(storage);
		}
	}

	
	public void test2(Storage storage) throws IOException {

		Document document;
		LuceneManager luceneManager = storage.getLuceneManager();
		document = new Document();
		document.add(new TextField("lexical", "dark and stormy night in document one", Field.Store.YES));
		luceneManager.addDocument(RandomStringUtils.randomAlphabetic(10), document);
		DocumentTerm documentTerm;
		
		FlexibleParameters parameters;
		
		parameters = new FlexibleParameters();
		parameters.addParameter("string",  "It was a dark and stormy night.");
		parameters.addParameter("string", "It was the best of times it was the worst of times.");
		parameters.addParameter("tool", "StepEnabledIndexedCorpusCreator");

		CorpusCreator creator = new CorpusCreator(storage, parameters);
		creator.run();
		parameters.setParameter("corpus", creator.getStoredId());
		
		parameters.setParameter("tool", "DocumentTermFrequencies");
		
		DocumentTerms documentTermFrequencies;
		List<DocumentTerm> documentTerms;
		
		parameters.setParameter("query", "dar*");
		documentTermFrequencies = new DocumentTerms(storage, parameters);
		documentTermFrequencies.run();
		documentTerms = documentTermFrequencies.getDocumentTerms();
		assertEquals(1, documentTerms.size());
		documentTerm = documentTerms.get(0);
		assertEquals("dar*", documentTerm.getTerm());
		assertEquals(1, documentTerm.getRawFrequency());
		assertEquals(0, documentTerm.getDocumentIndex());
		
		parameters.setParameter("query", "it was");
		documentTermFrequencies = new DocumentTerms(storage, parameters);
		documentTermFrequencies.run();
		// we sort by reverse frequency by default
		documentTerms = documentTermFrequencies.getDocumentTerms();
		assertEquals(2, documentTerms.size());
		documentTerm = documentTerms.get(0);
		assertEquals(1, documentTerm.getDocumentIndex());
		assertEquals("\"it was\"", documentTerm.getTerm());
		assertEquals(2, documentTerm.getRawFrequency());
		assertEquals(0.585539, documentTerm.getZscore(), 0.0001);
		documentTerm = documentTerms.get(1);
		assertEquals(0, documentTerm.getDocumentIndex());
		assertEquals("\"it was\"", documentTerm.getTerm());
		assertEquals(1, documentTerm.getRawFrequency());
		
		parameters.removeParameter("query");
		documentTermFrequencies = new DocumentTerms(storage, parameters);
		documentTermFrequencies.run();
		documentTerms = documentTermFrequencies.getDocumentTerms();
		assertEquals(14, documentTerms.size());
		documentTerm = documentTerms.get(0);
		assertEquals("was", documentTerm.getTerm());
		assertEquals(2, documentTerm.getRawFrequency());
		assertEquals(0.585539, documentTerm.getZscore(), 0.0001);
		
		parameters.setParameter("limit", 1);
		documentTermFrequencies = new DocumentTerms(storage, parameters);
		documentTermFrequencies.run();
		documentTerms = documentTermFrequencies.getDocumentTerms();
		assertEquals(1, documentTerms.size());
		documentTerm = documentTerms.get(0);
		assertEquals("was", documentTerm.getTerm());
		assertEquals(2, documentTerm.getRawFrequency());
		
		parameters.setParameter("start", 1);
		documentTermFrequencies = new DocumentTerms(storage, parameters);
		documentTermFrequencies.run();
		documentTerms = documentTermFrequencies.getDocumentTerms();
		assertEquals(1, documentTerms.size());
		documentTerm = documentTerms.get(0);
		assertEquals("times", documentTerm.getTerm());
		assertEquals(2, documentTerm.getRawFrequency());
		
		parameters.setParameter("start", 50);
		documentTermFrequencies = new DocumentTerms(storage, parameters);
		documentTermFrequencies.run();
		documentTerms = documentTermFrequencies.getDocumentTerms();
		assertEquals(0, documentTerms.size());
		
		// with stopwords
		parameters.setParameter("stopList", "stop.en.txt");
		parameters.removeParameter("start");
		parameters.removeParameter("limit");
		documentTermFrequencies = new DocumentTerms(storage, parameters);
		documentTermFrequencies.run();
		documentTerms = documentTermFrequencies.getDocumentTerms();
		assertEquals(6, documentTerms.size());
		documentTerm = documentTerms.get(0);
		assertEquals("times", documentTerm.getTerm());
		documentTerm = documentTerms.get(documentTerms.size()-1);
		assertEquals("best", documentTerm.getTerm());
		
		storage.destroy();
		
	}

}
