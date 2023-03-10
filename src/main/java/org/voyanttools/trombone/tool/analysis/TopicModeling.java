package org.voyanttools.trombone.tool.analysis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentToken;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.tool.corpus.AbstractCorpusTool;
import org.voyanttools.trombone.tool.corpus.DocumentTokens;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.TokenSequence;

@XStreamAlias("topicModeling")
@XStreamConverter(TopicModeling.TopicModelingConverter.class)
public class TopicModeling extends AbstractCorpusTool {

	private int numThreads = 1;

	private int numIterations;

	private int numTopics;

	private int numTermsPerTopic;
	
	private int seed = 0;

	private double alphaSum = 1.0;

	private double beta = 0.01;
	
	private double docSortSmoothing = 10.0;

	private boolean malletTokenization = false;
	
	private boolean debug = false;
	
	private String[][] topWords;
	
	private List<TopicDocument> topicDocuments;

	public String[][] getTopWords() {
		return topWords;
	}

	public List<TopicDocument> getTopicDocuments() {
		return topicDocuments;
	}

	public TopicModeling(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		numIterations = Math.min(1000, parameters.getParameterIntValue("iterations", 100));
		numTopics = Math.min(100, parameters.getParameterIntValue("topics", 10));
		numTermsPerTopic = Math.min(100, parameters.getParameterIntValue("termsPerTopic", 10));
		seed = parameters.getParameterIntValue("seed", 0);
//		malletTokenization = parameters.getParameterBooleanValue("malletTokenization");
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		ParallelTopicModel model = null;
		
//		try {
//			model = deserializeTopicModel();
//		} catch (Exception e) {
//		}
		
		if (model == null) {
			long start = System.currentTimeMillis();
			
			InstanceList instances;
			if (malletTokenization) {
				instances = getInstanceListFromTexts(corpusMapper);
			} else {
				instances = getInstanceListFromTokens(corpusMapper);
			} 
			
			long end = System.currentTimeMillis();
			long ellapsed = (end-start);
			if (debug) System.out.println("TopicModeling tokenization: "+ellapsed+"ms");
			
			model = getModelFromInstances(instances);
		}

		model.setRandomSeed(seed);
		
		long start = System.currentTimeMillis();
		
		model.estimate();
		
		long end = System.currentTimeMillis();
		long ellapsed = (end-start);
		if (debug) System.out.println("TopicModeling runtime: "+ellapsed+"ms");

		// https://mimno.github.io/Mallet/topics-devel
		topWords = getTopWords(model, numTermsPerTopic);
		topicDocuments = getTopicDocuments(model, docSortSmoothing);
		
//		serializeTopicModel(model);
	}

	private String[][] getTopWords(ParallelTopicModel model, int numWords) {
		Object[][] topics = model.getTopWords(numWords);
		String[][] topWords = new String[topics.length][];
		for (int i = 0; i < topics.length; i++) {
			Object[] topicWords = topics[i];
			topWords[i] = new String[topicWords.length];
			for (int j = 0; j < topicWords.length; j++) {
				topWords[i][j] = (String) topicWords[j];
			}
		}
		return topWords;
	}

	private List<TopicDocument> getTopicDocuments(ParallelTopicModel model, double smoothing) {
		Map<String, TopicDocument> topicDocs = new HashMap<>();
		
		ArrayList<TreeSet<IDSorter>> topicSortedDocuments = model.getTopicDocuments(smoothing);

		for (int topic = 0; topic < numTopics; topic++) {
			TreeSet<IDSorter> sortedDocuments = topicSortedDocuments.get(topic);

			for (IDSorter sorter : sortedDocuments) {

				int doc = sorter.getID();
				double weight = sorter.getWeight();
				String docId = (String) model.getData().get(doc).instance.getName();
				
				TopicDocument topicDoc = topicDocs.get(docId);
				if (topicDoc == null) {
					topicDoc = new TopicDocument(docId, numTopics);
					topicDocs.put(docId, topicDoc);
				}
				topicDoc.addTopicWeight(topic, weight);
			}
		}
		
		return new ArrayList<TopicDocument>(topicDocs.values());
	}
	
