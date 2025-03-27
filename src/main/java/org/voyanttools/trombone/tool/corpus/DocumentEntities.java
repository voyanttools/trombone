/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.EntityType;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.nlp.NlpAnnotator;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.Stripper;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ca.lincsproject.nssi.VoyantNssiClient;
import ca.lincsproject.nssi.VoyantSpacyClient;

/**
 * @author Andrew MacDonald
 *
 */
@XStreamAlias("documentEntities")
public class DocumentEntities extends AbstractAsyncCorpusTool {

	private static enum NLP {
		Stanford, OpenNLP, NSSI, SPACY
	}
	
	@XStreamOmitField
	private boolean verbose = false;
	
	@XStreamOmitField
	private boolean includeEntities = false; // flag for actually including the entities in the results
	
	@XStreamOmitField
	private boolean retryFailures = false;

	private final static int TIMEOUT_FAIL = 60; // how many minutes need to pass before failing
	
	private final static int CHARS_PER_TEXT_CHUNK = 100000; // text chunk size to divide documents into
	
	private static ListeningExecutorService workThreadPool = null;
	private static ListeningExecutorService listenThreadPool = null;
	
	private List<DocumentEntity> entities = new ArrayList<DocumentEntity>(); // the entities
	
	private Map<String, String> status = new HashMap<String, String>(); // the status for each document

	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentEntities(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		verbose = parameters.getParameterBooleanValue("verbose");
	}
	
