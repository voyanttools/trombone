/**
 * 
 */
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
public class XlsExpander implements Expander {
	
	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;
	
	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * @param storedDocumentSourceExpander 
	 * @param storedDocumentSourceStorage 
	 * 
	 */
	public XlsExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.expand.Expander#getExpandedStoredDocumentSources(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource)
			throws IOException {
		
		// first try to see if we've been here already
		String id = storedDocumentSource.getId();
		List<StoredDocumentSource> xlsStoredDocumentSources = storedDocumentSourceStorage.getMultipleExpandedStoredDocumentSources(id);

		if (xlsStoredDocumentSources!=null && xlsStoredDocumentSources.isEmpty()==false) {
			return xlsStoredDocumentSources;
		}
		
		xlsStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		
		// check to see if anything needs to be expanded
		String tableDocuments = parameters.getParameterValue("tableDocuments", "").toLowerCase();
		if (tableDocuments.isEmpty()==false) {
			if (tableDocuments.equals("rows")) {
				return getDocumentsRowCells(storedDocumentSource);
			}
			else if (tableDocuments.equals("columns")) {
				return getDocumentsColumns(storedDocumentSource);
			}
		}
		
		// otherwise, use the entire table
		xlsStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		xlsStoredDocumentSources.add(storedDocumentSource);
		return xlsStoredDocumentSources;
	}
	
	private Workbook getWorkBook(StoredDocumentSource storedDocumentSource) throws IOException {
		Workbook wb = null;
		try (InputStream inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId())) {
			wb = WorkbookFactory.create(inputStream);
		};
		return wb;
		
	}
	private List<StoredDocumentSource> getDocumentsColumns(StoredDocumentSource storedDocumentSource) throws IOException {
		DocumentMetadata metadata = storedDocumentSource.getMetadata();
		String id = storedDocumentSource.getId();
		Workbook wb = getWorkBook(storedDocumentSource);
		List<StoredDocumentSource> xlsStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		List<List<Integer>> columns = getInts(parameters.getParameterValues("tableContent"));
		StringBuffer docBuffer = new StringBuffer();
		int firstRow = parameters.getParameterBooleanValue("tableNoHeadersRow") ? 0 : 1;
		String title;
		for (int k = 0; k < wb.getNumberOfSheets(); k++) {
			Sheet sheet = wb.getSheetAt(k);
			int rows = sheet.getLastRowNum();
			
			// no columns defined, so take all, as defined by first row
			if (columns.isEmpty()) {
				short len = sheet.getRow(0).getLastCellNum();
				if (len>0) {
					for (int i=0; i<len; i++) {
						List<Integer> cols = new ArrayList<Integer>();
						cols.add(i);
						columns.add(cols);
					}
				}
			}
			
			for (List<Integer> set : columns) {
				for (int c : set) {
					for (int r = firstRow; r < rows+1; r++) {
						String value = getValue(sheet, r, c);
						if (value.isEmpty()==false)  {
							if (docBuffer.length()>0) docBuffer.append("\n\n");
							docBuffer.append(value);
						}
					}
				}
				if (docBuffer.length()>0) {
					String location = (k+1)+"."+StringUtils.join(set, "+")+"."+(firstRow+1);
					title = firstRow == 0 ? location : getValue(sheet.getRow(0), set, " ");
					xlsStoredDocumentSources.add(getChild(metadata, id, docBuffer.toString(), location, title, null, null, null, null, null, null, null));
					docBuffer.setLength(0); // reset buffer
				}
			}
			
		}
		wb.close();
		return xlsStoredDocumentSources;
	}
	
	private List<StoredDocumentSource> getDocumentsRowCells(StoredDocumentSource storedDocumentSource) throws IOException {
		DocumentMetadata metadata = storedDocumentSource.getMetadata();
		String id = storedDocumentSource.getId();
		Workbook wb = getWorkBook(storedDocumentSource);
		List<StoredDocumentSource> xlsStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		
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
		
		Row row;
		String contents;
		String location;
		for (int k = 0; k < wb.getNumberOfSheets(); k++) {
			Sheet sheet = wb.getSheetAt(k);
			int rows = sheet.getLastRowNum();
			for (int r = firstRow; r < rows+1; r++) {
				
				row = sheet.getRow(r);
				if (row==null) {continue;}
				if (columns.isEmpty()) {
					short len = row.getLastCellNum();
					if (len>0) {
						List<Integer> cols = new ArrayList<Integer>();
						for (int i=0; i<len; i++) {
							cols.add(i);
						}
						columns.add(cols);
					}
					
				}
				
				for (List<Integer> columnsSet : columns) {
					contents = columnsSet.isEmpty() ? getValue(row, "\t") : getValue(row, columnsSet, "\t");
					if (contents.isEmpty()==false) {
						location = (k+1)+"."+StringUtils.join(columnsSet, "+")+"."+(r+1);
						String title = location;
						List<String> currentAuthors = new ArrayList<String>();
						String pubDateStr = "";
						String publisherStr = "";
						String pubPlaceStr = "";
						String keywordsStr = "";
						String collectionStr = "";
						Map<String, String> extrasMap = new HashMap<>();
						
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
							
						}
						
						
						xlsStoredDocumentSources.add(getChild(metadata, id, contents, location, title, currentAuthors, pubDateStr, publisherStr, pubPlaceStr, keywordsStr, collectionStr, extrasMap));
						
					}
				}
			}
		}
		wb.close();
		return xlsStoredDocumentSources;
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
	
	private List<String> getAllValues(Row row, List<List<Integer>> ints) {
		return getAllValues(row, ints, " ");
	}
	
	private List<String> getAllValues(Row row, List<List<Integer>> ints, String separator) {
		List<String> allValues = new ArrayList<String>();
		for (List<Integer> cells : ints) {
			String val = getValue(row, cells, separator).trim();
			if (val.isEmpty()==false) {
				allValues.add(val);
			}
		}
		return allValues;
	}
	
	private String getValue(Row row, String separator) {
		short len = row.getLastCellNum();
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
	
	private List<String> getValues(Row row, List<Integer> cells) {
		List<String> strings = new ArrayList<String>();
		for (int i : cells) {
			Cell cell = row.getCell(i);
			if (cell!=null) {
				String s = getValue(cell);
				if (s!=null && s.isEmpty()==false) {
					strings.add(s);
				}
			}
		}
		return strings;
	}

	private String getValue(Row row, List<Integer> cells, String separator) {
		return StringUtils.join(getValues(row, cells), separator);
	}
	
	private String getValue(Sheet sheet, int rowIndex, int cellIndex) {
		
		if (rowIndex < 0 || cellIndex < 0) return "";
		
		Row row = sheet.getRow(rowIndex);
		if (row==null) return "";
		
		Cell cell = row.getCell(cellIndex);
		if (cell==null) return "";
		
		String value = getValue(cell);
		return value == null ? "" : value.trim();
		
	}
	
	private String getValue(Cell cell) {
		if (cell!=null) {
			return new DataFormatter().formatCellValue(cell);
		}
		return null;
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
		String id = DigestUtils.md5Hex(parentId + location + title + StringUtils.join(authors));
		InputSource inputSource = new StringInputSource(id, metadata, string);
		return storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
	}

	

}
