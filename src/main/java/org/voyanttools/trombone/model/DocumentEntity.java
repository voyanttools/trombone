/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.Serializable;

/**
 * @author sgs
 *
 */
public class DocumentEntity implements Serializable, Comparable<DocumentEntity> {

	// TODO add lemma?
	private int docIndex;
	private String term;
	private String normalized; // normalized form of numerical named entities, e.g. dates
	private EntityType type;
	private int rawFreq;
	private int[][] positions; // term position
	private float[] confidences;
	private int[][] offsets; // start and end character offsets

	public DocumentEntity(int docIndex, String term, String normalized, EntityType type, int rawFreq, int[][] positions, float[] confidences) {
		this.docIndex = docIndex;
		this.term = term;
		this.normalized = normalized;
		this.type = type;
		this.rawFreq = rawFreq;
		this.positions = positions;
		this.confidences = confidences;
		this.offsets = null;
	}
	
	public DocumentEntity(int docIndex, String term, String normalized, EntityType type, int rawFreq, int[][] positions) {
		this(docIndex, term, normalized, type, rawFreq, positions, null);
	}
	
	public DocumentEntity(int docIndex, String term, String normalized, EntityType type, int rawFreq) {
		this(docIndex, term, normalized, type, rawFreq, null, null);
	}
	
	public int getDocIndex() {
		return docIndex;
	}
	
	public void setDocIndex(int docIndex) {
		this.docIndex = docIndex;
	}
	
	public String getTerm() {
		return term;
	}
	
	public void setTerm(String term) {
		this.term = term;
	}

	public String getNormalized() {
		return normalized;
	}

	public void setNormalized(String normalized) {
		this.normalized = normalized;
	}

	@Override
	public int compareTo(DocumentEntity o) {
		return term.compareTo(o.term);
	}

	public int getRawFreq() {
		return rawFreq;
	}

	public EntityType getType() {
		return type;
	}
	
	public int[][] getPositions() {
		return positions;
	}
	
	public void setPositions(int[][] positions) {
		this.positions = positions;
	}
	
	public float[] getConfidences() {
		return confidences;
	}

	public int[][] getOffsets() {
		return offsets;
	}

	public void setOffsets(int[][] offsets) {
		this.offsets = offsets;
	}

}
