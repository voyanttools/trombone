package org.voyanttools.trombone.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleQueue;
import org.voyanttools.trombone.util.TestHelper;

public class CorpusTermTest {

	private FlexibleQueue<CorpusTerm> queue;
	private CorpusTerm c1;
	private CorpusTerm c2;
	private CorpusTerm c3;
	private CorpusTerm c4;
	
	@Test
	public void test() throws IOException {
		int totalTokens = 1000;
		
		// String termString, int rawFreq, int totalTokens, int inDocumentsCount, int totalDocuments, int[] rawFreqs, float[] relativeFreqs, int bins
		c1 = new CorpusTerm("one", 1, totalTokens, 1, 2, null, null, 10);
		c2 = new CorpusTerm("two", 2, totalTokens, 1, 2, null, null, 10);
		c3 = new CorpusTerm("three", 3, totalTokens, 1, 2, null, null, 10);
		c4 = new CorpusTerm("four", 4, totalTokens, 1, 2, null, null, 10);
		
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			test(storage);
		}
	}
	
	public void test(Storage storage) throws IOException {
		List<CorpusTerm> list = getOrderedList(CorpusTerm.Sort.RAWFREQASC);
		assertEquals(list.get(0).getTerm(), "one");
		
		list = getOrderedList(CorpusTerm.Sort.RAWFREQDESC);
		assertEquals(list.get(0).getTerm(), "four");
		
		list = getOrderedList(CorpusTerm.Sort.TERMASC);
		assertEquals(list.get(0).getTerm(), "four");
		
		list = getOrderedList(CorpusTerm.Sort.TERMDESC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(CorpusTerm.Sort.RELATIVEPEAKEDNESSASC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(CorpusTerm.Sort.RELATIVEPEAKEDNESSDESC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(CorpusTerm.Sort.RELATIVESKEWNESSASC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(CorpusTerm.Sort.RELATIVESKEWNESSDESC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(CorpusTerm.Sort.INDOCUMENTSCOUNTASC);
		assertEquals(list.get(0).getTerm(), "four");
		
		list = getOrderedList(CorpusTerm.Sort.INDOCUMENTSCOUNTDESC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(CorpusTerm.Sort.COMPARISONRELATIVEFREQDIFFERENCEASC);
		assertEquals(list.get(0).getTerm(), "two");
		
		list = getOrderedList(CorpusTerm.Sort.COMPARISONRELATIVEFREQDIFFERENCEDESC);
		assertEquals(list.get(0).getTerm(), "two");
	}

	private List<CorpusTerm> getOrderedList(CorpusTerm.Sort sorting) {
		queue = new FlexibleQueue<CorpusTerm>(CorpusTerm.getComparator(sorting), 4);
		queue.offer(c1);
		queue.offer(c2);
		queue.offer(c3);
		queue.offer(c4);
		return queue.getOrderedList();
	}
}
