/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.tool.build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.voyanttools.trombone.input.index.Indexer;
import org.voyanttools.trombone.input.index.LuceneIndexer;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
class DocumentIndexer extends AbstractTool {

	private String storedId = null;
	
	@XStreamOmitField
	private List<StoredDocumentSource> indexableStoredDocumentSources = null;
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentIndexer(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		String sid = parameters.getParameterValue("storedId");
		List<String> ids = storage.retrieveStrings(sid, Storage.Location.object);
		StoredDocumentSourceStorage storedDocumentStorage = storage.getStoredDocumentSourceStorage();
		List<StoredDocumentSource> indexableStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		for (String id : ids) {
			DocumentMetadata metadata = storedDocumentStorage.getStoredDocumentSourceMetadata(id);
			StoredDocumentSource storedDocumentSource = new StoredDocumentSource(id, metadata);
			indexableStoredDocumentSources.add(storedDocumentSource);
		}
		run(indexableStoredDocumentSources);
	}
	
	void run(List<StoredDocumentSource> indexableStoredDocumentSources) throws IOException {
		
		Calendar start = Calendar.getInstance();
		log("Starting document indexing.");

		this.indexableStoredDocumentSources = indexableStoredDocumentSources;
		Indexer indexer = new LuceneIndexer(storage, parameters);
		storedId = indexer.index(indexableStoredDocumentSources);
		
		log("Finished indexing of "+indexableStoredDocumentSources.size()+" documents.", start);

		
	}

	List<StoredDocumentSource> getStoredDocumentSources() {
		return indexableStoredDocumentSources;
	}

	String getStoredId() {
		return storedId;
	}

}
