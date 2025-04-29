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
import java.util.Calendar;
import java.util.List;

import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("stepEnabledCorpusCreator")
public class RealCorpusCreator extends AbstractTool {

	private String nextCorpusCreatorStep = "";
	
	private String storedId;
	
	public RealCorpusCreator(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		run(parameters.getParameterIntValue("steps", Integer.MAX_VALUE));
	}
	
	public void run(int steps) throws IOException {
		nextCorpusCreatorStep = parameters.getParameterValue("nextCorpusCreatorStep", "store");
		int timeout = parameters.getParameterIntValue("timeoutSeconds");
		long start = Calendar.getInstance().getTimeInMillis();
		int step = 0;
		
		// this is used to go from one step to the next in a single pass, without needing to deal with storedId
		List<StoredDocumentSource> storedDocumentSources = null;
		
		String initialStoredId = null;
		String expandId = null;
		
		log("nextCorpusCreatorStep: "+nextCorpusCreatorStep);
		log("steps: "+steps);
		
		if (nextCorpusCreatorStep.equals("store")) {
			DocumentStorer storer = new DocumentStorer(storage, parameters);
			storer.run();
			storedDocumentSources = storer.getStoredDocumentSources();
			storedId = storer.getStoredId();
			initialStoredId = storedId;
			log(step+") storing: "+storedId);
			nextCorpusCreatorStep = "expand";
			if (timeout>0 && Calendar.getInstance().getTimeInMillis()-start>timeout) {return;}
			if (steps>0 && ++step>=steps) {return;}
		}
		
		if (nextCorpusCreatorStep.equals("expand")) {
			DocumentExpander expander = new DocumentExpander(storage, parameters);
			if (storedDocumentSources==null) {expander.run();}
			else {expander.run(storedDocumentSources);}
			storedDocumentSources = expander.getStoredDocumentSources();
			storedId = expander.getStoredId();
			expandId = storedId;
			log(step+") expanding: "+storedId);
			nextCorpusCreatorStep = "extract";
			if (timeout>0 && Calendar.getInstance().getTimeInMillis()-start>timeout) {return;}
			if (steps>0 && ++step>=steps) {return;}
		}
		
		if (nextCorpusCreatorStep.equals("extract")) {
			DocumentExtractor extractor = new DocumentExtractor(storage,parameters);
			if (storedDocumentSources==null) {extractor.run();}
			else {extractor.run(storedDocumentSources);}
			storedDocumentSources = extractor.getStoredDocumentSources();
			storedId = extractor.getStoredId();
			log(step+") extracting: "+storedId);
			nextCorpusCreatorStep = "index";
			if (timeout>0 && Calendar.getInstance().getTimeInMillis()-start>timeout) {return;}
			if (steps>0 && ++step>=steps) {return;}
		}
		
//		cleanup(initialStoredId);
//		cleanup(expandId);
		
		if (nextCorpusCreatorStep.equals("index")) {
			DocumentIndexer indexer = new DocumentIndexer(storage, parameters);
			if (storedDocumentSources==null) {indexer.run();}
			else {indexer.run(storedDocumentSources);}
			storedDocumentSources = indexer.getStoredDocumentSources();
			storedId = indexer.getStoredId();
			log(step+") indexing: "+storedId);
			nextCorpusCreatorStep = "corpus";
			if (timeout>0 && Calendar.getInstance().getTimeInMillis()-start>timeout) {return;}
			if (steps>0 && ++step>=steps) {return;}
		}
		
		if (nextCorpusCreatorStep.equals("corpus")) {
			CorpusBuilder builder = new CorpusBuilder(storage, parameters);
			if (storedDocumentSources==null) {builder.run();}
			else {builder.run(storedId, storedDocumentSources);}
//			storedDocumentSources = builder.getStoredDocumentSources();
			storedId = builder.getStoredId();
			log(step+") corpus: "+storedId);
			nextCorpusCreatorStep = "done";
		}
	}
	
	private void cleanup(String id) {
		if (id == null) return;
		try {
			log("cleaning: "+id);
			List<String> docIds = storage.retrieveStrings(id, Storage.Location.object);
			for (String docId : docIds) {
				log("    removing doc: "+docId);
				storage.getStoredDocumentSourceStorage().deleteStoredDocumentSource(docId);
			}
			if (storage instanceof FileStorage) {
				log("    removing resource: "+id);
				((FileStorage)storage).deleteResourceFile(id, Storage.Location.object);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getNextCorpusCreatorStep() {
		return nextCorpusCreatorStep;
	}
	
	public String getStoredId() {
		return storedId;
	}


}
