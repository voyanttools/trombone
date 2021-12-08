package org.voyanttools.trombone.model;

import java.util.Comparator;

import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamConverter(DocumentTermsCorrelation.DocumentTermsCorrelationConverter.class)
public class DocumentTermsCorrelation {

	private DocumentTerm source;
	private DocumentTerm target;
	private float correlation;
	private float significance;
	
	public enum Sort {CORRELATIONASC, CORRELATIONDESC, CORRELATIONABS, SIGNIFICANCEASC, SIGNIFICANCEDESC, SIGNIFICANCEABS;
		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "CORRELATION"; // default
			if (sort.startsWith("SIGNIFICANCE")) {sortPrefix = "SIGNIFICANCE";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "DESC";
			if (dir.endsWith("ASC")) {dirSuffix="ASC";}
			else if (dir.endsWith("ABS")) {dirSuffix="ABS";}
			return valueOf(sortPrefix+dirSuffix);
		}		
	}
	public DocumentTermsCorrelation(DocumentTerm source, DocumentTerm target, float correlation, float significance) {
		this.source = source;
		this.target = target;
		this.correlation = correlation;
		this.significance = significance;
	}
	
	public float getCorrelation() {
		return correlation;
	}
	
	public float getSignificance() {
		return significance;
	}
	
	public DocumentTerm[] getDocumentTerms() {
		return new DocumentTerm[]{source, target};
	}

	public static Comparator<DocumentTermsCorrelation> getComparator(DocumentTermsCorrelation.Sort sort) {
		switch (sort) {
		case CORRELATIONASC:
			return CorrelationAscending;
		case CORRELATIONABS:
			return CorrelationAbsolute;
		case CORRELATIONDESC:
			return CorrelationDescending;
		case SIGNIFICANCEASC:
			return SignificanceAscending;
		case SIGNIFICANCEABS:
			return SignificanceAbsolute;
		case SIGNIFICANCEDESC:
			return SignificanceDescending;
		default:
			return CorrelationDescending;
		}
	}
	private static Comparator<DocumentTermsCorrelation> TieBreaker = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Integer.compare(o2.source.getRawFrequency()+o2.target.getRawFrequency(), o1.source.getRawFrequency()+o1.target.getRawFrequency());
			if (compare!=0) {return compare;}
			compare = o1.source.getTerm().compareTo(o2.source.getTerm());
			if (compare!=0) {return compare;}
			return o1.target.getTerm().compareTo(o2.target.getTerm());
		}
	};
	
	public static Comparator<DocumentTermsCorrelation> CorrelationAscending = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(o1.getCorrelation(), o2.getCorrelation());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<DocumentTermsCorrelation> CorrelationDescending = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(o2.getCorrelation(), o1.getCorrelation());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<DocumentTermsCorrelation> CorrelationAbsolute = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(Math.abs(o2.getCorrelation()), Math.abs(o1.getCorrelation()));
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};
	
	public static Comparator<DocumentTermsCorrelation> SignificanceAscending = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(o1.getSignificance(), o2.getSignificance());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<DocumentTermsCorrelation> SignificanceDescending = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(o2.getSignificance(), o1.getSignificance());
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};

	public static Comparator<DocumentTermsCorrelation> SignificanceAbsolute = new Comparator<DocumentTermsCorrelation>() {
		@Override
		public int compare(DocumentTermsCorrelation o1, DocumentTermsCorrelation o2) {
			int compare = Float.compare(Math.abs(o2.getSignificance()), Math.abs(o1.getSignificance()));
			if (compare==0) {return TieBreaker.compare(o1, o2);}
			else {return compare;}
		}
	};
	
	public static class DocumentTermsCorrelationConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return DocumentTermsCorrelation.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			DocumentTermsCorrelation documentTermCorrelation = (DocumentTermsCorrelation) source;
			
			boolean withDistributions = Boolean.TRUE.equals(context.get("withDistributions"));
			boolean termsOnly = Boolean.TRUE.equals(context.get("termsOnly"));
			int distributionBins = Integer.parseInt(String.valueOf(context.get("distributionBins")));
			
			writer.startNode("correlation"); // not written in JSON
			
			int i = 0;
			for (DocumentTerm documentTerm : documentTermCorrelation.getDocumentTerms()) {
				writer.startNode(i++==0 ? "source" : "target");
				
				if (termsOnly) {
					writer.setValue(documentTerm.getTerm());
				} else {
					writer.startNode("term");
					writer.setValue(documentTerm.getTerm());
					writer.endNode();
					
					ToolSerializer.setNumericNode(writer, "rawFreq", documentTerm.getRawFrequency());
					
					ToolSerializer.setNumericNode(writer, "relativeFreq", documentTerm.getRelativeFrequency());
					
					ToolSerializer.setNumericNode(writer, "zscore", documentTerm.getZscore());
					
					ToolSerializer.setNumericNode(writer, "zscoreRatio", documentTerm.getZscoreRatio());
					
					ToolSerializer.setNumericNode(writer, "tfidf", documentTerm.getTfIdf());
					
					ToolSerializer.setNumericNode(writer, "totalTermsCount", documentTerm.getTotalTermsCount());
					
					ToolSerializer.setNumericNode(writer, "docIndex", documentTerm.getDocIndex());
					
					writer.startNode("docId");
					writer.setValue(documentTerm.getDocId());
					writer.endNode();

					if (withDistributions) {
						ToolSerializer.setNumericList(writer, "distributions", documentTerm.getRelativeDistributions(distributionBins));
					}
				}
				
				writer.endNode();
			}
			
			ToolSerializer.setNumericNode(writer, "correlation", documentTermCorrelation.getCorrelation());
			
			ToolSerializer.setNumericNode(writer, "significance", documentTermCorrelation.getSignificance());
			
			writer.endNode();
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
