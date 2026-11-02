/**
 * 
 */
package org.voyanttools.trombone.model;

import org.voyanttools.trombone.tool.util.ToolSerializer;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamConverter(DocumentToken.DocumentTokenConverter.class)
public class DocumentToken implements Comparable<DocumentToken> {
	
	private String docId;
	
	private int docIndex;
	
	private String term;
	
	private TokenType tokenType;
	
	private int rawFreq;
	
	private int position;
	
	private int startOffset;
	
	private int endOffset;
	
	private String lemma;
	
	private String pos;
	
	/**
	 * 
	 */
	public DocumentToken(String docId, int docIndex, String term, TokenType tokenType, int position, int startOffset, int endOffset, int rawFreq) {
		this.docId = docId;
		this.docIndex = docIndex;
		this.term = term;
		this.tokenType = tokenType;
		this.rawFreq = rawFreq;
		this.position = position;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		lemma = null;
		pos = null;
	}

	@Override
	public int compareTo(DocumentToken dt) {
		return Integer.valueOf(position).compareTo(Integer.valueOf(dt.position));
	}

	public String getTerm() {
		return term;
	}
	
	public int getDocIndex() {
		return docIndex;
	}

	public String getDocId() {
		return docId;
	}
	
	public TokenType getTokenType() {
		return tokenType;
	}
	
	public int getRawFreq() {
		return rawFreq;
	}
	
	public int getPosition() {
		return position;
	}
	
	public int getStartOffset() {
		return startOffset;
	}
	
	public int getEndOffset() {
		return startOffset;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(getTerm());
		if (lemma!=null || pos!=null) {
			sb.append(" (");
			if (lemma!=null) {sb.append(lemma);}
			if (pos!=null) {sb.append("/").append(pos);}
		}
		if (lemma!=null) {sb.append(" (").append(lemma).append(")");}
		sb.append(" ").append(position).append(":").append(startOffset).append("-").append(endOffset);
		return sb.toString();
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public String getLemma() {
		return lemma;
	}
	public String getPos() {
		return pos;
	}

	public static class DocumentTokenConverter implements Converter {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean canConvert(Class type) {
			return DocumentToken.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			DocumentToken docToken = (DocumentToken) source;
			
			writer.startNode("token");

			ToolSerializer.setStringNode(writer, "term", docToken.getTerm());
			ToolSerializer.setStringNode(writer, "tokenType", docToken.getTokenType().name());
			ToolSerializer.setStringNode(writer, "docId", docToken.getDocId());
			ToolSerializer.setNumericNode(writer, "docIndex", docToken.getDocIndex());
			ToolSerializer.setNumericNode(writer, "rawFreq", docToken.getRawFreq());
			ToolSerializer.setNumericNode(writer, "position", docToken.getPosition());
			ToolSerializer.setNumericNode(writer, "startOffset", docToken.getStartOffset());
			ToolSerializer.setNumericNode(writer, "endOffset", docToken.getEndOffset());

			writer.endNode();
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