	private InstanceList getInstanceListFromTokens(CorpusMapper corpusMapper) throws IOException {
		FlexibleParameters docParams = parameters.clone();
		docParams.setParameter("stripTags", "all");
		docParams.setParameter("withPosLemmas", "false");
		docParams.setParameter("noOthers", "true");
		docParams.setParameter("limit", "0");
//		docParams.setParameter("perDocLimit", "25");

		DocumentTokens docTokensTool = new DocumentTokens(storage, docParams);
		
		InstanceList instances = new InstanceList(new SerialPipes(new Pipe[] {new TokenSequence2FeatureSequence()}));
		
		docTokensTool.run(corpusMapper);
		List<DocumentToken> docTokens = docTokensTool.getDocumentTokens();
		if (docTokens.isEmpty() == false) {

			TokenSequence tokSeq = null;
			int currDocIndex = -1;
			for (DocumentToken docToken : docTokens) {
				int docIndex = docToken.getDocIndex();
				if (docIndex != currDocIndex) {
					if (tokSeq != null) {
						instances.addThruPipe(new Instance(tokSeq, null, corpusMapper.getDocumentIdFromDocumentPosition(currDocIndex), null));
					}
					currDocIndex = docIndex;
					tokSeq = new TokenSequence();
				}
				tokSeq.add(docToken.getTerm().toLowerCase());
			}

			instances.addThruPipe(new Instance(tokSeq, null, corpusMapper.getDocumentIdFromDocumentPosition(currDocIndex), null));

			return instances;
		}

		return null;
	}

