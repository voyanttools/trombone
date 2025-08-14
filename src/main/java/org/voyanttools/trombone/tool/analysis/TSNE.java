package org.voyanttools.trombone.tool.analysis;

import java.io.IOException;
import java.util.List;

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

@XStreamAlias("tsneAnalysis")
@XStreamConverter(TSNE.TSNEConverter.class)
/**
 * A wrapper to facilitate sending vector inputs directly to TSNEAnalysis
 * @author Andrew MacDonald
 *
 */
public class TSNE extends AbstractTool {

	private TSNEAnalysis tsner;
	
	private String vectors;
    private int iterations = 2000;
	private float perplexity;
	private float theta;
	private int dimensions;
	
	private double[][] result;
	
	public TSNE(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		// the input vectors, a 2 dimensional array of floats/doubles
		vectors = parameters.getParameterValue("vectors");
		// the number of dimensions to reduce to
		dimensions = parameters.getParameterIntValue("dimensions", 2);
	}

	@Override
	public void run() throws IOException {
		double[][] input = AnalysisUtils.getMatrixFromString(vectors);
		if (input.length < 5) {
			throw new IOException("Too few vectors submitted. Analysis requires at least 5.");
		}
		
		tsner = new TSNEAnalysis(input);
		tsner.setIterations(parameters.getParameterIntValue("iterations"));
		tsner.setPerplexity(parameters.getParameterFloatValue("perplexity"));
		tsner.setTheta(parameters.getParameterFloatValue("theta"));
		tsner.setDimensions(parameters.getParameterIntValue("dimensions", 2));
		tsner.runAnalysis();
		result = tsner.getResult();
	}

	public static class TSNEConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return TSNE.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			TSNE tsne = (TSNE) source;
			final double[][] result = tsne.getResult();
			
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
