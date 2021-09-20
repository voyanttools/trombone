package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.tool.corpus.AbstractCorpusTool;
import org.voyanttools.trombone.tool.progress.Progress;
import org.voyanttools.trombone.tool.progress.Progress.Status;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import ca.lincsproject.nssi.NssiResult;
import ca.lincsproject.nssi.VoyantNssiClient;

@XStreamAlias("nssi")
@XStreamConverter(NSSI.NSSIConverter.class)
public class NSSI extends AbstractCorpusTool {

	private Progress totalProgress = null;
	
	private Map<String, Boolean> allDocProgress = null;
	
	private Map<String, NssiResult> nssiResults = null;
	
	private static ExecutorService threadPool = null;
	
	public NSSI(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);	
	}

	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		nssiResults = getEntities(corpusMapper);
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, NssiResult> getEntities(CorpusMapper corpusMapper) throws IOException {
		String corpusId = corpusMapper.getCorpus().getId();
		String id = getCorpusCacheId(corpusId);
		if (storage.isStored(id, Location.cache)) {
			try {
				System.out.println("Returning cached corpus result: "+id);
				return (Map<String, NssiResult>) storage.retrieve(id, Location.cache);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			totalProgress = Progress.retrieve(storage, id);
			if (totalProgress.isNew()) {
				totalProgress.update(.1f, Status.RUNNING, "nssiRunning", "NSSI is running");
			} else {
				System.out.println("NSSI is already running for "+id);
			}
			
			Map<String, NssiResult> nssiResults = new HashMap<String, NssiResult>();
			
			List<String> docIds = getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus());
			
			allDocProgress = new HashMap<String, Boolean>();
			for (String docId : docIds) {
				allDocProgress.put(docId, false);
			}
			
			ExecutorService threadPool = NSSI.getThreadPool();
			
			for (String docId : docIds) {
				String cachedDocId = getDocumentCacheId(docId);
				if (storage.isStored(cachedDocId, Location.cache)) {
					System.out.println("getting cached results for "+docId);
					allDocProgress.put(docId, true);
					try {
						NssiResult cachedResult = (NssiResult) storage.retrieve(cachedDocId, Location.cache);
						nssiResults.put(docId, cachedResult);
					} catch (Exception e) {
						System.out.println("failed to get cached results for "+docId);
						throw new IOException(e);
					}
				} else {
					Progress docProgress = Progress.retrieve(storage, cachedDocId);
					
					int jobId = -1;
					String code = docProgress.getCode();
					String message = docProgress.getMessage();
					if (code.equals("jobId")) {
						jobId = Integer.parseInt(message);
					}
					
					DocumentEntitiesGetter deg = null;
					if (docProgress.isNew() ||
						(docProgress.isActive() && jobId == -1 && !code.equals("startJob")) // resubmit job
					) {
						IndexedDocument indexedDocument = corpusMapper.getCorpus().getDocument(docId);
						String text = indexedDocument.getDocumentString();
						deg = new DocumentEntitiesGetter(text, docId, jobId, docProgress);
					} else {
						System.out.println("resume? docId: "+docId+", jobId: "+jobId);
						deg = new DocumentEntitiesGetter(null, docId, jobId, docProgress);
//						NssiResult emptyResult = new NssiResult();
//						nssiResults.put(docId, emptyResult);
					}
					
					if (deg != null) {
						try {
							System.out.println("execute: "+docId);
							threadPool.execute(deg);
						} catch (Exception e) {
							allDocProgress.put(docId, true);
							System.out.println(e);
							System.out.println("error, should executor shutdown?");
						}
					}
				}
			}
			
			if (nssiResults.size() == docIds.size()) {
				NSSI.shutdownThreadPool();
				totalProgress.update(1f, Status.FINISHED, "nssiFinished", "NSSI has completed");
				cacheForCorpus(corpusId, nssiResults);
				return nssiResults;
			} else {
				updateTotalProgress();
				return null;
			}
		}
	}
	
	private class DocumentEntitiesGetter implements Runnable {
		private String text;
		private String docId;
		private int jobId;
		private Progress docProgress;
		
		public DocumentEntitiesGetter(String text, String docId, int jobId, Progress docProgress) {
			this.text = text;
			this.docId = docId;
			this.jobId = jobId;
			this.docProgress = docProgress;
		}

		public void run() {
			try {
				System.out.println("DocumentEntitiesGetter \""+docId+"\": getting entities");
				
				if (text != null) {
					System.out.println("DocumentEntitiesGetter \""+docId+"\": submitted entities");
					docProgress.update(.1f, Status.RUNNING, "startJob", "startJob");
					jobId = VoyantNssiClient.submitJob(text);
					docProgress.update(.5f, Status.RUNNING, "jobId", String.valueOf(jobId));
					System.out.println("DocumentEntitiesGetter \""+docId+"\": got jobId: "+jobId);
				} else {
					System.out.println("DocumentEntitiesGetter \""+docId+"\": resuming jobId: "+jobId);
				}
				
				if (jobId != -1) {
					NssiResult nssiResult = VoyantNssiClient.getResults(jobId);
//					VoyantNssiClient.printResults(nssiResult);
					cacheForDocuments(docId, nssiResult);
					
					docProgress.update(1f, Status.FINISHED, "endJob", "endJob");
					allDocProgress.put(docId, true);
					updateTotalProgress();
				} else {
					throw new IOException("jobId isn't defined!");
				}
			} catch (IOException e) {
				System.out.println("DocumentEntitiesGetter \""+docId+"\": error getting entities: "+e);
				try {
					cacheForDocuments(docId, new NssiResult(true));
					docProgress.update(1f, Status.ABORTED, "endJob", "endJob");
					allDocProgress.put(docId, true);
					updateTotalProgress();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	public static ExecutorService getThreadPool() {
		if (NSSI.threadPool == null || NSSI.threadPool.isShutdown()) {
			int nThreads = Math.min(4, Math.round(Runtime.getRuntime().availableProcessors()/2));
			NSSI.threadPool = Executors.newFixedThreadPool(nThreads);
			System.out.println("NSSI thread pool init: "+nThreads);
		} else {
			System.out.println("NSSI thread pool already init");
		}
		return NSSI.threadPool;
	}
	
	public static void shutdownThreadPool() {
		if (NSSI.threadPool != null) {
			// from https://www.baeldung.com/java-executor-wait-for-threads
			System.out.println("NSSI thread pool shutdown");
			NSSI.threadPool.shutdown();
		    try {
		        if (!NSSI.threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
		        	NSSI.threadPool.shutdownNow();
		        }
		    } catch (InterruptedException ex) {
		    	NSSI.threadPool.shutdownNow();
		        Thread.currentThread().interrupt();
		    }
		}
	}
	
	private float getTotalProgressFloat() {
		float done = 0f;
		for (Boolean b : allDocProgress.values()) {
			if (b == true) done++;
		}
		return done / allDocProgress.size();
	}
	
	private void updateTotalProgress() throws IOException {
		float totalProgressFloat = getTotalProgressFloat();
		System.out.println("Progress: "+totalProgressFloat);
		if (totalProgressFloat < 1f) {
			totalProgress.update(totalProgressFloat, Status.RUNNING, "nssiRunning", "NSSI is running");	
		} else {
			totalProgress.update(totalProgressFloat, Status.FINISHED, "nssiFinished", "NSSI has completed");
		}
	}
	
	private void cacheForDocuments(String docId, NssiResult nssiResult) throws IOException {
		System.out.println("caching document results for "+docId);
		try {
			storage.store(nssiResult, getDocumentCacheId(docId), Location.cache);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void cacheForCorpus(String corpusId, Map<String, NssiResult> nssiResults) {
		System.out.println("caching corpus results for "+corpusId);
		try {
			storage.store(nssiResults, getCorpusCacheId(corpusId), Location.cache);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String getCorpusCacheId(String corpusId) {
		return getCommonId()+"-corpus-"+corpusId;
	}
	
	private String getDocumentCacheId(String docId) {
		return getCommonId()+"-document-"+docId;
	}
	
	private String getCommonId() {
		StringBuilder sb = new StringBuilder();
		return "nssi-"+getVersion()+"-"+DigestUtils.md5Hex(sb.toString());
	}

	public Progress getProgress() {
		return totalProgress;
	}
	
	public static class NSSIConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return NSSI.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			NSSI nssi = (NSSI) source;
			
			if (nssi.totalProgress != null && nssi.totalProgress.isActive()) {
				writer.startNode("progress");
				context.convertAnother(nssi.totalProgress);
				writer.endNode();
			}
			
			ToolSerializer.startNode(writer, "documents", List.class);
			
			if (nssi.nssiResults != null) {
				Set<String> docIds = nssi.nssiResults.keySet();
				
				for (String docId : docIds) {
					NssiResult nr = nssi.nssiResults.get(docId);
					
					writer.startNode("document");
					
					writer.startNode("id");
					writer.setValue(docId);
					writer.endNode();
					
					if (nr.hasError()) {
						ToolSerializer.startNode(writer, "error", Boolean.class);
						writer.setValue("true");
						ToolSerializer.endNode(writer);
					}
					
					ToolSerializer.startNode(writer, "entities", List.class);
					
					nr.iterator();
					while (nr.hasNext()) {
						nr.next();
						
						writer.startNode("entity");
						
						writer.startNode("entity");
						writer.setValue(nr.getCurrentEntity());
						writer.endNode();
						
						writer.startNode("lemma");
						writer.setValue(nr.getCurrentLemma());
						writer.endNode();
						
						writer.startNode("classification");
						writer.setValue(nr.getCurrentClassification());
						writer.endNode();
						
						ToolSerializer.startNode(writer, "start", Integer.class);
						writer.setValue(String.valueOf(nr.getCurrentStart()));
						ToolSerializer.endNode(writer);
						
						ToolSerializer.startNode(writer, "end", Integer.class);
						writer.setValue(String.valueOf(nr.getCurrentEnd()));
						ToolSerializer.endNode(writer);
	
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
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}

