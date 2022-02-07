/**
 * 
 */
package org.voyanttools.trombone.nlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.model.EntityType;
import org.voyanttools.trombone.util.FlexibleParameters;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
//import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author sgs
 *
 */
public class StanfordNlpAnnotator implements NlpAnnotator {
	StanfordCoreNLP pipeline;

	/**
	 * 
	 */
	StanfordNlpAnnotator(String languageCode) {
	    Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, entitymentions");
	    // props.setProperty("ner.verbose", "true");
	    if (languageCode.equals("fr")) {
	    	props.setProperty("props", "StanfordCoreNLP-french.properties");
	    }
	    pipeline = new StanfordCoreNLP(props);
	}

	@Override
	public List<DocumentEntity> getEntities(CorpusMapper corpusMapper, IndexedDocument indexedDocument, FlexibleParameters parameters) throws IOException {
		
		List<CoreMap> entitiesMap = getEntities(indexedDocument.getDocumentString());
		
		// organize by term-name so that we can group together
		Map<String, List<CoreMap>> stringEntitiesMap = new HashMap<String, List<CoreMap>>();
		for (CoreMap entity : entitiesMap) {
			String term = entity.get(TextAnnotation.class);
			EntityType type = EntityType.getForgivingly(entity.get(NamedEntityTagAnnotation.class));
			String key = term+" -- " +type.name(); 
			if (!stringEntitiesMap.containsKey(key)) {
				stringEntitiesMap.put(key, new ArrayList<CoreMap>());
			}
			stringEntitiesMap.get(key).add(entity);
		}
		
		List<DocumentEntity> entities = new ArrayList<DocumentEntity>();
		int corpusDocumentIndex = corpusMapper.getCorpus().getDocumentPosition(indexedDocument.getId());
		for (Map.Entry<String, List<CoreMap>> stringEntitiesMapEntry : stringEntitiesMap.entrySet()) {
			List<CoreMap> coreMaps = stringEntitiesMapEntry.getValue();
			Set<Integer> offsets = new HashSet<Integer>();
			for (CoreMap entity : coreMaps) {
				int startOffset = entity.get(CharacterOffsetBeginAnnotation.class);
				offsets.add(startOffset);
			}
			CoreMap entity = coreMaps.get(0);
			String term = entity.get(TextAnnotation.class);
			String normalized = entity.get(NormalizedNamedEntityTagAnnotation.class);
			EntityType type = EntityType.getForgivingly(entity.get(NamedEntityTagAnnotation.class));
			DocumentEntity e = new DocumentEntity(corpusDocumentIndex, term, normalized, type, coreMaps.size());
			e.setOffsets(offsets.stream().mapToInt(Integer::intValue).toArray());
			entities.add(e);
		}
		
		return entities;
	}

	private List<CoreMap> getEntities(String text) {
		
		List<CoreMap> sentences = getSentences(text);
		List<CoreMap> entities = new ArrayList<CoreMap>();
	    for(CoreMap sentence: sentences) {
	    	for (CoreMap entity : sentence.get(MentionsAnnotation.class)) {
	    		entities.add(entity);
	    	}
	    }
	    return entities;
	}
	
	public Annotation getAnnotated(String text) {
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		return document;
	}
	
	private List<CoreMap> getSentences(String text) {
		Annotation annotation = getAnnotated(text);
		return annotation.get(SentencesAnnotation.class);
	}
	
	// public static void main(String[] args) {
	// 	StanfordNlpAnnotator annotator = new StanfordNlpAnnotator("en");
	// 	Set<EntityType> types =  new HashSet<EntityType>();
	// 	types.add(EntityType.date);
	// 	types.add(EntityType.location);
	// 	List<CoreMap> entitiesMap = annotator.getEntities("October 1, 2015. This is a test from yesterday in London, UK.", types);
	// 	for (CoreMap entity : entitiesMap) {
	// 		String term = entity.get(TextAnnotation.class);
	// 		EntityType type = EntityType.getForgivingly(entity.get(NamedEntityTagAnnotation.class));
	// 		System.out.println(term+" -- " +type.name());
	// 	}
		
	// }

}
