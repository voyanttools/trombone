/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.lucene.search.LuceneDocIdsCollector;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.model.IndexedDocument.IndexedDocumentPriorityQueue;
import org.voyanttools.trombone.model.IndexedDocument.Sort;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TextUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("documentsMetadata")
public class DocumentsMetadata extends AbstractCorpusTool {
	
	private int total = 0;
	
	private List<IndexedDocument> documents = new ArrayList<IndexedDocument>();

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentsMetadata(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		
		Sort sort = IndexedDocument.Sort.getForgivingly(parameters);
		Corpus corpus = corpusMapper.getCorpus();
		List<String> ids = new ArrayList<String>(); // maintain insertion order, especially for no queries
		String[] queries = getQueries();
		
		Set<String> validIds = new HashSet<String>(getCorpusStoredDocumentIdsFromParameters(corpus));
		
		if (queries.length>0) {
			Map<String, Float> weights = new HashMap<String, Float>();
			weights.put("title", 1f);
			weights.put("author", 1f);
			weights.put(TokenType.lexical.name(), 1f);
			IndexSearcher indexSearcher = corpusMapper.getSearcher();
			SimpleQueryParser queryParser = new FieldPrefixAwareSimpleQueryParser(indexSearcher.getIndexReader(), storage.getLuceneManager().getAnalyzer(corpusMapper.getCorpus().getId()), weights);
			
			Set<String> idsSet = new HashSet<String>();
			for (String queryString : queries) {
				Query query = queryParser.parse(queryString);
				LuceneDocIdsCollector collector = new LuceneDocIdsCollector(corpusMapper);
				indexSearcher.search(query, collector);
				for (int doc : collector.getLuceneDocIds()) {
					idsSet.add(corpusMapper.getDocumentIdFromLuceneId(doc));
				}
			}
			
			if (sort==Sort.INDEXASC) {
				// insert them in order
				for (String id : corpus.getDocumentIds()) {
					if (idsSet.contains(id) && validIds.contains(id)) {ids.add(id);}
				}
			} else {
				for (String id : idsSet) {
					if (validIds.contains(id)) {
						ids.add(id);
					}
				}
				ids.addAll(idsSet);
			}
		}
		else {
			ids.addAll(validIds);
		}
		total = ids.size();
		
		int start = parameters.getParameterIntValue("start", 0);
		int limit = parameters.getParameterIntValue("limit", Integer.MAX_VALUE);
		
		int size = start+limit;
		IndexedDocumentPriorityQueue queue = new IndexedDocument.IndexedDocumentPriorityQueue(size > corpus.size() ? size : corpus.size(), sort);
		int index = 0;
		for (String id : ids) {
			IndexedDocument document = corpus.getDocument(id);
			DocumentMetadata metadata = document.getMetadata();
			metadata.setIndex(corpus.getDocumentPosition(id)); // make sure index is set
			
			// check to see if we have an older document without sentences count
			if (metadata.getTokensCount(TokenType.lexical)>0 && metadata.getSentencesCount()==0) {
				String text = document.getDocumentString();
				String language = metadata.getLanguageCode();
				int count = TextUtils.getSentences(text, language).size();
				if (count>0) {
					metadata.setSentencesCount(count);
					// store for next time
					storage.getStoredDocumentSourceStorage().updateStoredDocumentSourceMetadata(document.getId(), metadata);
				}
			}
			queue.offer(document);
			if (++index>=size && sort==Sort.INDEXASC) {break;} // we don't need to look any further since docs in order
		}
		
		for (int i=0, len = queue.size()-start; i<len; i++) {
			documents.add(queue.poll());
		}
		Collections.reverse(documents);

	}
	
	/**
	 * Get the documents after a run (should only be used by unit testing).
	 * 
	 * @return
	 */
	List<IndexedDocument> getDocuments() {
		return documents;
	}
	
	@Override
	public float getVersion() {
		return super.getVersion()+10;
	}


}
