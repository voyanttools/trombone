/**
 * 
 */
package org.voyanttools.trombone.tool.notebook;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.voyanttools.trombone.mail.Mailer;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.storage.git.RepositoryManager;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("notebook")
public class GitNotebookManager extends AbstractTool {
	
	private final static String ID_AND_CODE_TEMPLATE = "^[\\w-]{4,16}$"; // regex for matching notebook id and access code
	
	private final static String NOTEBOOK_REPO_NAME = "notebook";
	
	public static final List<String> multivalueFields = Arrays.asList(new String[]{"keywords"}); // used for facets when indexing notebook
	public static final List<String> metadataFieldsToIndex = Arrays.asList(new String[]{"author", "title", "created", "modified", "keywords", "license", "language", "description"});
	public static final List<String> metadataFieldsToTokenize = Arrays.asList(new String[]{"title", "keywords", "description"});
	
	private RepositoryManager repoManager = null;
	
	private String id = null; // notebook source (ID, URL, etc.)
	
	private String data = null; // notebook data as JSON
	
	public GitNotebookManager(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	public float getVersion() {
		return super.getVersion()+1f;
	}
	
	@Override
	public void run() throws IOException {
		String action = parameters.getParameterValue("action", "");
		
		RepositoryManager rm = getRepositoryManager();
		
		// SAVE NOTEBOOK
		if (action.equals("save")) {
			String accessCode = parameters.getParameterValue("accessCode");
			if (accessCode.isEmpty() == false && accessCode.matches(ID_AND_CODE_TEMPLATE) == false) {
				throw new IOException("Access code does not conform to template.");
			}
			
			String notebookData = parameters.getParameterValue("data","");
			if (notebookData.trim().isEmpty()) {
				throw new IOException("Notebook contains no data.");
			}
			
			String notebookMetadata = parameters.getParameterValue("metadata", "");
			if (notebookMetadata.trim().isEmpty()) {
				throw new IOException("Notebook contains no metadata.");
			}
			
			if (parameters.getParameterValue("id","").trim().isEmpty()==false) {
				id = parameters.getParameterValue("id");
				if (id.matches(ID_AND_CODE_TEMPLATE) == false) {
					throw new IOException("Notebook ID does not conform to template.");
				}
			} else {
				while(true) {
					id = RandomStringUtils.randomAlphanumeric(6);
					try {
						if (doesNotebookFileExist(rm, id+".json") == false) {
							break;
						}
					} catch (GitAPIException e) {
						throw new IOException(e.toString());
					}
				}
			}

			String storedAccessCode;
			try {
				storedAccessCode = getAccessCodeFile(rm, id);
			} catch (IOException | GitAPIException e) {
				storedAccessCode = "";
			}
			
			if (storedAccessCode.isEmpty() || storedAccessCode.equals(accessCode)) {
				try {
					RevCommit commit = rm.addFile(NOTEBOOK_REPO_NAME, id+".json", notebookData);
					rm.addNoteToCommit(NOTEBOOK_REPO_NAME, commit, notebookMetadata);
					rm.addFile(NOTEBOOK_REPO_NAME, id, accessCode);
				} catch (IOException | GitAPIException e) {
					throw new IOException(e.toString());
				}
				indexNotebook(new StoredNotebookSource(id, notebookData, notebookMetadata), false);
				data = "true";
			} else {
				data = "false"; // don't throw error here because we don't want to trigger a popup in Voyant
			}
			
			String email = parameters.getParameterValue("email", "");
			if (email.trim().isEmpty() == false) {
				String notebookUrl = "https://voyant-tools.org/spyral/"+id+"/";
				String body = "<html><head></head><body><h1>Voyant Notebooks</h1><p>Your notebook: "+notebookUrl+"</p><p>Your access code: "+accessCode+"</p></body></html>";
				try {
					Mailer.sendMail(email, "Voyant Notebooks", body);
				} catch (MessagingException e) {
					System.out.println(e.toString());
				}
			}
		}
		
		// CHECK IF NOTEBOOK EXISTS
		else if (action.equals("exists")) {
			id = parameters.getParameterValue("id","");
			if (id.trim().isEmpty() == false) {
				try {
					data = doesNotebookFileExist(rm, id+".json") ? "true" : "false";
				} catch (GitAPIException e) {
					throw new IOException(e.toString());
				}
			} else {
				throw new IOException("No notebook ID provided.");
			}
		}
		
		// CHECK IF ACCESS CODE EXISTS
		else if (action.equals("protected")) {
			id = parameters.getParameterValue("id","");
			if (id.trim().isEmpty() == false) {
				try {
					String accessCode = getAccessCodeFile(rm, id);
					if (accessCode.isEmpty()) {
						data = "false";
					} else {
						data = "true";
					}
				} catch (IOException | GitAPIException e) {
					data = "false";
				}
			} else {
				throw new IOException("No notebook ID provided.");
			}
		}
		
		// LOAD NOTEBOOK
		else if (action.equals("load")) {
			id = parameters.getParameterValue("id");
			if (id==null && parameters.getParameterValue("data", "").trim().isEmpty()==false) {
				data = parameters.getParameterValue("data"); // has been set by server
			} else {
				try {
					data = RepositoryManager.getRepositoryFile(rm.getRepository(NOTEBOOK_REPO_NAME), id+".json");
				} catch (IOException | GitAPIException e) {
					throw new IOException("Unable to retrieve notebook: "+id);
				}
			}
			if (data==null) {
				throw new IOException("Unable to retrieve notebook: "+id);
			}
		}
		
		else if (action.equals("git-catalogue")) {
			try {
				handleUntrackedFiles(rm);
				
				Repository notebookRepo = rm.getRepository(NOTEBOOK_REPO_NAME);
				
				File repoDir = notebookRepo.getWorkTree();
				List<String> files = listFilesNewestFirst(repoDir);
//				List<String> files = RepositoryManager.getRepositoryContents(notebookRepo);
				List<String> notebooks = files.stream().filter(f -> f.endsWith(".json")).collect(Collectors.toList());
				
				List<String> notes = new ArrayList<String>();
				
				int count = 0;
				int max = parameters.getParameterIntValue("limit", 100);
				for (String notebook : notebooks) {
					if (count >= max) {
						break;
					}
					RevCommit rc = RepositoryManager.getMostRecentCommitForFile(notebookRepo, notebook);
					
					try (Git git = new Git(notebookRepo)) {
						Note note = git.notesShow().setObjectId(rc).call();
						ObjectLoader loader = notebookRepo.open(note.getData());
						String noteContents = RepositoryManager.getStringFromObjectLoader(loader);
						notes.add(noteContents);
					} catch (IncorrectObjectTypeException | NullPointerException e) {
//						System.out.println("no note for "+notebook);
						String metadata = getMetadataFromNotebook(rm, notebook.replaceFirst(".json$", ""));
						if (metadata != null) {
							rm.addNoteToCommit(NOTEBOOK_REPO_NAME, rc, metadata);
							notes.add(metadata);
						}
					}
					count++;
				}
				
				data = "["+String.join(",", notes)+"]";
			} catch (GitAPIException e) {
				throw new IOException(e.toString());
			}
		}
		
		else if (action.equals("reindex")) {
			try {
				handleUntrackedFiles(rm);
				
				Repository notebookRepo = rm.getRepository(NOTEBOOK_REPO_NAME);
				
				File repoDir = notebookRepo.getWorkTree();
				List<String> files = listFilesNewestFirst(repoDir);
//				List<String> files = RepositoryManager.getRepositoryContents(notebookRepo);
				List<String> notebooks = files.stream().filter(f -> f.endsWith(".json")).collect(Collectors.toList());
				
				List<StoredNotebookSource> notebookSources = new ArrayList<>();
				for (String notebook : notebooks) {
					RevCommit rc = RepositoryManager.getMostRecentCommitForFile(notebookRepo, notebook);
					
					String notebookId = notebook.replaceFirst(".json$", "");
					
					try (Git git = new Git(notebookRepo)) {
						String notebookContents = RepositoryManager.getRepositoryFile(rm.getRepository(NOTEBOOK_REPO_NAME), notebook);
						
						Note note = git.notesShow().setObjectId(rc).call();
						ObjectLoader loader = notebookRepo.open(note.getData());
						String notebookMetadata = RepositoryManager.getStringFromObjectLoader(loader);
						
						notebookSources.add(new StoredNotebookSource(notebookId, notebookContents, notebookMetadata));
					} catch (IncorrectObjectTypeException | NullPointerException e) {
						System.out.println("no note for "+notebook);
						String notebookMetadata = getMetadataFromNotebook(rm, notebook.replaceFirst(".json$", ""));
						if (notebookMetadata != null) {
							rm.addNoteToCommit(NOTEBOOK_REPO_NAME, rc, notebookMetadata);
							String notebookContents = RepositoryManager.getRepositoryFile(rm.getRepository(NOTEBOOK_REPO_NAME), notebook);
							notebookSources.add(new StoredNotebookSource(notebookId, notebookContents, notebookMetadata));
						}
					}
				}
				indexNotebooks(notebookSources, false);
			} catch (GitAPIException e) {
				throw new IOException(e.toString());
			}
		}
	}

	private RepositoryManager getRepositoryManager() throws IOException {
		if (repoManager == null) {
			try {
				if (storage instanceof FileStorage) {
					repoManager = new RepositoryManager(((FileStorage)storage).storageLocation);
				} else {
					// TODO memory storage version
					throw new IOException("Only FileStorage is supported for RepositoryManager");
				}
				try {
					repoManager.getRepository(NOTEBOOK_REPO_NAME);
				} catch (RefNotFoundException e) {
					try (Git git = repoManager.setupRepository(NOTEBOOK_REPO_NAME)) {}
				}
			} catch (GitAPIException e) {
				throw new IOException(e.toString());
			}
		}
		return repoManager;
	}
	
	private boolean doesNotebookFileExist(RepositoryManager rm, String filename) throws IOException, GitAPIException {
		return RepositoryManager.doesRepositoryFileExist(rm.getRepository(NOTEBOOK_REPO_NAME), filename);
	}
	
	private String getAccessCodeFile(RepositoryManager rm, String filename) throws IOException, GitAPIException {
		return RepositoryManager.getRepositoryFile(rm.getRepository(NOTEBOOK_REPO_NAME), filename);
	}
	
	// from: https://stackoverflow.com/a/17625095
	private static List<String> listFilesNewestFirst(File directory) throws IOException {
	    try (final Stream<Path> fileStream = Files.list(directory.toPath())) {
	        return fileStream
	            .map(Path::toFile)
	            .collect(Collectors.toMap(Function.identity(), File::lastModified))
	            .entrySet()
	            .stream()
	            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
	            .map(Map.Entry::getKey)
	            .map(File::getName)
	            .collect(Collectors.toList());
	    }
	}
	
	private void handleUntrackedFiles(RepositoryManager rm) throws IOException, GitAPIException {
		Repository repo = rm.getRepository(NOTEBOOK_REPO_NAME);
		File work = repo.getWorkTree();
		Set<String> untracked = RepositoryManager.getUntrackedFiles(repo);
		for (String filename : untracked) {
			File untrackedFile = new File(work, filename);
			if (untrackedFile.exists()) {
				try (Git git = new Git(repo)) {
					git.add().addFilepattern(filename).call();
					RevCommit commit = git.commit().setMessage("Added file: "+filename).call();
					if (filename.endsWith(".json")) {
						String notebookMetadata = getMetadataFromNotebook(rm, filename.replaceFirst(".json$", ""));
						if (notebookMetadata != null) {
							rm.addNoteToCommit(NOTEBOOK_REPO_NAME, commit, notebookMetadata);
						}
					}
				}
			}
		}
	}
	
	private static String getMetadataFromNotebook(RepositoryManager rm, String notebookId) {
		String notebookJson;
		try {
			notebookJson = RepositoryManager.getRepositoryFile(rm.getRepository(NOTEBOOK_REPO_NAME), notebookId+".json");
		} catch (IOException | GitAPIException e) {
			return null;
		}
		
		StringBuilder metadata = new StringBuilder("{\"id\":\""+notebookId+"\"");
		
		JsonParser jsonParser = Json.createParser(new StringReader(notebookJson));
		while(jsonParser.hasNext()) {
			Event e = jsonParser.next();
			if (e == Event.KEY_NAME) {
				String key = jsonParser.getString();
				jsonParser.next();
				
				if (key.equals("metadata")) {
					JsonObject jobject = jsonParser.getObject();
					for (Entry<String, JsonValue> entry : jobject.entrySet()) {
						String e_key = entry.getKey();
						if (e_key.equals("keywords")) {
							String value = "[]";
							try {
								value = entry.getValue().asJsonArray().toString();
							} catch (Exception e2) {
								// need try statement for missing / non-array keywords
							}
							metadata.append(",\""+e_key+"\":"+value+"");
						} else {
							String value = ((JsonString) entry.getValue()).getString();
							if (e_key.equals("title") || e_key.equals("description")) {
								value = value.replaceAll("<\\/?\\w+.*?>", ""); // remove possible tags
								value = value.replaceAll("\\t|\\n|\\r", " "); // remove tabs, it causes the JsonTokenizer to fail
							}
							metadata.append(",\""+e_key+"\":\""+value+"\"");
						}
					}
					
					break;
				}
			}
		}
		
		metadata.append("}");
		return metadata.toString();
	}
	
	private void indexNotebook(StoredNotebookSource notebook, boolean useExecutor) throws IOException {
		List<StoredNotebookSource> notebooks = new ArrayList<StoredNotebookSource>();
		notebooks.add(notebook);
		indexNotebooks(notebooks, false);
	}
	
	private void indexNotebooks(List<StoredNotebookSource> notebooks, boolean useExecutor) throws IOException {
		IndexWriter indexWriter = storage.getNotebookLuceneManager().getIndexWriter("");
		DirectoryReader indexReader = DirectoryReader.open(indexWriter);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);	
		
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService executor;
		
		executor = Executors.newFixedThreadPool(processors);
		for (StoredNotebookSource notebook : notebooks) {
			Runnable worker = new NotebookIndexer(indexWriter, indexSearcher, notebook.getNotebookId(), notebook.getNotebookContents(), notebook.getNotebookMetadata());
			if (!useExecutor) {
				worker.run();
			} else {
				try {
					executor.execute(worker);
				} catch (Exception e) {
					System.out.println(e);
					executor.shutdown();
					throw e;
				}
			}
		}
		executor.shutdown();
		
		try {
			indexWriter.commit();
		} catch (IOException e) {
			indexWriter.close();
			throw e;
		}
	}
	
