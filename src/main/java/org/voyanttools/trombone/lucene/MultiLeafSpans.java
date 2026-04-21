package org.voyanttools.trombone.lucene;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;

/**
 * A {@link Spans} implementation that combines spans from multiple leaf segments
 * into a single iteration in global doc ID order.
 * 
 * Leaf spans are iterated sequentially since leaf segments are ordered by docBase.
 */
public class MultiLeafSpans extends Spans {

	private final List<Spans> leafSpans;
	private int currentIndex = -1;
	private Spans currentSpans = null;
	private int currentDoc = -1;

	public MultiLeafSpans(List<Spans> leafSpans) {
		this.leafSpans = leafSpans;
		if (!leafSpans.isEmpty()) {
			currentIndex = 0;
			currentSpans = leafSpans.get(0);
		}
	}

	@Override
	public int nextDoc() throws IOException {
		while (currentSpans != null) {
			int doc = currentSpans.nextDoc();
			if (doc != NO_MORE_DOCS) {
				currentDoc = doc;
				return currentDoc;
			}
			// Move to next leaf
			currentIndex++;
			if (currentIndex < leafSpans.size()) {
				currentSpans = leafSpans.get(currentIndex);
			} else {
				currentSpans = null;
			}
		}
		currentDoc = NO_MORE_DOCS;
		return NO_MORE_DOCS;
	}

	@Override
	public int advance(int target) throws IOException {
		int doc = nextDoc();
		while (doc < target && doc != NO_MORE_DOCS) {
			doc = nextDoc();
		}
		return doc;
	}

	@Override
	public int docID() {
		return currentDoc;
	}

	@Override
	public int nextStartPosition() throws IOException {
		if (currentSpans == null) return NO_MORE_POSITIONS;
		return currentSpans.nextStartPosition();
	}

	@Override
	public int startPosition() {
		if (currentSpans == null) return -1;
		return currentSpans.startPosition();
	}

	@Override
	public int endPosition() {
		if (currentSpans == null) return -1;
		return currentSpans.endPosition();
	}

	@Override
	public int width() {
		if (currentSpans == null) return 0;
		return currentSpans.width();
	}

	@Override
	public void collect(SpanCollector collector) throws IOException {
		if (currentSpans != null) {
			currentSpans.collect(collector);
		}
	}

	@Override
	public float positionsCost() {
		float cost = 0;
		for (Spans spans : leafSpans) {
			cost = Math.max(cost, spans.positionsCost());
		}
		return cost;
	}

	@Override
	public long cost() {
		long cost = 0;
		for (Spans spans : leafSpans) {
			cost += spans.cost();
		}
		return cost;
	}
}
