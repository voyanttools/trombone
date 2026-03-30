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

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

/**
 * @author sgs
 *
 */
public class ToolSerializer implements RunnableTool {
	
	private FlexibleParameters parameters;
	
	private RunnableTool runnableTool;
	
	private static final int VERSION = 1;

	private static final XStream XML_XSTREAM;
	private static final XStream JSON_XSTREAM;

	static {
		XML_XSTREAM = new XStream();
		XML_XSTREAM.setClassLoader(ToolSerializer.class.getClassLoader());
		secureXStream(XML_XSTREAM);
		processAnnotations(XML_XSTREAM);
		
		JSON_XSTREAM = new XStream(new JsonHierarchicalStreamDriver() {
			@Override
			public HierarchicalStreamWriter createWriter(Writer writer) {
				return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
			}
		});
		JSON_XSTREAM.setClassLoader(ToolSerializer.class.getClassLoader());
		secureXStream(JSON_XSTREAM);
		processAnnotations(JSON_XSTREAM);
		
	}
	public static XStream getXMLXStream() {
		return XML_XSTREAM;
	}
	public static XStream getJSONXStream() {
		return JSON_XSTREAM;
	}
	private static void processAnnotations(XStream xStream) {
		// xStream.autodetectAnnotations(true); // not thread safe
		xStream.processAnnotations(new Class[] {
			org.voyanttools.trombone.model.CorpusCollocate.class,
			org.voyanttools.trombone.model.CorpusEntity.class,
			org.voyanttools.trombone.model.CorpusNgram.class,
			org.voyanttools.trombone.model.DocumentNgram.class,
			org.voyanttools.trombone.model.IndexedDocument.class,
			org.voyanttools.trombone.model.Keywords.class,
			org.voyanttools.trombone.model.Kwic.class,
			org.voyanttools.trombone.tool.TableCorrelations.class,
			org.voyanttools.trombone.tool.TableManager.class,
			org.voyanttools.trombone.tool.analysis.TopicModeling.class,
			org.voyanttools.trombone.tool.analysis.CA.class,
			org.voyanttools.trombone.tool.analysis.PCA.class,
			org.voyanttools.trombone.tool.analysis.TSNE.class,
			org.voyanttools.trombone.tool.build.DocumentExpander.class,
			org.voyanttools.trombone.tool.build.DocumentExtractor.class,
			org.voyanttools.trombone.tool.build.DocumentStorer.class,
			org.voyanttools.trombone.tool.build.RealCorpusCreator.class,
			org.voyanttools.trombone.tool.corpus.CorpusCollocates.class,
			org.voyanttools.trombone.tool.corpus.CorpusCreator.class,
			org.voyanttools.trombone.tool.corpus.CorpusEntities.class,
			org.voyanttools.trombone.tool.corpus.CorpusFacets.class,
			org.voyanttools.trombone.tool.corpus.CorpusMetadata.class,
			org.voyanttools.trombone.tool.corpus.CorpusNgrams.class,
			org.voyanttools.trombone.tool.corpus.CorpusSegmentTerms.class,
			org.voyanttools.trombone.tool.corpus.CorpusTermCorrelations.class,
			org.voyanttools.trombone.tool.corpus.CorpusTexts.class,
			org.voyanttools.trombone.tool.corpus.DocumentAutomatedReadabilityIndex.class,
			org.voyanttools.trombone.tool.corpus.DocumentColemanLiauIndex.class,
			org.voyanttools.trombone.tool.corpus.DocumentCollocates.class,
			org.voyanttools.trombone.tool.corpus.DocumentDaleChallIndex.class,
			org.voyanttools.trombone.tool.corpus.DocumentFOGIndex.class,
			org.voyanttools.trombone.tool.corpus.DocumentLIXIndex.class,
			org.voyanttools.trombone.tool.corpus.DocumentSMOGIndex.class,
			org.voyanttools.trombone.tool.corpus.DocumentSimilarity.class,
			org.voyanttools.trombone.tool.corpus.DocumentTermCorrelations.class,
			org.voyanttools.trombone.tool.corpus.DocumentsMetadata.class,
			org.voyanttools.trombone.tool.corpus.Dreamscape.class,
			org.voyanttools.trombone.tool.corpus.EntityCollocationsGraph.class,
			org.voyanttools.trombone.tool.corpus.Geonames.class,
			org.voyanttools.trombone.tool.corpus.PCA.class,
			org.voyanttools.trombone.tool.corpus.SemanticGraph.class,
			org.voyanttools.trombone.tool.corpus.TSNE.class,
			org.voyanttools.trombone.tool.corpus.Veliza.class,
			org.voyanttools.trombone.tool.corpus.CA.class,
			org.voyanttools.trombone.tool.corpus.CorpusManager.class,
			org.voyanttools.trombone.tool.corpus.CorpusTerms.class,
			org.voyanttools.trombone.tool.corpus.CorpusVectors.class,
			org.voyanttools.trombone.tool.corpus.DocumentContexts.class,
			org.voyanttools.trombone.tool.corpus.DocumentEntities.class,
			org.voyanttools.trombone.tool.corpus.DocumentNgrams.class,
			org.voyanttools.trombone.tool.corpus.DocumentTerms.class,
			org.voyanttools.trombone.tool.corpus.DocumentTokens.class,
			org.voyanttools.trombone.tool.corpus.DocumentsFinder.class,
			org.voyanttools.trombone.tool.notebook.CatalogueFacets.class,
			org.voyanttools.trombone.tool.notebook.GitNotebookManager.class,
			org.voyanttools.trombone.tool.notebook.NotebookFinder.class,
			org.voyanttools.trombone.tool.notebook.NotebookManager.class,
			org.voyanttools.trombone.tool.progress.ProgressMonitor.class,
			org.voyanttools.trombone.tool.resource.KeywordsManager.class,
			org.voyanttools.trombone.tool.resource.StoredCategories.class,
			org.voyanttools.trombone.tool.resource.StoredResource.class,
			org.voyanttools.trombone.tool.table.PCA.class,
			org.voyanttools.trombone.tool.table.TSNE.class,
			org.voyanttools.trombone.tool.table.CA.class,
			org.voyanttools.trombone.tool.util.Geonames.class,
			org.voyanttools.trombone.tool.util.Message.class,
			org.voyanttools.trombone.tool.util.ToolRunner.class,
			org.voyanttools.trombone.util.FlexibleParameters.class
		});
	}

