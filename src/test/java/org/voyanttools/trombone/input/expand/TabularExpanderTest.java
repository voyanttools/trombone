package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class TabularExpanderTest {

	@Test
	public void test() throws IOException {
		
		InputSource inputSource;
		
//		inputSource = new FileInputSource(TestHelper.getResource("formats/tabular.csv"));
//		test(inputSource, DocumentFormat.CSV);

		inputSource = new FileInputSource(TestHelper.getResource("formats/tabular.tsv"));
		test(inputSource, DocumentFormat.TSV);
	}
	
	private void test(InputSource inputSource, DocumentFormat format) throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		
		StoredDocumentSource storedDocumentSource;
		List<StoredDocumentSource> expandedSourceDocumentSources;

		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		StoredDocumentSourceExpander storedDocumentSourceExpander;
		FlexibleParameters parameters;
		InputStream inputStream;
		String contents;
		
		// test metadata handling (need to test first because further tests will use cached DocumentMetadata)
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "rows");
		parameters.setParameter("tableNoHeadersRow", "false");
		parameters.setParameter("tableContent", 1);
		parameters.setParameter("tableExtraMetadata", "col2=2");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals("Title 1", expandedSourceDocumentSources.get(0).getMetadata().getExtra("col2"));
		
		// no parameters, this would get handled by an extractor, not the expander
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals(1, expandedSourceDocumentSources.size());
		
		/* COLUMNS */
		
		// documents as columns, no columns defined so use all
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "columns");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals("Author", expandedSourceDocumentSources.get(0).getMetadata().getTitle());
		assertEquals(3, expandedSourceDocumentSources.size());
		
		// first column tableDocumentsColumns, default header column
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "columns");
		parameters.setParameter("tableContent", 1);
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals(1, expandedSourceDocumentSources.size());
		assertEquals("Author", expandedSourceDocumentSources.get(0).getMetadata().getTitle());
		inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertEquals(7, contents.split("\n+").length);
		
		// first column tableDocumentsColumns, no header column
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "columns");
		parameters.setParameter("tableContent", 1);
		parameters.setParameter("tableNoHeadersRow", "true");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals("1.0.1", expandedSourceDocumentSources.get(0).getMetadata().getTitle());
		assertEquals(1, expandedSourceDocumentSources.size());
		inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertEquals(8, contents.split("\n+").length);
		
		// first and second columns tableDocumentsColumns
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "columns");
		parameters.setParameter("tableContent", "1,2");
		parameters.setParameter("tableNoHeadersRow", "true");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals(2, expandedSourceDocumentSources.size());

		// first and second columns merged (third doesn't is empty or doesn't exist) tableDocumentsColumns
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "columns");
		parameters.setParameter("tableContent", "1+2,3000");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals(1, expandedSourceDocumentSources.size());
		
		// syntax error with column definitions
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "columns");
		parameters.setParameter("tableContent", "a; 2, 3");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		boolean caught = false;
		try {
			expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
			fail("We should have had an illegal argument exception.");
		}
		catch (IllegalArgumentException e) {
		}

		
		/* ROWS */
		
		// documents as rows, nothing defined
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "rows");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals("1.0+1+2.2", expandedSourceDocumentSources.get(0).getMetadata().getTitle());
		assertEquals("1.0+1+2.2", expandedSourceDocumentSources.get(0).getMetadata().getLocation());
		assertEquals(7, expandedSourceDocumentSources.size());
		
		// documents as rows, first column only
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "rows");
		parameters.setParameter("tableContent", 1);
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals(7, expandedSourceDocumentSources.size());
		assertEquals("1.0.2", expandedSourceDocumentSources.get(0).getMetadata().getTitle());
		assertEquals("1.0.2", expandedSourceDocumentSources.get(0).getMetadata().getLocation());
		inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertEquals(1, contents.split("\n").length);
		
		// documents as rows, first column only, use header
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "rows");
		parameters.setParameter("tableContent", 1);
		parameters.setParameter("tableNoHeadersRow", "true");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals("1.0.1", expandedSourceDocumentSources.get(0).getMetadata().getTitle());
		assertEquals(8, expandedSourceDocumentSources.size());
		inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(expandedSourceDocumentSources.get(0).getId());
		contents = IOUtils.toString(inputStream, "UTF-8");
		inputStream.close();
		assertEquals(1, contents.split("\n+").length);
		
		// documents as rows, with grouping
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "rows");
		parameters.setParameter("tableGroupBy", 1);
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals(4, expandedSourceDocumentSources.size());
		
		// documents as rows, with grouping
		parameters = new FlexibleParameters();
		parameters.setParameter("tableDocuments", "rows");
		parameters.setParameter("tableGroupBy", "1+3");
		storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage, parameters);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandTabular(storedDocumentSource, format);
		assertEquals(6, expandedSourceDocumentSources.size());
		
		storage.destroy();
	}
}
