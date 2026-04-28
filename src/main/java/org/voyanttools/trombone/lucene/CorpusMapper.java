/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.queries.spans.TermSpans;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SparseFixedBitSet;
import org.voyanttools.trombone.lucene.search.DocumentFilter;
import org.voyanttools.trombone.lucene.search.DocumentFilterSpans;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class CorpusMapper {
	
	Storage storage;
	private DirectoryReader directoryReader;
	IndexSearcher searcher;
	Corpus corpus;
	private List<Integer> luceneIds = null;
	private BitSet bitSet = null;
	private Map<String, Integer> documentIdToLuceneIdMap = null;
	private Map<Integer, String> luceneIdToDocumentIdMap = null;

	public CorpusMapper(Storage storage, Corpus corpus) throws IOException {
		this.storage = storage;
		this.corpus = corpus;
	}
	
	public Storage getStorage() {
		return storage;
	}

	public Corpus getCorpus() {
		return corpus;
	}


	private synchronized List<String> getCorpusDocumentIds() {
		return corpus.getDocumentIds();
	}
	
	public synchronized List<Integer> getLuceneIds() throws IOException {
		if (luceneIds==null) {
			build();
		}
		return luceneIds;
	}
	
	public BitSet getBitSet() throws IOException {
		if (bitSet==null) {build();}
		return bitSet;
	}
	
	public IndexReader getLeafReader() throws IOException {
		if (directoryReader==null) {
			build();
		}
		return directoryReader;
	}
	
	public IndexSearcher getSearcher() throws IOException {
		if (searcher==null) {
			// ensure directoryReader is built
			if (directoryReader==null) {
				build();
			}
			searcher = new IndexSearcher(directoryReader);
		}
		return searcher;
	}

	public int getDocumentPositionFromLuceneId(int doc) throws IOException {
		String id = getDocumentIdFromLuceneId(doc);
		return corpus.getDocumentPosition(id);
	}


	public int getLuceneIdFromDocumentId(String id) throws IOException {
		if (documentIdToLuceneIdMap==null) {
			build();
		}
		return documentIdToLuceneIdMap.get(id);
	}

	public String getDocumentIdFromLuceneId(int doc) throws IOException {
		if (luceneIdToDocumentIdMap==null) {
			build();
		}
		return luceneIdToDocumentIdMap.get(doc);
	}
	
	public int getLuceneIdFromDocumentPosition(int doc) throws IOException {
		return getLuceneIdFromDocumentId(getDocumentIdFromDocumentPosition(doc));
	}

	private void build() throws IOException {
		luceneIdToDocumentIdMap =  new HashMap<Integer, String>();
		documentIdToLuceneIdMap = new HashMap<String, Integer>();
		luceneIds = new ArrayList<Integer>();
		buildFromTermsEnum();
	}
	
	/**
	 * This should not be called, except from the private build() method.
	 * Iterates over all leaf segments to support multi-segment indexes.
	 * @throws IOException
	 */
	private void buildFromTermsEnum() throws IOException {
		directoryReader = storage.getLuceneManager().getDirectoryReader(corpus.getId());
		
		int maxDoc = directoryReader.maxDoc();
		Set<String> ids = new HashSet<String>(getCorpusDocumentIds());
		bitSet = new SparseFixedBitSet(maxDoc);
		
		for (LeafReaderContext leafContext : directoryReader.leaves()) {
			LeafReader leafReader = leafContext.reader();
			int docBase = leafContext.docBase;
			
			Terms terms = leafReader.terms("id");
			if (terms == null) continue;
			
			TermsEnum termsEnum = terms.iterator();
			BytesRef bytesRef = termsEnum.next();
			int doc;
			String id;
			while (bytesRef!=null) {
				PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.NONE);
				doc = postingsEnum.nextDoc();
				if (doc!=PostingsEnum.NO_MORE_DOCS) {
					id = bytesRef.utf8ToString();
					int globalDoc = docBase + doc;
					if (ids.contains(id)) {
						bitSet.set(globalDoc);
						luceneIds.add(globalDoc);
						documentIdToLuceneIdMap.put(id, globalDoc);
						luceneIdToDocumentIdMap.put(globalDoc, id);
					}
				}
				bytesRef = termsEnum.next();
			}
		}
	}
	
	public String getDocumentIdFromDocumentPosition(int documentPosition) {
		return getCorpusDocumentIds().get(documentPosition);
	}

	public boolean hasLuceneId(int doc) throws IOException {
		if (bitSet==null) {
			build();
		}
		return bitSet.get(doc);
	}

	/**
	 * Get a Spans that filters for this corpus.
	 * Iterates over all leaf segments to support multi-segment indexes.
	 * @param spanQuery
	 * @return
	 * @throws IOException
	 */
	public Spans getFilteredSpans(SpanQuery spanQuery) throws IOException {
		return getFilteredSpans(spanQuery, getBitSet());
	}
	
	/**
	 * Get a Spans that filters for the specified BitSet.
	 * Iterates over all leaf segments to support multi-segment indexes.
	 * @param spanQuery
	 * @param bitSet
	 * @return
	 * @throws IOException
	 */
	public Spans getFilteredSpans(SpanQuery spanQuery, BitSet bitSet) throws IOException {
		SpanWeight weight = (SpanWeight) spanQuery.createWeight(getSearcher(), ScoreMode.COMPLETE_NO_SCORES, 1f);
		List<Spans> filteredLeafSpans = new ArrayList<Spans>();
		for (LeafReaderContext leafContext : directoryReader.leaves()) {
			Spans spans = weight.getSpans(leafContext, SpanWeight.Postings.POSITIONS);
			if (spans != null) {
				filteredLeafSpans.add(new DocumentFilterSpans(spans, bitSet, leafContext.docBase));
			}
		}
		if (filteredLeafSpans.isEmpty()) return null;
		if (filteredLeafSpans.size() == 1) return filteredLeafSpans.get(0);
		return new MultiLeafSpans(filteredLeafSpans);
	}
	
//	public Filter getFilter() throws IOException {
//		return new DocumentFilter(this);
//	}
//	
//	public Query getFilteredQuery(Query query) throws IOException {
//		BooleanQuery.Builder builder = new BooleanQuery.Builder();
//		builder.add(query, BooleanClause.Occur.MUST);
//		builder.add(getFilter(), BooleanClause.Occur.FILTER);
//		return builder.build();
//	}

	public BitSet getBitSetFromDocumentIds(Collection<String> documentIds) throws IOException {
		// ensure directoryReader is built
		if (directoryReader==null) {build();}
		BitSet subBitSet = new SparseFixedBitSet(directoryReader.maxDoc());
		for (String id : documentIds) {
			subBitSet.set(getLuceneIdFromDocumentId(id));
		}
		return subBitSet;
	}
	
	public DocIdSet getDocIdSet() throws IOException {
		return new BitDocIdSet(getBitSet());
	}
}