	// might be a bit faster than getInstanceListFromTokens, but not as robust
	private InstanceList getInstanceListFromTexts(CorpusMapper corpusMapper) throws IOException {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));

		Keywords stopwords = getStopwords(corpusMapper.getCorpus());
		if (!stopwords.isEmpty()) {
			TokenSequenceRemoveStopwords tsrs = new TokenSequenceRemoveStopwords();
			tsrs.addStopWords(stopwords.getKeywords().toArray(new String[] {}));
			pipeList.add(tsrs);
		}

		pipeList.add(new TokenSequence2FeatureSequence());

		InstanceList instances = new InstanceList(new SerialPipes(pipeList));

		instances.addThruPipe(new DocumentIterator(getDocumentsAsStrings(corpusMapper.getCorpus())));

		return instances;
	}

	private List<Map<String, String>> getDocumentsAsStrings(Corpus corpus) throws IOException {
		List<Map<String, String>> docStrings = new ArrayList<>();
		for (String id : getCorpusStoredDocumentIdsFromParameters(corpus)) {
			InputStream inputStream = null;
			try {
				inputStream = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceInputStream(id);
				String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
				// TODO subdivide very long documents to get better topic results
				Map<String, String> docMap = new HashMap<>();
				docMap.put("docId", id);
				docMap.put("docText", text);
				docStrings.add(docMap);
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		}
		return docStrings;
	}

	private ParallelTopicModel getModelFromInstances(InstanceList instances) throws IOException {
		// Note that the first parameter is passed as the sum over topics, while
		// the second is the parameter for a single dimension of the Dirichlet prior.
		ParallelTopicModel model = new ParallelTopicModel(numTopics, alphaSum, beta);

		model.addInstances(instances);
		model.setNumThreads(numThreads);
		model.setNumIterations(numIterations);

		return model;
	}

	private class DocumentIterator implements Iterator<Instance> {
		Iterator<Map<String, String>> subIterator;
		Object target;

		public DocumentIterator(List<Map<String, String>> data, Object target) {
			this.subIterator = data.iterator();
			this.target = target;
		}

		public DocumentIterator(List<Map<String, String>> data) {
			this(data, null);
		}

		public Instance next() {
			Map<String, String> next = subIterator.next();
			return new Instance(next.get("docText"), target, next.get("docId"), null);
		}

		public boolean hasNext() {
			return subIterator.hasNext();
		}

		public void remove() {
			subIterator.remove();
		}
	}
	
	private void serializeTopicModel(ParallelTopicModel model) throws IOException {
		String cacheId = getCacheIdFromParameters();
		System.out.println("saving model: "+cacheId);
		storage.store(model, cacheId, Storage.Location.object);
	}
	
	private ParallelTopicModel deserializeTopicModel() throws IOException, ClassNotFoundException {
		String cacheId = getCacheIdFromParameters();
		Object obj = storage.retrieve(cacheId, Storage.Location.object);
		ParallelTopicModel model = (ParallelTopicModel) obj;
		System.out.println("retrieving model: "+cacheId);
		return model;
	}
	
	private String getCacheIdFromParameters() {
		StringBuilder sb = new StringBuilder("topicmodel-");
		if (parameters.containsKey("corpus")) {
			sb.append(parameters.getParameterValue("corpus")).append("-");
		}
		List<String> names = new ArrayList(parameters.getKeys());
		Collections.sort(names);
		StringBuilder paramsBuilder = new StringBuilder();
		for (String name : names) {
			if (name.startsWith("_dc") == false && name.startsWith("noCache") == false) {
				paramsBuilder.append(name).append(StringUtils.join(parameters.getParameterValues(name)));
			}
		}
		sb.append(DigestUtils.md5Hex(paramsBuilder.toString()));
		String id = sb.toString();
		return id;
	}
	
	private class TopicDocument {
		String docId;
		double[] weights;
		public TopicDocument(String docId, int numTopics) {
			this.docId = docId;
			this.weights = new double[numTopics];
		}
		public void addTopicWeight(int topic, double weight) {
			this.weights[topic] = weight;
		}
	}
	
	public static class TopicModelingConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return TopicModeling.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			TopicModeling topMod = (TopicModeling) source;
			
			ToolSerializer.startNode(writer, "topicWords", List.class);
			for (String[] tw : topMod.getTopWords()) {
				ToolSerializer.startNode(writer, "words", List.class);
				for (String w : tw) {
					ToolSerializer.startNode(writer, "word", String.class);
					writer.setValue(w);
					ToolSerializer.endNode(writer);
				}
				ToolSerializer.endNode(writer);
			}
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "topicDocuments", List.class);
			for (TopicDocument td : topMod.getTopicDocuments()) {
				writer.startNode("documents");
				writer.startNode("docId");
				writer.setValue(td.docId);
				writer.endNode();
				ToolSerializer.setNumericList(writer, "weights", td.weights);
				writer.endNode();
			}
			ToolSerializer.endNode(writer);
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	public static void main(String[] args) throws IOException {
		File file = new File("D:\\VoyantData\\trombone5_3");
		Storage storage = new FileStorage(file);
		FlexibleParameters params = new FlexibleParameters();
//		params.addParameter("corpus", "435c8bf72a967b1d70682810d93c257a"); // short
		params.addParameter("corpus", "0d874272bddc7dbe5b88e4534a55b578"); // shakespeare
//		params.addParameter("docIndex", new String[] {"0", "1", "2"});
		params.addParameter("stopList", "auto");
		params.addParameter("iterations", "10");
		params.addParameter("topics", "5");
//		params.addParameter("malletTokenization", "true");
		TopicModeling tm = new TopicModeling(storage, params);
		tm.run();
		
		for (String[] tw : tm.getTopWords()) {
			System.out.println(Arrays.toString(tw));
		}
		
//		for (TopicDocument td : tm.getTopicDocuments()) {
//			System.out.println(td.docId+": "+Arrays.toString(td.weights));
//		}
	}

}