	public float getVersion() {
		return super.getVersion()+1;
	}
	
	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		realrun(corpusMapper);
	}
	
	public void realrun(CorpusMapper corpusMapper) throws IOException {
		
		includeEntities = parameters.getParameterBooleanValue("includeEntities");
		
		retryFailures = parameters.getParameterBooleanValue("retryFailures");
		
		Set<EntityType> types = new HashSet<EntityType>();
		for (String type : parameters.getParameterValues("type")) {
			for (String t : type.split(",\\s*")) {
				EntityType et = EntityType.getForgivingly(t);
				if (et!=EntityType.unknown) {
					types.add(et);
				}
			}
		}
		
		ListenableFuture<List<DocResult>> futures = runAsync(corpusMapper);
		
		future = Futures.transform(futures, new Function<List<DocResult>, List<DocumentEntity>>() {
			@Override
			public @Nullable List<DocumentEntity> apply(@Nullable List<DocResult> results) {
				boolean allDone = true;
				List<DocumentEntity> tempEntities = new ArrayList<DocumentEntity>();
				for (DocResult result : results) {
					if (result.status.equals("done")) {
						for (DocumentEntity entity : result.entities) {
							if (types.isEmpty() || types.contains(entity.getType())) {
								tempEntities.add(entity);
							}
						}
					} else if (result.status.startsWith("failed")) {
					} else {
						allDone = false;
					}
				}
				if (allDone) {
					if (includeEntities) {
						entities = tempEntities;
					}
					if (verbose) {
						System.out.println("entities found: "+tempEntities.size());
					}
				} else {
					if (verbose) {
						System.out.println("not done yet");
					}
				}
				return entities;
			}
			
		}, MoreExecutors.directExecutor());
	}
	
	@Override
	public ListenableFuture<List<DocResult>> runAsync(CorpusMapper corpusMapper) throws IOException {
		List<ListenableFuture<DocResult>> futures = new ArrayList<ListenableFuture<DocResult>>();
		
		NLP annotator;
		String anno = parameters.getParameterValue("annotator", "");
		if (anno.toLowerCase().equals("nssi")) {
			annotator = NLP.NSSI;
		} else if (anno.toLowerCase().equals("spacy")){
			annotator = NLP.SPACY;
		} else if (anno.toLowerCase().equals("opennlp")){
			annotator = NLP.OpenNLP;
		} else {
			annotator = NLP.Stanford;
		}
		
		List<String> ids = getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus());
		
		if (retryFailures) {
			for (String docId : ids) {
				resetFailedDocument(docId, annotator);
			}
		}
		
		for (String docId : ids) {
			futures.add(doNlp(corpusMapper, docId, annotator));
		}
		
		return Futures.allAsList(futures);
	}
	
	private void resetFailedDocument(String docId, NLP annotator) {
		String id = getDocumentCacheId(docId, annotator);
		try {
			String statusString = storage.retrieveString(id+"-status", Storage.Location.object);
			if (statusString.startsWith("failed")) {
				storage.storeString("retrying", id+"-status", Storage.Location.object, true);
			}
		} catch (IOException e) {
		}
	}
	
	@SuppressWarnings("unchecked")
	private ListenableFuture<DocResult> doNlp(CorpusMapper corpusMapper, String docId, NLP annotator) throws IOException {
		String id = getDocumentCacheId(docId, annotator);
		
		boolean startNlp = false;
		try {
			String statusString = storage.retrieveString(id+"-status", Storage.Location.object);
			
			status.put(docId, statusString);
			if (verbose) {
				System.out.println(docId+": "+statusString);
			}
			
			if (statusString.equals("done")) {
				if (storage.isStored(id, Storage.Location.object)) {
					if (verbose) {
						System.out.println(docId+": retrieved from storage");
					}
					try {
						DocResult result = new DocResult(docId, statusString, (List<DocumentEntity>) storage.retrieve(id, Storage.Location.object));
						return Futures.immediateFuture(result);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				} else {
					if (verbose) {
						System.out.println(docId+": done but no stored string??");
					}
					startNlp = true;
				}
			} else if (statusString.startsWith("failed")) {
				return Futures.immediateFuture(new DocResult(docId, statusString));
			} else if (statusString.equals("queued")) {
				return Futures.immediateFuture(new DocResult(docId, statusString));
			} else if (statusString.equals("retrying")) {
				startNlp = true;
			} else {
				Instant runStart = Instant.ofEpochSecond(Integer.parseInt(statusString));
				Duration diff = Duration.between(runStart, Instant.now());
				if (diff.toMinutes() > TIMEOUT_FAIL) {
					if (verbose) {
						System.out.println(docId+": timed out");
					}
					status.put(docId, "failed");
					storage.storeString("failed > timeout", id+"-status", Storage.Location.object, true);
				} else {
					return Futures.immediateFuture(new DocResult(docId, statusString));
				}
			}
		} catch (IOException e) {
			startNlp = true;
		}
		
		if (startNlp) {
			status.put(docId, "queued");
			storage.storeString("queued", getDocumentCacheId(docId, annotator)+"-status", Storage.Location.object, true);
			
			ListenableFuture<DocResult> future =
				(ListenableFuture<DocResult>) DocumentEntities.getWorkThreadPool().submit(new DocumentEntitiesGetter(corpusMapper, docId, annotator));
			
			Futures.addCallback(future, new FutureCallback<DocResult>() {
				@Override
				public void onSuccess(@Nullable DocResult result) {
					 try {
						if (verbose) {
							System.out.println(docId+": storing");
						}
						storage.store(result.entities, id, Storage.Location.object);
						storage.storeString("done", id+"-status", Storage.Location.object, true);
					} catch (IOException e) {
						e.printStackTrace();
						try {
							storage.storeString("failed > "+e.getMessage(), id+"-status", Storage.Location.object, true);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				@Override
				public void onFailure(Throwable t) {
					System.out.println(docId+" failed: "+t.getMessage());
					try {
						storage.storeString("failed > "+t.getMessage(), id+"-status", Storage.Location.object, true);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}, DocumentEntities.getListenThreadPool());
			
			return future;
		} else {
			if (verbose) {
				System.out.println(docId+": should never be here");
			}
			return Futures.immediateFuture(null);
		}
	}
	
	
	private class DocumentEntitiesGetter implements Callable<DocResult> {

		private CorpusMapper corpusMapper;
		private String docId;
		private NLP annotator;

		public DocumentEntitiesGetter(CorpusMapper corpusMapper, String docId, NLP annotator) {
			this.corpusMapper = corpusMapper;
			this.docId = docId;
			this.annotator = annotator;
		}

		@Override
		public DocResult call() throws Exception {
			if (verbose) {
				System.out.println(docId+": submitting job");
			}
			String startTime = String.valueOf(Instant.now().getEpochSecond());
			status.put(docId, startTime);
			storage.storeString(startTime, getDocumentCacheId(docId, annotator)+"-status", Storage.Location.object, true);
			
			IndexedDocument indexedDocument = corpusMapper.getCorpus().getDocument(docId);
			String lang = corpusMapper.getCorpus().getLanguageCodes().toArray(new String[0])[0];
			
			String docString = indexedDocument.getDocumentString();
			
			List<DocumentEntity> ents = new ArrayList<DocumentEntity>();
			
			if (hasChunkSupport(annotator)) {
				Map<String, DocumentEntity> entitiesMap = new HashMap<>();
				
				List<String> chunks = getTextChunks(docString, CHARS_PER_TEXT_CHUNK);
				if (verbose) {
					System.out.println(docId+": chunks "+chunks.size());
				}
				
				int currOffset = 0;
				for (String chunk : chunks) {
					
					List<DocumentEntity> chunkEnts;
					if (annotator.equals(NLP.NSSI)) {
						int jobId = VoyantNssiClient.submitJob(chunk);
						chunkEnts = VoyantNssiClient.getResults(jobId);
					} else if (annotator.equals(NLP.SPACY)) {
						if (verbose) {
							System.out.println(docId+": submitting chunk "+chunk.getBytes().length);
						}
						chunkEnts = VoyantSpacyClient.submitJob(chunk, lang, verbose);
					} else {
						chunkEnts = new ArrayList<DocumentEntity>();
					}
					
					// go through the offsets and adjust them
					for (DocumentEntity chunkEnt : chunkEnts) {
						int[][] offsets = chunkEnt.getOffsets();
						int[][] adjOffsets = new int[offsets.length][2];
						for (int i = 0; i < offsets.length; i++) {
							int[] offset = offsets[i];
							adjOffsets[i] = new int[] {offset[0]+currOffset, offset[1]+currOffset};
						}
						chunkEnt.setOffsets(adjOffsets);
						
						// add/update the map
						if (entitiesMap.containsKey(chunkEnt.getTerm())) {
							DocumentEntity match = entitiesMap.get(chunkEnt.getTerm());
							match.setOffsets(concatOffsets(match.getOffsets(), chunkEnt.getOffsets()));
						} else {
							entitiesMap.put(chunkEnt.getTerm(), chunkEnt);
						}
					}
					
					currOffset += chunk.length();
				}
				
				// convert map to list
				for (DocumentEntity ent : entitiesMap.values()) {
					ents.add(ent);
				}
			} else {
				if (lang.equals("en")) {
					if (annotator.equals(NLP.OpenNLP)) {
						NlpAnnotator nlpAnnotator = storage.getNlpAnnotatorFactory().getOpenNlpAnnotator(lang);
						if (verbose) {
							System.out.println(docId+": getting entities");
						}
						ents = nlpAnnotator.getEntities(corpusMapper, indexedDocument, parameters);
						if (verbose) {
							System.out.println(docId+": got entities");
						}
					} else {
						NlpAnnotator nlpAnnotator = storage.getNlpAnnotatorFactory().getNlpAnnotator(lang);
						if (verbose) {
							System.out.println(docId+": getting entities");
						}
						ents = nlpAnnotator.getEntities(corpusMapper, indexedDocument, parameters);
						if (verbose) {
							System.out.println(docId+": got entities");
						}
					}
				} else {
					return new DocResult(docId, "done", new ArrayList<DocumentEntity>());
				}
			}
			
			cleanEntities(ents);
			int docIndex = corpusMapper.getCorpus().getDocumentPosition(docId);
			for (DocumentEntity ent : ents) {
				ent.setDocIndex(docIndex);
			}
			// FIXME OpenNLP offsets and positions don't match up with Lucene because of how tokens are provided to OpenNLP
			addPositionsToEntities(corpusMapper, indexedDocument, ents);
			
			return new DocResult(docId, "done", ents);
		}
		
	}
	
	private boolean hasChunkSupport(NLP annotator) {
		return annotator.equals(NLP.NSSI) || annotator.equals(NLP.SPACY);
	}
	
	// split a string into chunks at the nearest space character
	private List<String> getTextChunks(String text, int chunkSize) {
		List<String> chunks = new ArrayList<String>();
		
		if (text.length() < chunkSize) {
			chunks.add(text);
			return chunks;
		}
		
		int nextPos = chunkSize;
		int currPos = 0;
		while (nextPos < text.length()) {
			nextPos = text.lastIndexOf(" ", nextPos);
			chunks.add(text.substring(currPos, nextPos));
			currPos = nextPos;
			nextPos = currPos + chunkSize;
		}
		chunks.add(text.substring(currPos));
		
		return chunks;
	}
	
	private void cleanEntities(List<DocumentEntity> entities) {
		Stripper stripper = new Stripper(Stripper.TYPE.ALL);
		for (DocumentEntity ent : entities) {
			ent.setTerm(stripper.strip(ent.getTerm()));
			ent.setNormalized(stripper.strip(ent.getNormalized()));
		}
	}
	
	private int[][] concatOffsets(int[][] array1, int[][] array2) {
		int[][] array1and2 = new int[array1.length + array2.length][2];
		System.arraycopy(array1, 0, array1and2, 0, array1.length);
		System.arraycopy(array2, 0, array1and2, array1.length, array2.length);
		return array1and2;
	}
	
	private void addPositionsToEntities(CorpusMapper corpusMapper, IndexedDocument indexedDocument, List<DocumentEntity> entities) throws IOException {
		// term/type key -> offsetIndex key -> [startPosition, endPosition]
		Map<String, Map<Integer, List<Integer>>> entityPositionsMap = new HashMap<String, Map<Integer, List<Integer>>>();
		
		int luceneDoc = corpusMapper.getLuceneIdFromDocumentId(indexedDocument.getId());
		// TODO: check that we can assume that offsets align regardless of TokenType
		Terms terms = corpusMapper.getLeafReader().getTermVector(luceneDoc, TokenType.lexical.name()); 
		TermsEnum termsEnum = terms.iterator();
		while(true) {
			// go through all terms
			BytesRef term = termsEnum.next();
			if (term!=null) {
				PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.OFFSETS);
				if (postingsEnum!=null) {
					postingsEnum.nextDoc();
					// go through each instance of the term
					for (int i=0, len = postingsEnum.freq(); i<len; i++) {
						int pos = postingsEnum.nextPosition();
						int startOffset = postingsEnum.startOffset();
						int endOffset = postingsEnum.endOffset();
						// go through the entities and match entity offset to term instance offset
						for (DocumentEntity entity : entities) {
							int[][] entOffsets = entity.getOffsets();
							int offsetsCount = 0;
							for (int[] entOffset : entOffsets) {
								if (entOffset[0] == startOffset || entOffset[1] == endOffset) {
									String key = entity.getTerm()+" -- "+entity.getType().name();
									if (!entityPositionsMap.containsKey(key)) {
										entityPositionsMap.put(key, new HashMap<>());
									}
									if (entityPositionsMap.get(key).get(offsetsCount) == null) {
										entityPositionsMap.get(key).put(offsetsCount, new ArrayList<>());
									}
									if (entOffset[0] == startOffset) {
										entityPositionsMap.get(key).get(offsetsCount).add(0, pos);
									} else {
										entityPositionsMap.get(key).get(offsetsCount).add(pos);
									}
								}
								offsetsCount++;
							}
						}
					}
				}
			}
			else {break;}
		}
		
		List<Integer> toRemove = new ArrayList<Integer>();
		for (int i = 0; i < entities.size(); i++) {
			DocumentEntity entity = entities.get(i);
			String key = entity.getTerm()+" -- "+entity.getType().name();
			Map<Integer, List<Integer>> offsetPositions = entityPositionsMap.get(key);
			if (offsetPositions != null) {
				// find max index instead of using offsetPositions size, since some entries might be missing
				int maxIndex = Collections.max(offsetPositions.keySet()) + 1;
				int[][] posArray = new int[maxIndex][2];
				for (int offsetIndex = 0; offsetIndex < maxIndex; offsetIndex++) {
					if (offsetPositions.containsKey(offsetIndex)) {
						List<Integer> positions = offsetPositions.get(offsetIndex);
						if (positions.size() == 2) {
							posArray[offsetIndex] = new int[] {positions.get(0), positions.get(1)};
						} else {
							posArray[offsetIndex] = new int[] {positions.get(0)};
						}
					} else {
						posArray[offsetIndex] = new int[] {-1};
					}
				}
				entity.setPositions(posArray);
			} else {
				toRemove.add(i);
			}
		}
		
		for (int j = toRemove.size()-1; j >= 0; j--) {
			int index = toRemove.get(j);
			entities.remove(index);
		}
	}
	
	
	public static ExecutorService getWorkThreadPool() {
		if (DocumentEntities.workThreadPool == null || DocumentEntities.workThreadPool.isShutdown()) {
			int nThreads = 2;
			DocumentEntities.workThreadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(nThreads));
			System.out.println("DocumentEntities work thread pool init: "+nThreads);
		} else {
//			System.out.println("DocumentEntities work thread pool already init");
		}
		return DocumentEntities.workThreadPool;
	}
	
	public static ExecutorService getListenThreadPool() {
		if (DocumentEntities.listenThreadPool == null || DocumentEntities.listenThreadPool.isShutdown()) {
			int nThreads = 1;
			DocumentEntities.listenThreadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(nThreads));
			System.out.println("DocumentEntities listen thread pool init: "+nThreads);
		} else {
//			System.out.println("DocumentEntities listen thread pool already init");
		}
		return DocumentEntities.listenThreadPool;
	}
	
	public static void shutdownThreadPools() {
		doShutdown(DocumentEntities.workThreadPool);
		doShutdown(DocumentEntities.listenThreadPool);
	}
	
	private static void doShutdown(ExecutorService threadPool) {
		if (threadPool != null) {
			System.out.println("DocumentEntities thread pool shutting down: "+threadPool.toString());
			threadPool.shutdown();
			try {
				if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
					System.out.println("DocumentEntities thread pool timeout shutdown now: "+threadPool.toString());
					threadPool.shutdownNow();
				}
			} catch (InterruptedException ex) {
				System.out.println("DocumentEntities thread pool error catch shutdown now: "+threadPool.toString());
				threadPool.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}
	
	
	private String getDocumentCacheId(String docId, NLP annotator) {
		return "cached-document-entities-"+String.valueOf(this.getVersion())+"-"+annotator.name().toLowerCase()+"-"+docId;
	}
	
	
	public List<DocumentEntity> getDocumentEntities() {
		return entities;
	}
	
	public Map<String, String> getStatus() {
		return status;
	}

	private class DocResult {
		String docId;
		List<DocumentEntity> entities;
		String status;
		
		public DocResult(String docId, String status) {
			this(docId, status, null);
		}
		
		public DocResult(String docId, String status, List<DocumentEntity> entities) {
			this.docId = docId;
			this.entities = entities;
			this.status = status;
		}
	}

}
