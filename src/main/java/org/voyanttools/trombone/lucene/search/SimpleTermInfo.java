package org.voyanttools.trombone.lucene.search;

/**
 * Simple replacement for FieldTermStack.TermInfo whose constructor became
 * package-private in Lucene 9. Provides the same essential data-holder
 * interface used by AbstractContextTerms and DocumentTokens.
 */
public class SimpleTermInfo implements Comparable<SimpleTermInfo> {

	private final String text;
	private final int startOffset;
	private final int endOffset;
	private final int position;

	public SimpleTermInfo(String text, int startOffset, int endOffset, int position) {
		this.text = text;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.position = position;
	}

	public String getText() {
		return text;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public int getPosition() {
		return position;
	}

	@Override
	public int compareTo(SimpleTermInfo other) {
		return Integer.compare(this.position, other.position);
	}

	@Override
	public String toString() {
		return text + " (" + startOffset + "," + endOffset + "," + position + ")";
	}
}
