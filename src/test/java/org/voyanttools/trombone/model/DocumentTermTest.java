package org.voyanttools.trombone.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleQueue;
import org.voyanttools.trombone.util.TestHelper;

public class DocumentTermTest {

	private FlexibleQueue<DocumentTerm> queue;
	private DocumentTerm d1;
	private DocumentTerm d2;
	private DocumentTerm d3;
	private DocumentTerm d4;
	
	@Test
	public void test() throws IOException {
		int docIndex = 0;
		String docId = "foo";
		int totalTokens = 1000;
		
		d1 = new DocumentTerm(docIndex, docId, "one", 1, totalTokens, 0, null, null, new CorpusTermMinimal("one", 1, 1, 1, 0));
		d2 = new DocumentTerm(docIndex, docId, "two", 2, totalTokens, 0, null, null, new CorpusTermMinimal("two", 2, 1, 1, 0));
		d3 = new DocumentTerm(docIndex, docId, "three", 3, totalTokens, 0, null, null, new CorpusTermMinimal("three", 3, 1, 1, 0));
		d4 = new DocumentTerm(docIndex, docId, "four", 4, totalTokens, 0, null, null, new CorpusTermMinimal("four", 4, 1, 1, 0));
		
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}
	
	public void test(Storage storage) throws IOException {
		List<DocumentTerm> list = getOrderedList(DocumentTerm.Sort.RAWFREQASC);
		
		assertEquals(list.get(3).getRelativeFrequency(), 0.004f, 0.0f);
		
		assertEquals(list.get(0).getTerm(), "one");
		
		list = getOrderedList(DocumentTerm.Sort.RAWFREQDESC);
		assertEquals(list.get(0).getTerm(), "four");
		
		list = getOrderedList(DocumentTerm.Sort.TERMASC);
		assertEquals(list.get(0).getTerm(), "four");
		
		list = getOrderedList(DocumentTerm.Sort.TERMDESC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(DocumentTerm.Sort.RELATIVEFREQASC);
		assertEquals(list.get(0).getTerm(), "one");
		
		list = getOrderedList(DocumentTerm.Sort.RELATIVEFREQDESC);
		assertEquals(list.get(0).getTerm(), "four");
		
		list = getOrderedList(DocumentTerm.Sort.TFIDFASC);
		assertEquals(list.get(0).getTerm(), "four");
		
		list = getOrderedList(DocumentTerm.Sort.TFIDFDESC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(DocumentTerm.Sort.ZSCOREASC);
		assertEquals(list.get(0).getTerm(), "four");
		
		list = getOrderedList(DocumentTerm.Sort.ZSCOREDESC);
		assertEquals(list.get(0).getTerm(), "two");

		storage.destroy();
	}
	
	private List<DocumentTerm> getOrderedList(DocumentTerm.Sort sorting) {
		queue = new FlexibleQueue<DocumentTerm>(DocumentTerm.getComparator(sorting), 4);
		queue.offer(d1);
		queue.offer(d2);
		queue.offer(d3);
		queue.offer(d4);
		return queue.getOrderedList();
	}

}
