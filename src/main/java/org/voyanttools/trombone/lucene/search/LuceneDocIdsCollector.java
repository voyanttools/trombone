/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.MatchesIterator;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BitSet;
import org.voyanttools.trombone.lucene.CorpusMapper;

/**
 * @author sgs
 *
 */
public class LuceneDocIdsCollector implements Collector {

	private Map<Integer, Integer> luceneDocIds = new HashMap<Integer, Integer>();
	private int rawFreq = 0;
	private BitSet bitSet;
	private Weight weight;

	/**
	 * Create a collector that tracks matching document IDs and per-document term frequencies.
	 * @param corpusMapper the corpus mapper for bitSet-based document filtering
	 * @param weight the Weight for the query, used to compute per-document match frequency
	 *               via the Matches API (since Scorer.freq() was removed in Lucene 9)
	 * @throws IOException
	 */
	public LuceneDocIdsCollector(CorpusMapper corpusMapper, Weight weight) throws IOException {
		bitSet = corpusMapper.getBitSet();
		this.weight = weight;
	}

	@Override
	public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
		final int base = context.docBase;
		final LeafReaderContext leafContext = context;
		return new LeafCollector() {
			@Override
			public void setScorer(Scorable scorer) throws IOException {
				// no-op; we use Weight.matches() for frequency instead
			}

			@Override
			public void collect(int doc) throws IOException {
				int absoluteDoc = base + doc;
				if (bitSet.get(absoluteDoc) && !luceneDocIds.containsKey(absoluteDoc)) {
					int freq = 1; // default to at least 1 since we matched
					if (weight != null) {
						Matches matches = weight.matches(leafContext, doc);
						if (matches != null) {
							freq = 0;
							for (String field : matches) {
								MatchesIterator mi = matches.getMatches(field);
								while (mi.next()) {
									freq++;
								}
							}
							if (freq == 0) freq = 1;
						}
					}
					rawFreq += freq;
					luceneDocIds.put(absoluteDoc, freq);
				}
			}
		};
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE;
	}

	public int getRawFreq() {
		return rawFreq;
	}

	public int getInDocumentsCount() {
		return luceneDocIds.size();
	}

	public Map<Integer, Integer> getLuceneDocIds() {
		return luceneDocIds;
	}

	private boolean isSeen(int doc) {
		return luceneDocIds.containsKey(doc);
	}
}
