/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
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
@XStreamAlias("corpusVectors")
@XStreamConverter(CorpusVectors.CorpusVectorsConverter.class)
public class CorpusVectors extends AbstractCorpusTool {
	
	private Map<String, Collection<String>> nearestWords = null;
	private Map<String, double[]> wordVectors = null;
	
	private enum Model {
		da("/opennlp/da-sent.bin"),
		de("/opennlp/de-sent.bin"),
		en("/opennlp/en-sent.bin"),
		nl("/opennlp/nl-sent.bin"),
		pt("/opennlp/pt-sent.bin"),
		se("/opennlp/se-sent.bin");
		
		private static class Holder {
			static Map<String, Model> MODEL_MAP = new HashMap<>();
		}
		private String filename;
		private Model(String filename) {
			this.filename = filename;
			Holder.MODEL_MAP.put(this.name(), this);
		}
		public static Model find(String lang) {
			Model m = Holder.MODEL_MAP.get(lang);
			return m;
		}
	}
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public CorpusVectors(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		String lang = corpusMapper.getCorpus().getLanguageCodes().iterator().next();
		Word2Vec word2Vec = getWord2Vec(corpusMapper, lang);
		
		boolean getVectors = parameters.getParameterBooleanValue("vectors");
		if (getVectors) {
			wordVectors = new HashMap<String, double[]>();
			for (String query : getQueries()) {
				double[] vec = word2Vec.getWordVector(query);
				if (vec != null) {
					wordVectors.put(query, vec);
				}
			}
		} else {
			nearestWords = new HashMap<String, Collection<String>>();
			int limit = Math.min(parameters.getParameterIntValue("limit", 10), 100);
			for (String query : getQueries()) {
				nearestWords.put(query, word2Vec.wordsNearest(query, limit));
			}
		}
	}

	
	private Word2Vec getWord2Vec(CorpusMapper corpusMapper, String lang) throws IOException {
//		new File(getClass().getClassLoader().getResource("opennlp/GoogleNews-vectors-negative300.bin.gz").getFile());
//		File gModel = new File(getClass().getClassLoader().getResource("opennlp/GoogleNews-vectors-negative300.bin.gz").getFile());
//	    Word2Vec vec = WordVectorSerializer.readWord2VecModel(gModel);
//	    return vec;
		
		// much of the code here adapted from https://deeplearning4j.org/docs/latest/deeplearning4j-nlp-word2vec
		
		String corpusVectorsId = corpusMapper.getCorpus().getId()+"-vectors.zip";

		// generate word2vec
		Word2Vec word2Vec;
		if (storage instanceof FileStorage && storage.isStored(corpusVectorsId, Location.object)) {
			File file = ((FileStorage) storage).getResourceFile(corpusVectorsId, Location.object);
			word2Vec = WordVectorSerializer.readWord2VecModel(file);
		} else {
			// adapted from https://www.tutorialspoint.com/opennlp/opennlp_sentence_detection.htm
			// Loading sentence detector model
			
			SentenceIterator iter = getSentenceIterator(corpusMapper, lang);
			
			// Split on white spaces in the line to get words
			TokenizerFactory t = new DefaultTokenizerFactory();
			t.setTokenPreProcessor(new CommonPreprocessor());
			
			word2Vec = new Word2Vec.Builder()
						.minWordFrequency(5)
						.layerSize(50)
						.seed(42)
						.windowSize(5)
						.iterate(iter)
						.tokenizerFactory(t)
						.build();

			word2Vec.fit();
			
			// save for next time
			if (storage instanceof FileStorage) {
				File file = ((FileStorage) storage).getResourceFile(corpusVectorsId, Location.object);
				WordVectorSerializer.writeWord2VecModel(word2Vec, file);
			}
		}
		
		return word2Vec;
	}
	
	private SentenceIterator getSentenceIterator(CorpusMapper corpusMapper, String lang) throws IOException {
		Corpus corpus = corpusMapper.getCorpus();
		
		Model langModel = Model.find(lang);
		
		SentenceIterator iter;
		if (langModel != null) {
			SentenceModel model;
			try(InputStream is = getClass().getResourceAsStream(langModel.filename)) {
				 model = new SentenceModel(is);
			} catch (IOException e) {
				throw new IOException("Unable to load sentence model");
			}
			
			// Instantiating the SentenceDetectorME class 
			SentenceDetectorME detector = new SentenceDetectorME(model);
			
			File tempSentencesFile = File.createTempFile(corpus.getId()+"tempSentences", null);
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempSentencesFile));
			
			for (IndexedDocument doc : corpusMapper.getCorpus()) {
				String text = doc.getDocumentString();
				text = text.replaceAll("<.+?>", ""); // simple strip tags but don't modify too much in case sentence model can use info (like case)
				String sentences[] = detector.sentDetect(text);
				for (String sentence : sentences) {
					sentence = sentence.toLowerCase().replaceAll("(\r|\n|\r\n)+", "").trim();
					writer.write(sentence);
					writer.newLine();
				}
			}
			writer.close();
			
			iter = new LineSentenceIterator(tempSentencesFile);
		} else {
			Pattern sentencePattern = Pattern.compile("\\b[^.!?]+[.!?]+");
			File tempSentencesFile = File.createTempFile(corpus.getId()+"tempSentences", null);
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempSentencesFile));
			for (IndexedDocument doc : corpusMapper.getCorpus()) {
				String text = doc.getDocumentString();
				text = text.replaceAll("<.+?>", ""); // simple strip tags but don't modify too much in case sentence model can use info (like case)
				Matcher matcher = sentencePattern.matcher(text);
				while (matcher.find()) {
					String sentence = matcher.group();
					sentence = sentence.toLowerCase().replaceAll("(\r|\n|\r\n)+", "").trim();
					writer.write(sentence);
					writer.newLine();
				}
			}
			writer.close();
			
			iter = new BasicLineIterator(tempSentencesFile);
		}
		
		return iter;
	}
	
	public Map<String, Collection<String>> getNearestWords() {
		return nearestWords;
	}
	
	public Map<String, double[]> getWordVectors() {
		return wordVectors;
	}
	
	public static class CorpusVectorsConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return CorpusVectors.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CorpusVectors parent = (CorpusVectors) source;
			
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
