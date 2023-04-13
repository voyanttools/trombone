package org.voyanttools.trombone.model;

import java.text.Normalizer;
import java.util.Comparator;

import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;


@XStreamAlias("collocate")
@XStreamConverter(CorpusCollocate.CorpusCollocateConverter.class)
public class CorpusCollocate implements Comparable<CorpusCollocate> {
	
	private String term; // the term whose collocates we're looking for
	
	private int rawFreq;
	
	private String contextTerm; // the collocate term
	
	private int contextTermRawFreq;
	
	private transient String normalizedContextTerm = null;

	private transient String normalizedKeyword = null;

	public enum Sort {
		RAWFREQASC, RAWFREQDESC, TERMASC, TERMDESC, CONTEXTTERMASC, CONTEXTTERMDESC, CONTEXTTERMRAWFREQASC, CONTEXTTERMRAWFREQDESC;

		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "CONTEXTTERMRAWFREQ"; // default
			if (sort.equals("TERM")) {sortPrefix = "TERM";}
			else if (sort.equals("CONTEXTTERM")) {sortPrefix = "CONTEXTTERM";}
			else if (sort.equals("RAWFREQ")) {sortPrefix = "RAWFREQ";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			return valueOf(sortPrefix+dirSuffix);
		}
	}
	
	public CorpusCollocate(String keyword, int keywordRawFrequency, String contextTerm, int contextTermRawFrequency) {
		this.term = keyword;
		this.rawFreq = keywordRawFrequency;
		this.contextTerm = contextTerm;
		this.contextTermRawFreq = contextTermRawFrequency;
	}
	
	private String getNormalizedContextTerm() {
		if (normalizedContextTerm==null) {normalizedContextTerm = Normalizer.normalize(contextTerm, Normalizer.Form.NFD);}
		return normalizedContextTerm;
	}
	
	public String getContextTerm() {
		return contextTerm;
	}
	
	public int getContextTermRawFrequency() {
		return contextTermRawFreq;
	}
	
	private String getNormalizedKeyword() {
		if (normalizedKeyword==null) {normalizedKeyword = Normalizer.normalize(term, Normalizer.Form.NFD);}
		return normalizedKeyword;
	}

	public static Comparator<CorpusCollocate> getComparator(Sort sort) {
		switch (sort) {
		case RAWFREQASC:
			return RawFrequencyAscendingComparator;
		case RAWFREQDESC:
			return RawFrequencyDescendingComparator;
		case TERMASC:
			return TermAscendingComparator;
		case TERMDESC:
			return TermDescendingComparator;
		case CONTEXTTERMASC:
			return ContextTermAscendingComparator;
		case CONTEXTTERMDESC:
			return ContextTermDescendingComparator;
		case CONTEXTTERMRAWFREQASC:
			return ContextTermRawFrequencyAscendingComparator;
		default:
			return ContextTermRawFrequencyDescendingComparator;
		}
	}

	private static Comparator<CorpusCollocate> RawFrequencyAscendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.rawFreq==corpusCollocate2.rawFreq ? 
				corpusCollocate1.compareTo(corpusCollocate2) :
				Integer.compare(corpusCollocate1.rawFreq, corpusCollocate2.rawFreq);
		}
	};

	private static Comparator<CorpusCollocate> RawFrequencyDescendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.rawFreq==corpusCollocate2.rawFreq ? 
				corpusCollocate2.compareTo(corpusCollocate1) :
				Integer.compare(corpusCollocate2.rawFreq, corpusCollocate1.rawFreq);
		}
	};

	private static Comparator<CorpusCollocate> ContextTermRawFrequencyAscendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.contextTermRawFreq==corpusCollocate2.contextTermRawFreq ?
				corpusCollocate1.compareTo(corpusCollocate2) :
				Integer.compare(corpusCollocate1.contextTermRawFreq, corpusCollocate2.contextTermRawFreq);
		}
	};

	private static Comparator<CorpusCollocate> ContextTermRawFrequencyDescendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.contextTermRawFreq==corpusCollocate2.contextTermRawFreq ?
				corpusCollocate2.compareTo(corpusCollocate1) :
				Integer.compare(corpusCollocate2.contextTermRawFreq, corpusCollocate1.contextTermRawFreq);
		}
	};

	private static Comparator<CorpusCollocate> ContextTermAscendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.contextTerm.equals(corpusCollocate2.contextTerm) ? 
					corpusCollocate1.compareTo(corpusCollocate2) : 
					corpusCollocate1.getNormalizedContextTerm().compareTo(corpusCollocate2.getNormalizedContextTerm());
		}
	};
	
	private static Comparator<CorpusCollocate> ContextTermDescendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.contextTerm.equals(corpusCollocate2.contextTerm) ? 
					corpusCollocate2.compareTo(corpusCollocate1) : 
					corpusCollocate2.getNormalizedContextTerm().compareTo(corpusCollocate1.getNormalizedContextTerm());
		}
	};
	
	private static Comparator<CorpusCollocate> TermAscendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.term.equals(corpusCollocate2.term) ? 
					corpusCollocate1.compareTo(corpusCollocate2) : 
					corpusCollocate1.getNormalizedKeyword().compareTo(corpusCollocate2.getNormalizedKeyword());
		}
	};
	
	private static Comparator<CorpusCollocate> TermDescendingComparator =  new Comparator<CorpusCollocate>() {
		@Override
		public int compare(CorpusCollocate corpusCollocate1, CorpusCollocate corpusCollocate2) {
			return corpusCollocate1.term.equals(corpusCollocate2.term) ? 
					corpusCollocate2.compareTo(corpusCollocate1) : 
					corpusCollocate2.getNormalizedKeyword().compareTo(corpusCollocate1.getNormalizedKeyword());
		}
	};

	@Override
	public int compareTo(CorpusCollocate o) {
		if (rawFreq!=o.rawFreq) {
			return Integer.compare(rawFreq, o.rawFreq);
		}
		
		if (!term.equals(o.term)) {
			return getNormalizedKeyword().compareTo(o.getNormalizedKeyword());
		}
		
		if (contextTermRawFreq!=o.contextTermRawFreq) {
			return Integer.compare(contextTermRawFreq, o.contextTermRawFreq);
		}

		if (!contextTerm.equals(o.contextTerm)) {
			return getNormalizedContextTerm().compareTo(o.getNormalizedContextTerm());
		}

		return Integer.compare(hashCode(), o.hashCode());
	}
	
	public String toString() {
		return new StringBuilder("{corpus collocate - context: ").append(contextTerm).append(" (").append(contextTermRawFreq).append("); keyword: ").append(term).append(" (").append(rawFreq).append(")}").toString();
	}

	public static class CorpusCollocateConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return CorpusCollocate.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CorpusCollocate cc = (CorpusCollocate) source;
			
			writer.startNode("corpusCollocate");
			
			writer.startNode("term");
			writer.setValue(cc.term);
			writer.endNode();
			
			ToolSerializer.setNumericNode(writer, "rawFreq", cc.rawFreq);
			
			writer.startNode("contextTerm");
			writer.setValue(cc.contextTerm);
			writer.endNode();
			
			ToolSerializer.setNumericNode(writer, "contextTermRawFreq", cc.contextTermRawFreq);
			
			writer.endNode();
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			return null;
		}
		
	}


}
