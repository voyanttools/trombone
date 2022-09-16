/**
 * 
 */
package org.voyanttools.trombone.tool.notebook;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
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

import org.apache.commons.io.IOUtils;
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
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.storage.git.RepositoryManager;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("notebook")
public class GitNotebookManager extends AbstractTool {
	
	private final static String NOTEBOOK_NAME_TEMPLATE = "^[A-Za-z0-9-]{4,32}$"; // regex for matching notebook name
	
	private final static String GITHUB_USERNAME_TEMPLATE = "^[a-z\\d](?:[a-z\\d]|-(?=[a-z\\d])){0,38}$"; // regex for matching github username
	
	private final static String NOTEBOOK_REPO_NAME = "notebook";
	
	private final static String USERNAME_SEPARATOR = "@";
	
	private final static String NOTEBOOK_ID_SEPARATOR = "_";
	
	private final static String KEY_FILE_PATH = "/org/voyanttools/trombone/spyral/key.txt";
	
	public static final List<String> multivalueFields = Arrays.asList(new String[]{"keywords"}); // used for facets when indexing notebook
	public static final List<String> metadataFieldsToIndex = Arrays.asList(new String[]{"userId", "author", "title", "created", "modified", "keywords", "license", "language", "description", "catalogue"});
	public static final List<String> metadataFieldsToTokenize = Arrays.asList(new String[]{"title", "keywords", "description"});
	
	@XStreamOmitField
	private RepositoryManager repoManager = null;
	
	private String id = null; // notebook source (ID, URL, etc.)
	
	private String data = null; // notebook data as JSON
	
	private boolean success = true; // was the call successful?
	
