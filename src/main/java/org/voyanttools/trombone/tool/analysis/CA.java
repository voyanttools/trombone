package org.voyanttools.trombone.tool.analysis;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.ArrayUtils;
import org.voyanttools.trombone.model.RawCATerm;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("caAnalysis")
@XStreamConverter(CA.CAConverter.class)
/**
 * A wrapper to facilitate sending vector inputs directly to CorrespondenceAnalysis
 * @author Andrew MacDonald
 *
 */
public class CA extends AbstractTool {

	private CorrespondenceAnalysis ca;
	
	private String vectors;
	private int dimensions;

	private double[][] result;
	
	public CA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		// the input vectors, a 2 dimensional array of floats/doubles
		vectors = parameters.getParameterValue("vectors");

		dimensions = parameters.getParameterIntValue("dimensions", 2);
	}

	@Override
	public void run() throws IOException {
		double[][] input = AnalysisUtils.getMatrixFromString(vectors);
		if (input.length < 5) {
			throw new IOException("Too few vectors submitted. Analysis requires at least 5.");
		}
		
		ca = new CorrespondenceAnalysis(input);
		ca.runAnalysis();
		result = ca.getRowProjections();
		
		if (result.length > 0 && result[0].length > dimensions) {
			for (int i = 0; i < result.length; i++) {
				result[i] = ArrayUtils.subarray(result[i], 0, dimensions);
			}
		}
	}

	public static class CAConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return CA.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			CA ca = (CA) source;
			final double[][] result = ca.getResult();
			
			ToolSerializer.startNode(writer, "vectors", List.class);
			context.convertAnother(result);
			ToolSerializer.endNode(writer);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			return null;
		}
	}
	
	public double[][] getResult() {
		return result;
	}
	
}
