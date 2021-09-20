/**
 * 
 */
package ca.lincsproject.nssi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NssiResult implements Iterable<NssiResult>, Iterator<NssiResult>, Serializable {

	
	private static final long serialVersionUID = -4012523062653951171L;
	
	private List<String> entities;
	private List<String> lemmas;
	private List<String> classifications;
	private List<Integer> starts;
	private List<Integer> ends;
	private int counter;
	private Map<Integer, Integer> startsToIndexMap;
	
	private boolean error = false;
	
	
	public NssiResult() {
		entities = new ArrayList<String>();
		lemmas = new ArrayList<String>();
		classifications = new ArrayList<String>();
		starts = new ArrayList<Integer>();
		ends = new ArrayList<Integer>();
		startsToIndexMap = new HashMap<Integer, Integer>();
	}
	
	public NssiResult(boolean error) {
		this();
		this.error = error;
	}
	
	public void add(String entity, String classification, String lemma, int start, int end) {
		counter = entities.size();
		startsToIndexMap.put(start, counter);
		entities.add(entity);
		classifications.add(classification);
		lemmas.add(lemma);
		starts.add(start);
		ends.add(end);
	}

	@Override
	public Iterator<NssiResult> iterator() {
		counter = -1;
		return this;
	}

	@Override
	public boolean hasNext() {
		return counter+1<entities.size();
	}

	@Override
	public NssiResult next() {
		counter++;
		return this;
	}
	
	public String getCurrentEntity() {
		return counter>-1 && counter < entities.size() ? entities.get(counter) : null;
	}

	public String getCurrentLemma() {
		return counter>-1 && counter < lemmas.size() ? lemmas.get(counter) : null;
	}
	
	public String getCurrentClassification() {
		return counter>-1 && counter < classifications.size() ? classifications.get(counter) : null;
	}

	public int getCurrentStart() {
		return counter>-1 && counter < starts.size() ? starts.get(counter) : -1;
	}

	public int getCurrentEnd() {
		return counter>-1 && counter < ends.size() ? ends.get(counter) : -1;
	}

	public void setCurrentByStart(int start) {
		counter = startsToIndexMap.containsKey(start) ? startsToIndexMap.get(start) : -1;
	}

	public void setCurrentOffset(int correctedStart, int correctedEnd) {
		startsToIndexMap.put(correctedStart, counter);
		starts.set(counter, correctedStart);
		ends.set(counter, correctedEnd);
	}
	
	public boolean hasError() {
		return error;
	}
}