	private String error = null; // used to store error message if not successful
	
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
			doSave();
		}
		
		// DELETE NOTEBOOK
		else if (action.equals("delete")) {
			doDelete();
		}
		
		// CHECK IF NOTEBOOK EXISTS
		else if (action.equals("exists")) {
			id = parameters.getParameterValue("id","");
			if (id.trim().isEmpty() == false) {
				data = doesNotebookFileExist(rm, id+".json") ? "true" : "false";
			} else {
				setError("No notebook ID provided.");
				return;
			}
		}
		
		// LOAD NOTEBOOK
		else if (action.equals("load")) {
			id = parameters.getParameterValue("id");
			if (id==null && parameters.getParameterValue("data", "").trim().isEmpty()==false) {
				data = parameters.getParameterValue("data"); // has been set by server
			} else {
				try {
					try (Repository repo = rm.getRepository(NOTEBOOK_REPO_NAME)) {
						data = RepositoryManager.getRepositoryFile(repo, id+".json");
					}
				} catch (Exception e) {
					setError("Unable to retrieve notebook: "+id);
					return;
				}
			}
			if (data==null) {
				setError("Unable to retrieve notebook: "+id);
				return;
			}
		}
		
		else if (action.equals("git-catalogue")) {
			try {
				handleUntrackedFiles(rm);
				
				try (Repository notebookRepo = rm.getRepository(NOTEBOOK_REPO_NAME)) {
				
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
				}
			} catch (Exception e) {
				setError(e.toString());
				return;
			}
		}
		
		else if (action.equals("reindex")) {
			try {
				handleUntrackedFiles(rm);
				
				try (Repository notebookRepo = rm.getRepository(NOTEBOOK_REPO_NAME)) {
				
					File repoDir = notebookRepo.getWorkTree();
					List<String> files = listFilesNewestFirst(repoDir);
	//				List<String> files = RepositoryManager.getRepositoryContents(notebookRepo);
					List<String> notebooks = files.stream().filter(f -> f.endsWith(".json")).collect(Collectors.toList());
					
					List<StoredNotebookSource> notebookSources = new ArrayList<>();
					for (String notebook : notebooks) {
						RevCommit rc = RepositoryManager.getMostRecentCommitForFile(notebookRepo, notebook);
						
						String notebookId = notebook.replaceFirst(".json$", "");
						
						try (Git git = new Git(notebookRepo)) {
							String notebookContents = RepositoryManager.getRepositoryFile(notebookRepo, notebook);
							
							Note note = git.notesShow().setObjectId(rc).call();
							ObjectLoader loader = notebookRepo.open(note.getData());
							String notebookMetadata = RepositoryManager.getStringFromObjectLoader(loader);
							
							notebookSources.add(new StoredNotebookSource(notebookId, notebookContents, notebookMetadata));
						} catch (IncorrectObjectTypeException | NullPointerException e) {
							System.out.println("no note for "+notebook);
							String notebookMetadata = getMetadataFromNotebook(rm, notebook.replaceFirst(".json$", ""));
							if (notebookMetadata != null) {
								rm.addNoteToCommit(NOTEBOOK_REPO_NAME, rc, notebookMetadata);
								String notebookContents = RepositoryManager.getRepositoryFile(notebookRepo, notebook);
								notebookSources.add(new StoredNotebookSource(notebookId, notebookContents, notebookMetadata));
							}
						}
					}
					indexNotebooks(notebookSources, false);
				}
			} catch (Exception e) {
				setError(e.toString());
				return;
			}
		}
	}
	
	private void doSave() throws IOException {
		
		if (isRequestAuthentic() == false) {
			setError("Inauthentic call.");
			return;
		}
		
		RepositoryManager rm = getRepositoryManager();
		
		String notebookData = parameters.getParameterValue("data","");
		if (notebookData.trim().isEmpty()) {
			setError("Notebook contains no data.");
			return;
		}
		
		String notebookMetadata = parameters.getParameterValue("metadata", "");
		if (notebookMetadata.trim().isEmpty()) {
			setError("Notebook contains no metadata.");
			return;
		}
		
		String userId = parameters.getParameterValue("spyral-id");
		if (userId == null) {
			setError("No Spyral account detected. Please sign in.");
			return;
		}
		
		JSONObject obj = (JSONObject) JSONValue.parse(notebookMetadata);
		String metadataUserId = (String) obj.get("userId");
		if (metadataUserId.equals(userId) == false) {
			setError("Notebook author does not match Spyral account.");
			return;
		}
		
		if (parameters.getParameterValue("name","").trim().isEmpty()==false) {
			String name = parameters.getParameterValue("name");
			if (name.matches(NOTEBOOK_NAME_TEMPLATE) == false) {
				setError("Notebook ID does not conform to template.");
				return;
			}
			id = userId + NOTEBOOK_ID_SEPARATOR + name;
		} else {
			while(true) {
				id = userId + NOTEBOOK_ID_SEPARATOR + RandomStringUtils.randomAlphanumeric(6);
				if (doesNotebookFileExist(rm, id+".json") == false) {
					break;
				}
			}
		}
		
		try {
			RevCommit commit = rm.addFile(NOTEBOOK_REPO_NAME, id+".json", notebookData);
			rm.addNoteToCommit(NOTEBOOK_REPO_NAME, commit, notebookMetadata);
		} catch (Exception e) {
			setError(e.toString());
			return;
		}
		
		indexNotebook(new StoredNotebookSource(id, notebookData, notebookMetadata), false);
	}
	
	private void doDelete() throws IOException {
		if (isRequestAuthentic() == false) {
			setError("Inauthentic call.");
			return;
		}
		
		String userId = parameters.getParameterValue("spyral-id");
		if (userId == null) {
			setError("No Spyral account detected. Please sign in.");
			return;
		}
		
		String notebookId = parameters.getParameterValue("id","");
		if (notebookId.trim().isEmpty()) {
			setError("No notebook ID specified!");
			return;
		}
		
		if (isNotebookIdAuthentic(notebookId) == false) {
			setError("Bad notebook ID!");
			return;
		}
		
		if (notebookId.startsWith(userId) == false) {
			setError("Not authorized!");
			return;
		}
		
		RepositoryManager rm = getRepositoryManager();
		if (doesNotebookFileExist(rm, notebookId+".json")) {
			try {
				rm.removeFile(NOTEBOOK_REPO_NAME, notebookId+".json");
			} catch (Exception e) {
				setError(e.toString());
				return;
			}
		}
		
		try {
			removeNotebookFromIndex(notebookId);
		} catch (IOException e) {
			setError(e.toString());
			return;
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
					try (Repository repo = repoManager.getRepository(NOTEBOOK_REPO_NAME);) {}
				} catch (RefNotFoundException e) {
					try (Git git = repoManager.setupRepository(NOTEBOOK_REPO_NAME)) {}
				}
			} catch (GitAPIException e) {
				throw new IOException(e.toString());
			}
		}
		return repoManager;
	}
	
	private boolean isNotebookIdAuthentic(String notebookId) {
		String[] parts = notebookId.split(NOTEBOOK_ID_SEPARATOR);
		if (parts.length == 1) return false;
		
		String[] usernameAndProvider = parts[0].split(USERNAME_SEPARATOR);
		if (usernameAndProvider.length == 1) return false;
		else {
			String username = usernameAndProvider[0];
			String provider = usernameAndProvider[1];
			if (provider.equals("gh")) {
				if (username.matches(GITHUB_USERNAME_TEMPLATE) == false) return false;
			} else {
				return false;
			}
		}
		
		String notebook = parts[1];
		if (notebook.matches(NOTEBOOK_NAME_TEMPLATE) == false) return false;
		
		return true;
	}
	
	// checks the key in the parameters to see if it matches the key in the local key file
	private boolean isRequestAuthentic() throws IOException {
		try(InputStream inputStream = this.getClass().getResourceAsStream(KEY_FILE_PATH)) {
			String key = IOUtils.readLines(inputStream, Charset.forName("UTF-8")).get(0).trim();
			return key.equals(parameters.getParameterValue("key"));
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean doesNotebookFileExist(RepositoryManager rm, String filename) {
		return rm.doesFileExist(NOTEBOOK_REPO_NAME, filename);
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
		try (Repository repo = rm.getRepository(NOTEBOOK_REPO_NAME)) {
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
	}
	
	private static String getMetadataFromNotebook(RepositoryManager rm, String notebookId) {
		String notebookJson;
		try {
			try (Repository repo = rm.getRepository(NOTEBOOK_REPO_NAME)) {
				notebookJson = RepositoryManager.getRepositoryFile(repo, notebookId+".json");
			}
		} catch (IOException | GitAPIException e) {
			return null;
		}
		
		StringBuilder metadata = new StringBuilder("{\"id\":\""+notebookId+"\"");
		
		JsonObject jobject = getJsonObjectForKey(notebookJson, "metadata");
		if (jobject != null) {
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
		}
		
		metadata.append("}");
		return metadata.toString();
	}
	
	private static JsonObject getJsonObjectForKey(String jsonString, String key) {
		try (JsonParser jsonParser = Json.createParser(new StringReader(jsonString))) {
			while(jsonParser.hasNext()) {
				Event e = jsonParser.next();
				if (e == Event.KEY_NAME) {
					String currKey = jsonParser.getString();
					jsonParser.next();
					
					if (currKey.equals(key)) {
						return jsonParser.getObject();
					}
				}
			}
		}
		return null;
	}
	
	private void setError(String errorString) {
		success = false;
		error = JSONValue.escape(errorString);
	}
	
	private void indexNotebook(StoredNotebookSource notebook, boolean useExecutor) throws IOException {
		List<StoredNotebookSource> notebooks = new ArrayList<StoredNotebookSource>();
		notebooks.add(notebook);
		indexNotebooks(notebooks, false);
	}
	
	private void indexNotebooks(List<StoredNotebookSource> notebooks, boolean useExecutor) throws IOException {
		IndexWriter indexWriter = storage.getNotebookLuceneManager().getIndexWriter(""); // note: do not close the indexWriter
		try (DirectoryReader indexReader = DirectoryReader.open(indexWriter)) {
			
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
	}
	
	private void removeNotebookFromIndex(String notebookId) throws IOException {
		IndexWriter indexWriter = storage.getNotebookLuceneManager().getIndexWriter(""); // note: do not close the indexWriter
		try (DirectoryReader indexReader = DirectoryReader.open(indexWriter)) {
			
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			Term notebookIdTerm = new Term("id", notebookId);
			TopDocs topDocs = indexSearcher.search(new TermQuery(notebookIdTerm), 1);
			if (topDocs.totalHits.value>0) {
				indexWriter.deleteDocuments(notebookIdTerm);
			} else {
				throw new IOException("Unable to remove notebook from index. Notebook not found: "+notebookId);
			}
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
				if (topDocs.totalHits.value>0) {
					alreadyIndexed = true;
					System.out.println("Re-Indexing: "+notebookId);
				} else {
					System.out.println("Indexing: "+notebookId);
				}
				
				org.apache.lucene.document.Document document = new org.apache.lucene.document.Document();
				
				document.add(new StringField("id", notebookId, Field.Store.YES));
				
				try (JsonParser jsonParser = Json.createParser(new StringReader(notebookContents))) {
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
				}
				
				FacetsConfig config = new FacetsConfig();
				
				try (JsonParser jsonParser = Json.createParser(new StringReader(notebookMetadata))) {
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
								case TRUE:
									document.add(new SortedSetDocValuesFacetField(facet, "true"));
									if (doIndex) {
										System.out.println("adding: "+key+", true");
										document.add(new StringField(key, "true", Field.Store.YES));
									}
									break;
								case FALSE:
									document.add(new SortedSetDocValuesFacetField(facet, "false"));
									if (doIndex) {
										System.out.println("adding: "+key+", false");
										document.add(new StringField(key, "false", Field.Store.YES));
									}
									break;
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
