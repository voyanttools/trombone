package org.voyanttools.trombone.lucene;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.voyanttools.trombone.lucene.analysis.KitchenSinkPerFieldAnalyzerWrapper;
import org.voyanttools.trombone.storage.DirectoryFactory;
import org.voyanttools.trombone.storage.Storage;

public class NotebookLuceneManager extends AbstractLuceneManager {

	private Directory directory;
	
	private DirectoryReader directoryReader = null;
	
	private IndexWriter indexWriter = null;
	
	private IndexSearcher indexSearcher = null;
	
	private Analyzer analyzer;
	
	private Storage storage;
	
	private DirectoryFactory directoryFactory;
	
	public NotebookLuceneManager(Storage storage, DirectoryFactory directoryFactory) throws IOException {
		super();
		this.storage = storage;
		this.directoryFactory = directoryFactory;
		analyzer = new KitchenSinkPerFieldAnalyzerWrapper(storage);
	}
	
	private Directory getDirectory(String corpus) throws IOException {
		if (directory==null) {
			directory = directoryFactory.getDirectory(corpus);
			access();
		}
		return directory;
	}
	
	@Override
	public DirectoryReader getDirectoryReader(String corpus) throws CorruptIndexException, IOException {
		if (directoryReader == null) {
			directoryReader = DirectoryReader.open(getDirectory(corpus));
		} else {
			// get a new reader if the index has changed
			DirectoryReader newReader = DirectoryReader.openIfChanged(directoryReader);
			if (newReader != null) {
				directoryReader = newReader;
			}
		}
		access();
		return directoryReader;
	}

	@Override
	public void addDocument(String corpus, Document document) throws CorruptIndexException, IOException {
		IndexWriter writer = getIndexWriter(corpus);
		writer.addDocument(document);
		writer.commit();
		setDirectoryReader(corpus, DirectoryReader.open(writer));
	}

	@Override
	public synchronized IndexWriter getIndexWriter(String corpus)
			throws CorruptIndexException, LockObtainFailedException, IOException {
		if (indexWriter==null) {
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			indexWriter = new IndexWriter(getDirectory(corpus), new IndexWriterConfig(analyzer));
		}
		access();
		return indexWriter;
	}

	@Override
	public Analyzer getAnalyzer(String corpus) {
		return analyzer;
	}

	@Override
	public boolean directoryExists(String corpus) throws IOException {
		return DirectoryReader.indexExists(getDirectory(corpus));
	}

	@Override
	public void setDirectoryReader(String corpus, DirectoryReader indexReader) {
		this.directoryReader = indexReader;
		this.indexSearcher = new IndexSearcher(directoryReader);
		access();
	}

	@Override
	public void close(String corpus) throws IOException {
		System.out.println("closing notebook lucene manager");
		try {
			if (indexWriter != null) {
				indexWriter.close();
				indexWriter = null;
			}
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw e;
			} else {
				throw new IOException("Error closing index writer: "+corpus, e);
			}
		}
	}

	@Override
	public void closeAll() throws IOException {
		close(RandomStringUtils.randomAlphabetic(10));
	}
}
