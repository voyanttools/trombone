/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class JsonExpander implements Expander {
	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;

	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * 
	 */
	public JsonExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	@Override
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource)
			throws IOException {
		List<StoredDocumentSource> childStoredDocumentSources = new ArrayList<StoredDocumentSource>();

		String documentsPointer = parameters.getParameterValue("jsonDocumentsPointer", "");
		// if this query doesn't exist than it's all one document and will be handled by extractor
		if (documentsPointer.trim().isEmpty()) {
			childStoredDocumentSources.add(storedDocumentSource);
			return childStoredDocumentSources;
		}
		
		// load line
		InputStream is = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
		JsonReader jsonReader = Json.createReader(is);
		
		JsonStructure jsonStructure = jsonReader.read();

		JsonValue jsonValue;
		try {
			jsonValue = jsonStructure.getValue(documentsPointer);
		} catch (JsonException e) {
			throw new IOException("Unable to find the specified jsonDocumentsPointer: "+documentsPointer+" for document "+storedDocumentSource.getMetadata().getLocation());
		}
		
		List<String> strings = new ArrayList<String>();
		ValueType jsonValueType = jsonValue.getValueType();
		if (jsonValueType==ValueType.ARRAY) {
			JsonArray jsonArray = jsonValue.asJsonArray();
			jsonArray.forEach((Consumer<? super JsonValue>) s -> strings.add(s.toString()));
		} else {
			strings.add(jsonValue.toString()); // this could be a string or something else
		}
		
		/* groupBy is tricky because documents are combined in ways that may conflict 
		 * with the other extraction options (especially since Pointer expects absolute
		 * addresses from root). So for now we'll skip.
		 
		String groupByPointer = parameters.getParameterValue("jsonGroupByPointer", "");
		if (groupByPointer.isEmpty()==false) {
			Map<String, List<String>> stringsMap = new HashMap<String, List<String>>();
			for (String string : strings) {
				 JsonReader jsonStringReader = Json.createReader(new StringReader(string));
				 JsonStructure jsonStringStructure = jsonStringReader.read();
				 JsonValue jsonStringValue;
				 try {
					 jsonStringValue = jsonStringStructure.getValue(groupByPointer);					 
				 } catch (JsonException e) {
					throw new IOException("Unable to find the specified jsonGroupByPointer: "+groupByPointer+" for document "+string.substring(0, 30)+"…"); 
				 }
				 String stringValue = jsonStringValue.toString(); // doesn't matter if it has quotes, etc.
				 if (stringsMap.containsKey(stringValue)==false) {
					 stringsMap.put(stringValue, new ArrayList<String>());
				 }
				 stringsMap.get(stringValue).add(string);
			}
			
			strings.clear();
			for (Map.Entry<String, List<String>> entry : stringsMap.entrySet()) {
				strings.add(StringUtils.join(entry.getValue(),"\n"));
			}
		}
		*/
		
		DocumentMetadata parentMetadata = storedDocumentSource.getMetadata();
		String parentId = storedDocumentSource.getId();
		int counter = 0;
		for (String string : strings) {
			String location = documentsPointer+" ("+counter+")";
			DocumentMetadata metadata = parentMetadata.asParent(parentId, DocumentMetadata.ParentType.EXPANSION);
			metadata.setModified(parentMetadata.getModified());
			metadata.setSource(Source.STRING);
			metadata.setLocation(location);
			metadata.setDocumentFormat(DocumentFormat.JSON);
			String id = DigestUtils.md5Hex(parentId + location);
			InputSource inputSource = new StringInputSource(id, metadata, string);
			StoredDocumentSource storedChildDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
			childStoredDocumentSources.add(storedChildDocumentSource);
			counter++;
		}
		
		return childStoredDocumentSources;
	}

}
