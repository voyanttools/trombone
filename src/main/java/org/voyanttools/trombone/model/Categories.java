package org.voyanttools.trombone.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.storage.Storage;

public class Categories {
	
	private Map<String, HashSet<String>> categories;

	public Categories() {
		categories = new HashMap<String, HashSet<String>>();
	}
	
	public boolean isEmpty() {
		return categories.isEmpty();
	}
	
	public boolean hasCategory(String category) {
		return categories.containsKey(category);
	}

	public Collection<String> getCategory(String category) {
		return categories.get(category);
	}
	
	public static Categories getCategories(Storage storage, Corpus corpus, String id) throws IOException {
		
		if (id.trim().isEmpty()) {
			return new Categories();
		}
		
		if (id.equals("auto")) {
			for (String lang : corpus.getLanguageCodes()) {
				try(InputStream inputStream = Categories.class.getResourceAsStream("/org/voyanttools/trombone/categories/categories."+lang+".txt")) {
					StringWriter writer = new StringWriter();
					IOUtils.copy(inputStream, writer, Charset.forName("UTF-8"));
					return getCategories(writer.toString());
				} catch (Exception e) {
				}
			}
			// we've tried all the language codes and none match, so return empty categories
			return new Categories();
		}
		
		if (id.length()==2) { // looks like language code
			return getCategories(storage, corpus, "categories."+id);
		}
		
		if (id.matches("categories\\.\\w+")) { // looks like local resource
			try(InputStream inputStream = Categories.class.getResourceAsStream("/org/voyanttools/trombone/categories/"+id+".txt")) {
				StringWriter writer = new StringWriter();
				IOUtils.copy(inputStream, writer, Charset.forName("UTF-8"));
				return getCategories(writer.toString());
			} catch (Exception e) {
			}
		}
		
		// good 'ol resource
		String contents = storage.retrieveString(id, Storage.Location.object);
		return getCategories(contents);	
	}
	
	private static Categories getCategories(String contents) {
		if (contents.trim().startsWith("{")==false || contents.contains("categories")==false) {
			throw new IllegalArgumentException("Unable to find categories.");
		}
		Categories categories = new Categories();
		StringReader stringReader = new StringReader(contents);
		JsonReader reader = Json.createReader(stringReader);
		JsonObject root = reader.readObject();
		JsonObject cats = root.getJsonObject("categories");
		for (String key : cats.keySet()) {
			JsonArray wordsArray = cats.getJsonArray(key);
			HashSet<String> words = new HashSet<String>();
			for (int i=0,len=wordsArray.size(); i<len; i++) {
				words.add(wordsArray.getJsonString(i).getString());
			}
			categories.categories.put(key, words);
		}
		stringReader.close();
		return categories;
	}
	
}
