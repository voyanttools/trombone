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
package org.voyanttools.trombone.tool.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.voyanttools.trombone.input.source.InputSourcesBuilder;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.ToolFactory;
import org.voyanttools.trombone.tool.corpus.CorpusExporter;
import org.voyanttools.trombone.tool.corpus.CorpusMetadata;
import org.voyanttools.trombone.tool.progress.Progressable;
import org.voyanttools.trombone.tool.resource.StoredResource;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author sgs
 *
 */
@XStreamAlias("results")
public class ToolRunner extends AbstractTool {
	
	private String version;
	private String voyantVersion;
	private String voyantBuild;
	private long duration;
	
	@XStreamOmitField
	private Writer writer = null;
	
	@XStreamOmitField
	private OutputStream outputStream = null;
	
	@XStreamImplicit
	List<RunnableTool> results = new ArrayList<RunnableTool>();
	
	/**
	 * 
	 */
	public ToolRunner(Storage storage, FlexibleParameters parameters, Writer writer) {
		super(storage, parameters);
		this.version = Float.toString(getVersion());
		this.writer = writer;
	}
	
	public ToolRunner(Storage storage, FlexibleParameters parameters, OutputStream outputStream) {
		super(storage, parameters);
		this.outputStream = outputStream;
	}

	public void run() throws IOException {

		ToolFactory toolFactory = new ToolFactory(storage, parameters);
		toolFactory.run();
		List<RunnableTool> tools = toolFactory.getRunnableTools();
		
		// handle alternative tools
		if (tools.size()==1) {
			RunnableTool tool = tools.get(0);
			if (tool instanceof CorpusExporter) {
				if (outputStream==null) {
					throw new IllegalArgumentException("The CorpusExporter tool requires the outputFormat=zip parameter to be set (or otherwise an OutputStream to be used instead of a Writer).");
				}
				((CorpusExporter) tool).run(outputStream); // make sure we check for ConsumptiveTool
				return;
			}
		}
		
		StringBuilder sb = new StringBuilder("cache-ToolRunner-").append(getVersion());
		for (RunnableTool tool : tools) {
			sb.append("-").append(tool.getClass().getSimpleName()).append(tool.getVersion());
		}
		sb.append("-");
		if (parameters.containsKey("corpus")) { // add corpus to make it easier to find
			sb.append(parameters.getParameterValue("corpus")).append("-");
		}
		List<String> names = new ArrayList<String>(parameters.getKeys());
		Collections.sort(names);
		StringBuilder paramsBuilder = new StringBuilder();
		for (String name : names) {
			if (name.startsWith("_dc")==false) {
				paramsBuilder.append(name).append(StringUtils.join(parameters.getParameterValues(name)));
			}
		}
		sb.append(DigestUtils.md5Hex(paramsBuilder.toString()));
		String id = sb.toString();
		
		boolean hasParameterSources = InputSourcesBuilder.hasParameterSources(parameters);
		// skip for corpus (makes it easier to change or remove) and stored resource (cacheing not relevant, easier to change)
		boolean noCache = parameters.getParameterBooleanValue("useCache")==false && tools.size()==1 && (tools.get(0) instanceof CorpusMetadata || tools.get(0) instanceof StoredResource);
		if (noCache==false && parameters.getParameterBooleanValue("noCache")==false && parameters.getParameterBooleanValue("reCache")==false && hasParameterSources==false && storage.isStored(id, Storage.Location.cache)) {
			Reader reader = storage.getStoreReader(id, Storage.Location.cache);
			IOUtils.copy(reader, writer);
			reader.close();
			writer.flush();
		}
		else {
			long start = Calendar.getInstance().getTimeInMillis();
			boolean hasProgress = false;
			for (RunnableTool tool : tools) {
				tool.run();
				results.add(tool);
				if (tool instanceof Progressable && ((Progressable) tool).getProgress()!=null) {
					hasProgress = true;
				}
			}
			duration = Calendar.getInstance().getTimeInMillis() - start;
			voyantVersion = parameters.getParameterValue("VOYANT_VERSION", "");
			voyantBuild = parameters.getParameterValue("VOYANT_BUILD", "");

			ToolSerializer toolSerializer = new ToolSerializer(parameters, this);
			if (parameters.getParameterBooleanValue("noCache") || hasParameterSources==true || hasProgress) { // use the configured writer directly
				toolSerializer.run(writer); 
			}
			else { // try to cache
				Writer cacheWriter = storage.getStoreWriter(id, Storage.Location.cache);
				toolSerializer.run(cacheWriter);
				cacheWriter.close();
				Reader reader = storage.getStoreReader(id, Storage.Location.cache);
				IOUtils.copy(reader, writer); // now write from cache
				reader.close();
				writer.flush();
			}
		}
		
	}
	

	
	public List<RunnableTool> getRunnableToolResults() {
		return results;
	}


}