	/**
	 * @throws IOException 
	 * 
	 */
	public ToolSerializer(FlexibleParameters parameters, RunnableTool runnableTool) {
		this.parameters = parameters;
		this.runnableTool = runnableTool;
	}
	
	public static XStream secureXStream(XStream xs) {
		// clear all
		xs.addPermission(NoTypePermission.NONE);
		// allow basics
		xs.addPermission(NullPermission.NULL);
		xs.addPermission(PrimitiveTypePermission.PRIMITIVES);
		// allow common types
		xs.allowTypeHierarchy(Collection.class);
		xs.allowTypeHierarchy(Map.class);
		xs.allowTypeHierarchy(String.class);
		// allow trombone classes
		xs.allowTypesByWildcard(new String[] {"org.voyanttools.trombone.**"});
		
		return xs;
	}

	public void run(Writer writer) throws IOException {
		
		if (this.runnableTool instanceof ToolRunner) {
			List<RunnableTool> tools = ((ToolRunner) runnableTool).getRunnableToolResults();
			if (tools.size()==1 && tools.get(0) instanceof RawSerializable) {
				((RawSerializable) tools.get(0)).serialize(writer);
				return;
			}
		}
		
		if (parameters.getParameterValue("outputFormat", "").equals("xml")) {
			if (parameters.containsKey("outputFile")) {
				try (FileWriter fileWriter = new FileWriter(parameters.getParameterValue("outputFile"))) {
					getXMLXStream().toXML(runnableTool, fileWriter);
				} catch (IOException e) {
				}
			} else {
				getXMLXStream().toXML(runnableTool, writer);
			}
		} else {
			if (parameters.containsKey("outputFile")) {
				throw new IOException("Can only output XML to a file");
			}
			getJSONXStream().toXML(runnableTool, writer);
		}
	}

	public void run() throws IOException {
		if (parameters.containsKey("outputFile")) {
			Writer writer = new FileWriter(parameters.getParameterValue("outputFile"));
			run(writer);
			writer.close();
		}
		else {
			Writer writer = new OutputStreamWriter(System.out);
			run(writer);
			writer.close();
		}
	}
	
	public static void startNode(HierarchicalStreamWriter writer, String nodeName, Class<?> clazz) {
		if (writer.underlyingWriter() instanceof JsonWriter) {
			((JsonWriter)writer.underlyingWriter()).startNode(nodeName, clazz);
		} else {
			writer.startNode(nodeName);
		}
	}
	
	public static void endNode(HierarchicalStreamWriter writer) {
		if (writer.underlyingWriter() instanceof JsonWriter) {
			((JsonWriter)writer.underlyingWriter()).endNode();
		} else {
			writer.endNode();
		}
	}
	
	public static void setNumericNode(HierarchicalStreamWriter writer, String nodeName, float value) {
		startNode(writer, nodeName, Float.class);
		if (Float.isNaN(value) || Float.isInfinite(value)) {
			writer.setValue("null");
		} else {
			writer.setValue(String.valueOf(value));
		}
		endNode(writer);
	}
	
	public static void setNumericNode(HierarchicalStreamWriter writer, String nodeName, double value) {
		setNumericNode(writer, nodeName, (float)value);
	}
	
	public static void setNumericNode(HierarchicalStreamWriter writer, String nodeName, int value) {
		startNode(writer, nodeName, Integer.class);
		writer.setValue(String.valueOf(value));
		endNode(writer);
	}
	
	public static void setNumericList(HierarchicalStreamWriter writer, String nodeName, float[] list) {
		startNode(writer, nodeName, List.class);
		for (float value : list) {
			setNumericNode(writer, "float", value);
		}
		endNode(writer);
	}
	
	public static void setNumericList(HierarchicalStreamWriter writer, String nodeName, double[] list) {
		startNode(writer, nodeName, List.class);
		for (double value : list) {
			setNumericNode(writer, "float", value);
		}
		endNode(writer);
	}
	
	public static void setNumericList(HierarchicalStreamWriter writer, String nodeName, int[] list) {
		startNode(writer, nodeName, List.class);
		for (int value : list) {
			setNumericNode(writer, "int", value);
		}
		endNode(writer);
	}

	@Override
	public float getVersion() {
		return VERSION;
	}
}
