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

	/**
	 * @throws IOException 
	 * 
	 */
	public ToolSerializer(FlexibleParameters parameters, RunnableTool runnableTool) {
		this.parameters = parameters;
		this.runnableTool = runnableTool;
	}
	
	public void run(Writer writer) throws IOException {
		
		if (this.runnableTool instanceof ToolRunner) {
			List<RunnableTool> tools = ((ToolRunner) runnableTool).getRunnableToolResults();
			if (tools.size()==1 && tools.get(0) instanceof RawSerializable) {
				((RawSerializable) tools.get(0)).serialize(writer);
				return;
			}
		}
		
		XStream xs = null;
		final Writer fileWriter = parameters.containsKey("outputFile") ? new FileWriter(parameters.getParameterValue("outputFile")) : null;
		if (parameters.getParameterValue("outputFormat", "").equals("xml")) {
			xs = new XStream();
		}
		else {
			xs = new XStream(new JsonHierarchicalStreamDriver() {
				@Override
				public HierarchicalStreamWriter createWriter(Writer writer) {
					return new JsonWriter(fileWriter!=null ? fileWriter : writer, JsonWriter.DROP_ROOT_MODE);
				}
			});
		}
		if (xs == null) return; // don't serialize results, therefore no output data is emitted
	
		// xstream whitelist
		xs.addPermission(NoTypePermission.NONE);
		xs.addPermission(NullPermission.NULL);
		xs.addPermission(PrimitiveTypePermission.PRIMITIVES);
		xs.allowTypeHierarchy(Collection.class);
		xs.allowTypeHierarchy(Map.class);
		xs.allowTypes(new Class[] { String.class });
		xs.allowTypesByWildcard(new String[] {"org.voyanttools.trombone.**"});
		
		xs.autodetectAnnotations(true);
//		xs.setMode(XStream.NO_REFERENCES);
		xs.toXML(runnableTool, fileWriter!=null ? fileWriter : writer);
		if (fileWriter!=null) {
			fileWriter.close();
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
