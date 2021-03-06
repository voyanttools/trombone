package org.voyanttools.trombone.storage.file;

import java.io.File;
import java.io.IOException;

import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;

public abstract class AbstractFileMigrator implements FileMigrator {
	
	protected FileStorage storage;
	
	protected String id;
	
	protected AbstractFileMigrator(FileStorage storage, String id) {
		this.storage = storage;
		this.id = id.trim();
	}

	@Override
	public String getMigratedCorpusId() throws IOException {
		
		if (!corpusExists()) {return null;}
		
		String storedId = transferDocuments();
		
		FlexibleParameters parameters = getCorpusCreationParameters();
		
		return getNewCorpusId(storedId, parameters);
		
	}

	@Override
	public boolean corpusExists() {
		if (id==null || id.isEmpty() || !getSourceTromboneCorpusDirectory().exists()) {return false;}
		try {
			String[] ids = getDocumentIds();
			return ids.length>0;
		}
		catch (Exception IOException) {
			return false;
		}
	}

	protected String transferDocuments() throws IOException {
		
		String[] ids = getDocumentIds();
		
		return getStoredDocumentsId(ids);
		
	}
	
	protected String getNewCorpusId(String storedId, FlexibleParameters parameters) throws IOException {
		parameters.setParameter("nextCorpusCreatorStep", "extract"); // I *think* we can skip expand at the document level
		parameters.setParameter("storedId", storedId);
		RealCorpusCreator realCorpusCreator = new RealCorpusCreator(storage, parameters);
		realCorpusCreator.run();
		return realCorpusCreator.getStoredId();
	}

	
	protected File getRootDataDirectory() {
		return storage.storageLocation.getParentFile();
	}

	// this is package level for testing
	File getSourceTromboneDirectory() {
		return new File(getRootDataDirectory(), getSourceTromboneDirectoryName());
	}

	protected abstract File getSourceTromboneCorpusDirectory();
	
	protected abstract String getSourceTromboneDirectoryName();
	
	protected abstract String[] getDocumentIds() throws IOException;
	
	protected abstract String getStoredDocumentsId(String[] ids) throws IOException;
	
	protected abstract FlexibleParameters getCorpusCreationParameters() throws IOException;
	
}
