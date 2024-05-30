package org.voyanttools.trombone.input.expand;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

public class TabularExpander implements Expander {

	private FlexibleParameters parameters;
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private DocumentFormat format;
	
	public TabularExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters, DocumentFormat format) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
		this.format = format;
	}

	@Override
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource) throws IOException {
		
		// first try to see if we've been here already
		String id = storedDocumentSource.getId();
		List<StoredDocumentSource> tabularStoredDocumentSources = storedDocumentSourceStorage.getMultipleExpandedStoredDocumentSources(id);

		if (tabularStoredDocumentSources!=null && tabularStoredDocumentSources.isEmpty()==false) {
			return tabularStoredDocumentSources;
		}
		
		tabularStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		
		String tableDocuments = parameters.getParameterValue("tableDocuments", "").toLowerCase();
		if (tableDocuments.isEmpty()==false) {
			if (tableDocuments.equals("rows")) {
				return getDocumentsRowCells(storedDocumentSource);
			}
			else if (tableDocuments.equals("columns")) {
				return getDocumentsColumns(storedDocumentSource);
			}
		}
		
		tabularStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		tabularStoredDocumentSources.add(storedDocumentSource);
		return tabularStoredDocumentSources;
	}

	private List<StoredDocumentSource> getDocumentsColumns(StoredDocumentSource storedDocumentSource) throws IOException {
		DocumentMetadata metadata = storedDocumentSource.getMetadata();
		String id = storedDocumentSource.getId();
		List<StoredDocumentSource> tabularStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		
		List<String[]> allRows = getAllRows(storedDocumentSource);
		int numRows = allRows.size();
		
		List<List<Integer>> columns = getInts(parameters.getParameterValues("tableContent"));
		
		if (columns.isEmpty()) {
			int len = allRows.get(0).length;
			for (int i=0; i<len; i++) {
				List<Integer> cols = new ArrayList<Integer>();
				cols.add(i);
				columns.add(cols);
			}
		}
		
		
		StringBuffer docBuffer = new StringBuffer();
		int firstRow = parameters.getParameterBooleanValue("tableNoHeadersRow") ? 0 : 1;
		String title;
		
		for (List<Integer> set : columns) {
			for (int c : set) {
				for (int r = firstRow; r < numRows; r++) { 
					String value = "";
					String[] row = allRows.get(r);
					if (c >= 0 && c < row.length) {
						value = row[c];
					}
					if (value.isEmpty()==false) {
						if (docBuffer.length()>0) docBuffer.append("\n\n");
						docBuffer.append(value);
					}
				}
			}
			if (docBuffer.length()>0) {
				String location = (1)+"."+StringUtils.join(set, "+")+"."+(firstRow+1);
				title = firstRow == 0 ? location : getValue(allRows.get(0), set, " ");
				tabularStoredDocumentSources.add(getChild(metadata, id, docBuffer.toString(), location, title, null, null, null, null, null, null, null));
				docBuffer.setLength(0); // reset buffer
			}
		}
		
		return tabularStoredDocumentSources;
	}

	private List<StoredDocumentSource> getDocumentsRowCells(StoredDocumentSource storedDocumentSource) throws IOException {
		DocumentMetadata parentMetadata = storedDocumentSource.getMetadata();
		String parentId = storedDocumentSource.getId();
		
		List<StoredDocumentSource> tabularStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		
		List<String[]> allRows = getAllRows(storedDocumentSource);
		int numRows = allRows.size();
		
		List<List<Integer>> columns = getInts(parameters.getParameterValues("tableContent"));
		List<List<Integer>> titles = getInts(parameters.getParameterValues("tableTitle"));
		List<List<Integer>> authors = getInts(parameters.getParameterValues("tableAuthor"));
		List<List<Integer>> pubDate = getInts(parameters.getParameterValues("tablePubDate"));
		List<List<Integer>> publisher = getInts(parameters.getParameterValues("tablePublisher"));
		List<List<Integer>> pubPlace = getInts(parameters.getParameterValues("tablePubPlace"));
		List<List<Integer>> keywords = getInts(parameters.getParameterValues("tableKeywords"));
		List<List<Integer>> collection = getInts(parameters.getParameterValues("tableCollection"));
		Map<String, List<List<Integer>>> extras = processExtras("tableExtraMetadata");
		int firstRow = parameters.getParameterBooleanValue("tableNoHeadersRow") ? 0 : 1;
		
		boolean doGrouping = parameters.getKeys().contains("tableGroupBy");
		
		List<List<Integer>> groupBy = getInts(parameters.getParameterValues("tableGroupBy"));
		Map<String, List<String>> groupedRowInputSources = new HashMap<String, List<String>>();
		
		for (int r = firstRow; r < numRows; r++) {
			String[] row = allRows.get(r);
			if (columns.isEmpty()) {
				int len = row.length;
				if (len > 0) {
					List<Integer> cols = new ArrayList<Integer>();
					for (int i=0; i<len; i++) {
						cols.add(i);
					}
					columns.add(cols);
				}
			}
			
			for (List<Integer> columnsSet : columns) {
				String contents = columnsSet.isEmpty() ? getValue(row, "\t") : getValue(row, columnsSet, "\t");
				if (contents.isEmpty()==false) {
					String location = (1)+"."+StringUtils.join(columnsSet, "+")+"."+(r+1);
					String title = location;
					List<String> currentAuthors = new ArrayList<String>();
					String pubDateStr = "";
					String publisherStr = "";
					String pubPlaceStr = "";
					String keywordsStr = "";
					String collectionStr = "";
					Map<String, String> extrasMap = new HashMap<>();
					
					String groupByKey = "";
					
					if (columns.size()==1) {
						if (titles.isEmpty()==false) {
							List<String> currentTitles = getAllValues(row, titles);
							if (currentTitles.isEmpty()==false) {
								title = StringUtils.join(currentTitles, " ");
							}
						}
						
						currentAuthors = getAllValues(row, authors, " ");
						
						List<String> currentPubDate = getAllValues(row, pubDate);
						if (currentPubDate.isEmpty()==false) {
							pubDateStr = StringUtils.join(currentPubDate, " ");
						}
						
						List<String> currentPublisher = getAllValues(row, publisher);
						if (currentPublisher.isEmpty()==false) {
							publisherStr = StringUtils.join(currentPublisher, " ");
						}
						
						List<String> currentPubPlace = getAllValues(row, pubPlace);
						if (currentPubPlace.isEmpty()==false) {
							pubPlaceStr = StringUtils.join(currentPubPlace, " ");
						}
						
						List<String> currentKeywords = getAllValues(row, keywords);
						if (currentKeywords.isEmpty()==false) {
							keywordsStr = StringUtils.join(currentKeywords, " ");
						}
						
						List<String> currentCollection = getAllValues(row, collection);
						if (currentCollection.isEmpty()==false) {
							collectionStr = StringUtils.join(currentCollection, " ");
						}
						
						for (String extraKey : extras.keySet()) {
							List<String> extraValues = getAllValues(row, extras.get(extraKey));
							if (extraValues.isEmpty()==false) {
								extrasMap.put(extraKey, StringUtils.join(extraValues, " "));
							}
						}
						
						List<String> currentGroupByKey = getAllValues(row, groupBy);
						if (currentGroupByKey.isEmpty()==false) {
							groupByKey = StringUtils.join(currentGroupByKey, " ");
						}
						
					}
					
					if (doGrouping) {
						if (groupedRowInputSources.containsKey(groupByKey) == false) {
							groupedRowInputSources.put(groupByKey, new ArrayList<String>());
						}
						groupedRowInputSources.get(groupByKey).add(contents);
					} else {
						DocumentMetadata metadata = createChildMetadata(parentMetadata, parentId, location, title, currentAuthors, pubDateStr, publisherStr, pubPlaceStr, keywordsStr, collectionStr, extrasMap);
						InputSource inputSource = new StringInputSource(TabularExpander.generateId(parentId, metadata, "row"), metadata, contents);
						tabularStoredDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(inputSource));
					}
				}
			}
		}
		
		if (doGrouping) {
			for (Map.Entry<String, List<String>> mappedRowInputSources : groupedRowInputSources.entrySet()) {
				String key = mappedRowInputSources.getKey();
				List<String> mappedRowInputSourcesList = mappedRowInputSources.getValue();
				String location = parentId+";group:"+key;
				StringBuffer combinedContents = new StringBuffer();
				for (String rowInputSource : mappedRowInputSourcesList) {
					combinedContents.append(rowInputSource).append("\n\n");
				}
				DocumentMetadata combinedMetadata = parentMetadata.asParent(parentId, DocumentMetadata.ParentType.EXPANSION);
				combinedMetadata.setModified(parentMetadata.getModified());
				combinedMetadata.setSource(Source.STRING);
				combinedMetadata.setLocation(location);
				combinedMetadata.setDocumentFormat(DocumentFormat.TEXT);
				String id = DigestUtils.md5Hex(parentId + location);
				
				InputSource inputSource = new StringInputSource(id, combinedMetadata, combinedContents.toString());
				tabularStoredDocumentSources.add(storedDocumentSourceStorage.getStoredDocumentSource(inputSource));
			}
		}
		
		return tabularStoredDocumentSources;
	}
	
	
	private Map<String, List<List<Integer>>> processExtras(String paramKey) {
		Map<String, List<List<Integer>>> extras = new HashMap<>();
		for (String string : parameters.getParameterValues(paramKey)) {
			for (String x :string.split("(\r\n|\r|\n)+")) {
				x = x.trim();
				String[] parts = x.split("=");
				if (parts.length>1) {
					String extraKey = parts[0].trim();
					String[] extraValue = new String[] {StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length), "=").trim()};
					extras.put(extraKey, getInts(extraValue));
				}
			}
		}
		return extras;
	}
	
	private List<String> getAllValues(String[] row, List<List<Integer>> ints) {
		return getAllValues(row, ints, " ");
	}
	
	private List<String> getAllValues(String[] row, List<List<Integer>> ints, String separator) {
		List<String> allValues = new ArrayList<String>();
		for (List<Integer> cells : ints) {
			String val = getValue(row, cells, separator).trim();
			if (val.isEmpty()==false) {
				allValues.add(val);
			}
		}
		return allValues;
	}
	
	private String getValue(String[] row, String separator) {
		int len = row.length;
		if (len>0) {
			List<Integer> cells = new ArrayList<Integer>();
			for (int i=0; i<len; i++) {
				cells.add(i);
			}
			return getValue(row, cells, separator);
		}
		else {
			return "";
		}
	}
	
	private List<String> getValues(String[] row, List<Integer> cells) {
		List<String> strings = new ArrayList<String>();
		for (int i : cells) {
			String cell = row[i];
			if (cell!=null) {
				if (cell!=null && cell.isEmpty()==false) {
					strings.add(cell);
				}
			}
		}
		return strings;
	}

	private String getValue(String[] row, List<Integer> cells, String separator) {
		return StringUtils.join(getValues(row, cells), separator);
	}
	
	private List<List<Integer>> getInts(String[] values) {
		List<List<Integer>> outerList = new ArrayList<List<Integer>>();
		for (String string : values) {
			for (String set : string.trim().split(",")) {
				List<Integer> innerList = new ArrayList<Integer>();
				for (String s : set.trim().split("\\+")) {
					try {
						innerList.add(Integer.valueOf(s.trim())-1); // subtract 1 because user input is 1-based but processing is 0-based
					}
					catch (NumberFormatException e) {
						throw new IllegalArgumentException("Table parameter should only contain numbers: "+string, e);
					}
				}
				if (innerList.isEmpty()==false) {
					outerList.add(innerList);
				}
			}
		}
		return outerList;
	}
	
	private List<String[]> getAllRows(StoredDocumentSource storedDocumentSource) throws IOException {
		InputStream input = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
		List<String[]> allRows;
		if (format == DocumentFormat.CSV) {
			CsvParser parser = new CsvParser(new CsvParserSettings());
			allRows = parser.parseAll(input);
		} else {
			TsvParser parser = new TsvParser(new TsvParserSettings());
			allRows = parser.parseAll(input);
		}
		
		return allRows;
	}
	
	private DocumentMetadata createChildMetadata(DocumentMetadata parentMetadata, String parentId, String location, String title,
			List<String> authors, String pubDate, String publisher, String pubPlace, String keywords, String collection, Map<String, String> extras) {
		DocumentMetadata metadata = parentMetadata.asParent(parentId, DocumentMetadata.ParentType.EXPANSION);
		metadata.setModified(parentMetadata.getModified());
		metadata.setSource(Source.STRING);
		metadata.setLocation(location);
		metadata.setTitle(title);
		if (authors!=null && authors.isEmpty()==false) {
			metadata.setAuthors(authors.toArray(new String[0]));
		}
		if (pubDate!=null && pubDate.isEmpty()==false) {
			metadata.setPubDates(pubDate);
		}
		if (publisher!=null && publisher.isEmpty()==false) {
			metadata.setPublishers(publisher);
		}
		if (pubPlace!=null && pubPlace.isEmpty()==false) {
			metadata.setPubPlaces(pubPlace);
		}
		if (keywords!=null && keywords.isEmpty()==false) {
			metadata.setKeywords(keywords);
		}
		if (collection!=null && collection.isEmpty()==false) {
			metadata.setCollections(collection);
		}
		if (extras!=null && extras.isEmpty()==false) {
			for (String key : extras.keySet()) {
				metadata.setExtra(key, extras.get(key));
			}
		}
		metadata.setDocumentFormat(DocumentFormat.TEXT);
		
		return metadata;
	}
	
	private static String generateId(String parentId, DocumentMetadata metadata, String extra) {
		String location = metadata.getLocation();
		String title = metadata.getTitle();
		String author = metadata.getAuthor();
		return DigestUtils.md5Hex(parentId + location + title + author + extra);
	}
	
	private StoredDocumentSource getChild(DocumentMetadata parentMetadata, String parentId, String string, String location, String title,
			List<String> authors, String pubDate, String publisher, String pubPlace, String keywords, String collection, Map<String, String> extras) throws IOException {
		DocumentMetadata metadata = parentMetadata.asParent(parentId, DocumentMetadata.ParentType.EXPANSION);
		metadata.setModified(parentMetadata.getModified());
		metadata.setSource(Source.STRING);
		metadata.setLocation(location);
		metadata.setTitle(title);
		if (authors!=null && authors.isEmpty()==false) {
			metadata.setAuthors(authors.toArray(new String[0]));
		}
		if (pubDate!=null && pubDate.isEmpty()==false) {
			metadata.setPubDates(pubDate);
		}
		if (publisher!=null && publisher.isEmpty()==false) {
			metadata.setPublishers(publisher);
		}
		if (pubPlace!=null && pubPlace.isEmpty()==false) {
			metadata.setPubPlaces(pubPlace);
		}
		if (keywords!=null && keywords.isEmpty()==false) {
			metadata.setKeywords(keywords);
		}
		if (collection!=null && collection.isEmpty()==false) {
			metadata.setCollections(collection);
		}
		if (extras!=null && extras.isEmpty()==false) {
			for (String key : extras.keySet()) {
				metadata.setExtra(key, extras.get(key));
			}
		}
		metadata.setDocumentFormat(DocumentFormat.TEXT);
		String id = TabularExpander.generateId(parentId, metadata, "");
		InputSource inputSource = new StringInputSource(id, metadata, string);
		return storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
	}
}
