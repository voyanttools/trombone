package org.voyanttools.trombone.tool.analysis;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

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

@XStreamAlias("pcaAnalysis")
@XStreamConverter(PCA.PCAConverter.class)
/**
 * A wrapper to facilitate sending vector inputs directly to PrincipalComponentsAnalysis
 * @author Andrew MacDonald
 *
 */
public class PCA extends AbstractTool {

	private PrincipalComponentsAnalysis pca;
	
	private String vectors;
	private int dimensions;
	
	private double[][] result;
	
	public PCA(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		
		// the input vectors, a 2 dimensional array of floats/doubles
		vectors = parameters.getParameterValue("vectors");
		// the number of dimensions to reduce to
		dimensions = parameters.getParameterIntValue("dimensions", 2);
	}

	@Override
	public void run() throws IOException {
		double[][] input = PCA.convertStringToArray(vectors);
		pca = new PrincipalComponentsAnalysis(input);
		pca.runAnalysis();
		result = pca.getResult(dimensions);
	}
	
	private static double[][] convertStringToArray(String inputString) throws IOException {
		int numRows = -1;
		int numCols = -1;
		
		inputString = inputString.replaceAll("\\s+", "");
		
		String[] rows = inputString.split("],");
		if (rows.length > 0) {
			numRows = rows.length;
			String row = rows[0];
			String[] cols = row.split(",");
			if (cols.length > 0) {
				numCols = cols.length;
			} else {
				throw new IOException("No cols found!");
			}
		} else {
			throw new IOException("No rows found!");
		}
		
		try (Scanner sc = new Scanner(inputString).useDelimiter("\\[+|\\],\\[|\\]+|,")) {
			double[][] matrix = new double[numRows][numCols];
			for (int r = 0; r < numRows; r++) {
				for (int c = 0; c < numCols; c++) {
					String num = sc.next();
					matrix[r][c] = Double.parseDouble(num);
				}
			}
			return matrix;
		} catch (NumberFormatException e) {
			throw new IOException("Bad input string!");
		}
	}

	public static class PCAConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return PCA.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			
			PCA pca = (PCA) source;
			final double[][] result = pca.getResult();
			
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
	
	
	public static void main(String[] args) {
		String inputString = "[[0.005988024175167084, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0], [0.0, 0.0, 0.005988024175167084, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.005988024175167084, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0], [0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084], [0.0, 0.0, 0.0, 0.005988024175167084, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0], [0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084], [0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0], [0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0], [0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0], [0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0], [0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0], [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.005988024175167084, 0.0, 0.0]]";
		
		try {
			PCA.convertStringToArray(inputString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
