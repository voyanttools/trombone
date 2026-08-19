/**
 * 
 */
package org.voyanttools.trombone.input.extract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class JsonFeaturesExtractor implements Extractor {
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private FlexibleParameters parameters;

	/**
	 * 
	 */
	public JsonFeaturesExtractor(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.extract.Extractor#getExtractableInputSource(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public InputSource getExtractableInputSource(StoredDocumentSource storedDocumentSource) throws IOException {
		StringBuilder id = new StringBuilder(storedDocumentSource.getId()).append("jsonfeatures-extracted");
		// add any other parameters
		return new ExtractableJsonFeaturesInputSource(DigestUtils.md5Hex(id.toString()), storedDocumentSource);
	}

	private class ExtractableJsonFeaturesInputSource implements InputSource {
		
		private String id;
		private StoredDocumentSource storedDocumentSource;
		private DocumentMetadata metadata;
		private boolean isProcessed = false;
		
		ExtractableJsonFeaturesInputSource(String id, StoredDocumentSource storedDocumentSource) {
			this.id = id;
			this.storedDocumentSource = storedDocumentSource;
			this.metadata = storedDocumentSource.getMetadata();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			
			// load line
			InputStream is = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
			String jsonString = IOUtils.toString(is, this.metadata.getEncoding());
			
			// parse doc
			JsonObject jsonObject;
			try {
				jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
			} catch (JsonSyntaxException | IllegalStateException e) {
				throw new IOException("Unable to parse JSON features "+storedDocumentSource.getId()+" ("+storedDocumentSource.getMetadata().getLocation()+")");
			}
			
	        List<String> words = new ArrayList<String>();

			JsonObject features = jsonObject.getAsJsonObject("features");
			if (features!=null) {
	        		        
				JsonArray pages = features.getAsJsonArray("pages");
				for (JsonElement pageEl : pages) {
					JsonObject page = pageEl.getAsJsonObject();
					JsonObject body = page.getAsJsonObject("body");
					if (body==null) {continue;}
					JsonObject tokenPosCount = body.getAsJsonObject("tokenPosCount");
		        	Set<String> terms = tokenPosCount.keySet();
		        	for (String term : terms) {
						if (term.isEmpty() || !Character.isLetter(term.charAt(0))) {continue;} // skip if not starting with alphabetic
						JsonObject posCounts = tokenPosCount.getAsJsonObject(term);
						for (String pos : posCounts.keySet()) {
							int count = posCounts.get(pos).getAsInt();
							for (int i=0; i<count; i++) {
		        				words.add(term);
		        			}
		        		}
		        	}
		        }
	        }
	        
			JsonObject metadataObj = jsonObject.getAsJsonObject("metadata");
			
			if (metadataObj!=null) {
				
				String title = metadataObj.has("name") && !metadataObj.get("name").isJsonNull() ? metadataObj.get("name").getAsString() : null;
				if (title==null) {
					title = metadataObj.has("title") && !metadataObj.get("title").isJsonNull() ? metadataObj.get("title").getAsString() : null;
				}
				if (title!=null) {
					metadata.setTitle(title);
				}
				
				// loop array
				Set<String> authors = new HashSet<String>();
				JsonElement contributor = metadataObj.get("contributor");
				if (contributor!=null && !contributor.isJsonNull()) {
					setNamesFromContributor(contributor, authors);
				}
				if (authors.isEmpty()==false) {
					metadata.setAuthors(authors.toArray(new String[0]));
				}
			}
			
	    	Collections.shuffle(words); // we re-hydrate words but in a plausible order to avoid nasty ngrams
	    	String string = String.join(" ", words);
	    	isProcessed = true;
	    	
	    	return new ByteArrayInputStream(string.getBytes(metadata.getEncoding()));
	    }
		
		private void setNamesFromContributor(JsonElement object, Set<String> names) {
			if (object.isJsonObject()) {
				setNamesFromName(object.getAsJsonObject().get("name"), names);
			} else if (object.isJsonArray()) {
				for (JsonElement el : object.getAsJsonArray()) {
					setNamesFromContributor(el, names);
				}
			} else {
				throw new IllegalStateException("contributor should be an array or object");
			}
		}
		
		private void setNamesFromName(JsonElement object, Set<String> names) {
			if (object == null || object.isJsonNull())
				return;
			if (object.isJsonPrimitive() && object.getAsJsonPrimitive().isString()) {
				String str = object.getAsString();
				if (str.trim().isEmpty() == false) {
					names.add(str);
				}
			} else if (object.isJsonArray()) {
				for (JsonElement el : object.getAsJsonArray()) {
					setNamesFromName(el, names);
				}
			}
		}

		@Override
		public DocumentMetadata getMetadata() throws IOException {
			return isProcessed ? this.metadata : storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id);
		}

		@Override
		public String getUniqueId() {
			return id;
		}
		
	}
}
