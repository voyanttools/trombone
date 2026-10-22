package org.voyanttools.trombone.storage.memory;

import java.io.IOException;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.voyanttools.trombone.storage.DirectoryFactory;

public class MemoryDirectoryFactory implements DirectoryFactory {

	@Override
	public Directory getDirectory(String corpus) throws IOException {
		return new ByteBuffersDirectory();
	}

}
