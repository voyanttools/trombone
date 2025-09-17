/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.documentiterator.BasicLabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.SimpleLabelAwareIterator;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

/**
 * @author sgs
 * @author Andrew MacDonald
 *
 */
@XStreamAlias("documentVectors")
@XStreamConverter(DocumentVectors.DocumentVectorsConverter.class)
public class DocumentVectors extends AbstractCorpusTool {
	
	private Map<String, Collection<String>> nearestWords = null;
	private Map<String, double[]> wordVectors = null;
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentVectors(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		ParagraphVectors para2vec = getParagraphVectors(corpusMapper);
		
		// https://github.com/deeplearning4j/deeplearning4j-examples/blob/051c59bd06b38ed39ca92f5940a6ca43b0f34c0f/oreilly-book-dl4j-examples/dl4j-examples/src/main/java/org/deeplearning4j/examples/nlp/paragraphvectors/ParagraphVectorsClassifierExample.java#L38
		
		String target = parameters.getParameterValue("target", "foo bar");
		LabelledDocument unlabelledDoc = new LabelledDocument();
		unlabelledDoc.setContent(target);
		
		TokenizerFactory t = new DefaultTokenizerFactory();
		t.setTokenPreProcessor(new CommonPreprocessor());
		
		List<String> docLabels = Arrays.asList(parameters.getParameterValues("docLabels"));
		
		INDArray documentAsCentroid = para2vec.inferVector(target);
		LabelSeeker seeker = new LabelSeeker(docLabels, (InMemoryLookupTable<VocabWord>) para2vec.getLookupTable());
		
		List<Pair<String, Double>> scores = seeker.getScores(documentAsCentroid);
	}
	
	private ParagraphVectors getParagraphVectors(CorpusMapper corpusMapper) throws IOException {
		String paragraphVectorsId = corpusMapper.getCorpus().getId()+"-paragraphvectors.zip";
		
		ParagraphVectors para2vec;
		if (storage instanceof FileStorage && storage.isStored(paragraphVectorsId, Location.object)) {
			File file = ((FileStorage) storage).getResourceFile(paragraphVectorsId, Location.object);
			para2vec = WordVectorSerializer.readParagraphVectors(file);
		} else {
			LabelAwareIterator iter = getDocumentIterator(corpusMapper);
			
			// Split on white spaces in the line to get words
			TokenizerFactory t = new DefaultTokenizerFactory();
			t.setTokenPreProcessor(new CommonPreprocessor());
			
			para2vec = new ParagraphVectors.Builder()
						.learningRate(0.025)
						.minLearningRate(0.001)
						.batchSize(1000)
						.epochs(20)
						.iterate(iter)
						.trainWordVectors(true)
						.tokenizerFactory(t)
						.build();

			para2vec.fit();
			
			// save for next time
			if (storage instanceof FileStorage) {
				File file = ((FileStorage) storage).getResourceFile(paragraphVectorsId, Location.object);
				WordVectorSerializer.writeParagraphVectors(para2vec, file);
			}
		}
		
		return para2vec;
	}

	
	
	private LabelAwareIterator getDocumentIterator(CorpusMapper corpusMapper) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		List<String> docIds = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		
		List<String> docLabels = Arrays.asList(parameters.getParameterValues("docLabels"));
		
		List<LabelledDocument> labelledDocs = new ArrayList<LabelledDocument>();
		for (int i = 0; i < docIds.size(); i++) {
			String docId = docIds.get(i);
			String docContent = corpus.getDocument(docId).getDocumentString();
			String docLabel = docLabels.get(i);
			LabelledDocument lDoc = new LabelledDocument();
			lDoc.setContent(docContent);
			lDoc.addLabel(docLabel);
		}
		
		SimpleLabelAwareIterator iter = new SimpleLabelAwareIterator(labelledDocs);
		
		return iter;
	}
	
	private class LabelSeeker {
		private List<String> labelsUsed;
		private InMemoryLookupTable<VocabWord> lookupTable;

		public LabelSeeker(List<String> labelsUsed, InMemoryLookupTable<VocabWord> lookupTable) {
			if (labelsUsed.isEmpty()) throw new IllegalStateException("You can't have 0 labels used for ParagraphVectors");
			this.lookupTable = lookupTable;
			this.labelsUsed = labelsUsed;
		}

		/**
		 * This method accepts vector, that represents any document,
		 * and returns distances between this document, and previously trained categories
		 * @return
		 */
		public List<Pair<String, Double>> getScores(INDArray vector) {
			List<Pair<String, Double>> result = new ArrayList<>();
			for (String label: labelsUsed) {
				INDArray vecLabel = lookupTable.vector(label);
				if (vecLabel == null) throw new IllegalStateException("Label '"+ label+"' has no known vector!");

				double sim = Transforms.cosineSim(vector, vecLabel);
				result.add(new Pair<String, Double>(label, sim));
			}
			return result;
		}
	}
	
	public Map<String, Collection<String>> getNearestWords() {
		return nearestWords;
	}
	
	public Map<String, double[]> getWordVectors() {
		return wordVectors;
	}
	
	public static class DocumentVectorsConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return DocumentVectors.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			DocumentVectors parent = (DocumentVectors) source;
			
			ToolSerializer.startNode(writer, "queries", Map.class);
			
			if (parent.getParameters().getParameterBooleanValue("vectors")) {
				final Map<String, double[]> vecs = parent.getWordVectors();
				for (String word : vecs.keySet()) {
					writer.startNode("query");
					
					writer.startNode("word");
					writer.setValue(word);
					writer.endNode();
					
					ToolSerializer.startNode(writer, "vector", List.class);
					context.convertAnother(vecs.get(word));
					ToolSerializer.endNode(writer);
					
					writer.endNode();
				}
				
			} else {
				final Map<String, Collection<String>> words = parent.getNearestWords();
				for (String key : words.keySet()) {
					Collection<String> nearest = words.get(key);
					
					writer.startNode("query");
					
					writer.startNode("word");
					writer.setValue(key);
					writer.endNode();
					
					ToolSerializer.startNode(writer, "nearestWords", List.class);
					for (String near : nearest) {
						writer.startNode("word");
						writer.setValue(near);
						writer.endNode();
					}
					ToolSerializer.endNode(writer);
					
					writer.endNode();
				}
			}
			
			ToolSerializer.endNode(writer);
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			return null;
		}
		
	}

}