	private class StoredNotebookSource {
		private String notebookId;
		private String notebookContents;
		private String notebookMetadata;
		
		public String getNotebookId() {
			return notebookId;
		}

		public String getNotebookContents() {
			return notebookContents;
		}

		public String getNotebookMetadata() {
			return notebookMetadata;
		}

		public StoredNotebookSource(String notebookId, String notebookContents, String notebookMetadata) {
			this.notebookId = notebookId;
			this.notebookContents = notebookContents;
			this.notebookMetadata = notebookMetadata;
		}
		
	}

	private class NotebookIndexer implements Runnable {

		private IndexWriter indexWriter;
		private IndexSearcher indexSearcher;
		private String notebookId;
		private String notebookContents;
		private String notebookMetadata;
		
		public NotebookIndexer(IndexWriter indexWriter, IndexSearcher indexSearcher, String notebookId, String notebookContents, String notebookMetadata) {
			this.indexWriter = indexWriter;
			this.indexSearcher = indexSearcher;
			this.notebookId = notebookId;
			this.notebookContents = notebookContents;
			this.notebookMetadata = notebookMetadata;
		}
		
		@Override
		public void run() {
			try {
				boolean alreadyIndexed = false;
				Term notebookIdTerm = new Term("id", notebookId);
				TopDocs topDocs = indexSearcher.search(new TermQuery(notebookIdTerm), 1);
				if (topDocs.totalHits>0) {
					alreadyIndexed = true;
					System.out.println("Re-Indexing: "+notebookId);
				} else {
					System.out.println("Indexing: "+notebookId);
				}
				
				org.apache.lucene.document.Document document = new org.apache.lucene.document.Document();
				
				document.add(new StringField("id", notebookId, Field.Store.YES));
				
				JsonParser jsonParser = Json.createParser(new StringReader(notebookContents));
				while(jsonParser.hasNext()) {
					Event e = jsonParser.next();
					if (e == Event.KEY_NAME) {
						String key = jsonParser.getString();
						
						jsonParser.next();
						JsonValue jsonValue = jsonParser.getValue();
						
						if (key.equals("content")) {
							if (jsonValue.getValueType() == ValueType.STRING) {
								String content = ((JsonString) jsonValue).getString();
								document.add(new TextField("lexical", content, Field.Store.NO));
							} else {
								// it's a code or data cell
							}
						}
					}
				}
				
				FacetsConfig config = new FacetsConfig();
				
				jsonParser = Json.createParser(new StringReader(notebookMetadata));
				while(jsonParser.hasNext()) {
					Event e = jsonParser.next();
					if (e == Event.KEY_NAME) {
						String key = jsonParser.getString();
						String facet = "facet."+key;
						config.setIndexFieldName(key, facet);
						
						boolean isMulti = multivalueFields.contains(key);
						config.setMultiValued(facet, isMulti);
						
						boolean doIndex = metadataFieldsToIndex.contains(key);
						boolean tokenize = metadataFieldsToTokenize.contains(key);
						
						jsonParser.next();
						JsonValue jsonValue = jsonParser.getValue();
						
						switch (jsonValue.getValueType()) {
							case ARRAY:
								JsonArray jsonArray = jsonValue.asJsonArray();
								for (JsonValue val : jsonArray) {
									if (val.getValueType() == JsonValue.ValueType.STRING) {
										String value = ((JsonString) val).getString();
										if (value.trim().isEmpty()==false) {
											document.add(new SortedSetDocValuesFacetField(facet, value));
											if (doIndex) {
												System.out.println("adding: "+key+", "+value);
												if (tokenize) {
													document.add(new TextField(key, value, Field.Store.YES));	
													document.add(new TextField("lexical", value, Field.Store.NO));
												} else {
													document.add(new StringField(key, value, Field.Store.YES));
												}
												
											}
										}
									}
								}
								break;
							case STRING:
								String value = ((JsonString) jsonValue).getString();
								if (value.trim().isEmpty()==false) {
									document.add(new SortedSetDocValuesFacetField(facet, value));
									if (doIndex) {
										System.out.println("adding: "+key+", "+value);
										if (tokenize) {
											document.add(new TextField(key, value, Field.Store.YES));	
											document.add(new TextField("lexical", value, Field.Store.NO));
										} else {
											document.add(new StringField(key, value, Field.Store.YES));
										}
									}
								}
								break;
							default:
								throw new RuntimeException("JSON type not supported: "+jsonValue.getValueType().toString());
						}
					}
				}
				
				
				if (alreadyIndexed) {
					indexWriter.updateDocument(notebookIdTerm, config.build(document));
				} else {
					indexWriter.addDocument(config.build(document));
				}
			
			} catch (IOException e) {
				throw new RuntimeException("Unable to index notebook: "+notebookId, e);
			}
		}
		
	}

	
	
}
