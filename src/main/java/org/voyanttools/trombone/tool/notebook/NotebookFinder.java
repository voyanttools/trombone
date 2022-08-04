package org.voyanttools.trombone.tool.notebook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.voyanttools.trombone.lucene.search.FieldPrefixAwareSimpleQueryParser;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.tool.util.ToolSerializer;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamAlias("catalogue")
@XStreamConverter(NotebookFinder.CatalogueFacetsConverter.class)
public class NotebookFinder extends AbstractTool {
	
	private final static String FACET_PREFIX = "facet";
	
	private List<LabelAndValueAndDim> facetResults = new ArrayList<LabelAndValueAndDim>();
	
	private List<Document> notebookResults = new ArrayList<Document>();
	
	public NotebookFinder(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {

		String[] queries = null;
		if (parameters.containsKey("query")) {
			queries = getQueries();
		} else {
			throw new IllegalArgumentException("Must provide a query parameter");
		}
		
		DirectoryReader indexReader = DirectoryReader.open(storage.getNotebookLuceneManager().getIndexWriter(""));
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		Analyzer analyzer = storage.getNotebookLuceneManager().getAnalyzer("");
		SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(indexReader);
		
		Query query = getFacetAwareQuery(queries, indexReader, analyzer);
		FacetsCollector fc = new FacetsCollector();
		TopDocs topdocs = FacetsCollector.search(indexSearcher, query, indexReader.maxDoc(), fc);
		
		Facets facets = new SortedSetDocValuesFacetCounts(state, fc);
		List<FacetResult> results = facets.getAllDims(100);
		for (FacetResult result : results) {
			addResult(facetResults, result);
		}
		
		System.out.println(query.toString());
		System.out.println("hits: "+topdocs.scoreDocs.length);
		
		for (ScoreDoc sd : topdocs.scoreDocs) {
			Document doc = indexSearcher.doc(sd.doc);
			notebookResults.add(doc);
		}
	}
	
	private Query getFacetAwareQuery(String[] queryStrings, IndexReader indexReader, Analyzer analyzer) throws IOException {
		
		FacetsConfig config = new FacetsConfig();
		SimpleQueryParser queryParser = new FieldPrefixAwareSimpleQueryParser(indexReader, analyzer);
		
		Map<String, List<Query>> fieldedQueries = new HashMap<String, List<Query>>();
		for (String queryString : queryStrings) {
			if (queryString.startsWith(FACET_PREFIX+".") && queryString.contains(":")) {
				String field = queryString.substring(0, queryString.indexOf(":"));
				DrillDownQuery ddq = new DrillDownQuery(config);
				ddq.add(queryString.substring(0, queryString.indexOf(":")), queryString.substring(queryString.indexOf(":")+1));
				if (!fieldedQueries.containsKey(field)) {fieldedQueries.put(field, new ArrayList<Query>());}
				fieldedQueries.get(field).add(ddq);
			}
			else {
				Query query = queryParser.parse(queryString);
				String field = query.toString();
				if (query instanceof TermQuery) {field = ((TermQuery) query).getTerm().field();}
				else if (query instanceof PrefixQuery) {field = ((PrefixQuery) query).getField();}
				else if (query instanceof PhraseQuery) {field = ((PhraseQuery) query).getTerms()[0].field();}
				else {
					System.out.println(query);
				}
				if (!fieldedQueries.containsKey(field)) {fieldedQueries.put(field, new ArrayList<Query>());}
				fieldedQueries.get(field).add(query);
			}
		}

		
		List<Query> queries = new ArrayList<Query>();
		for (List<Query> queriesSet : fieldedQueries.values()) {
			if (queriesSet.size()==1) {queries.add(queriesSet.get(0));}
			else {
				BooleanQuery.Builder builder = new BooleanQuery.Builder();
				for (Query query : queriesSet) {
					builder.add(query, Occur.MUST);
				}
				queries.add(builder.build());
			}
		}
		
		if (queries.size()==1) {return queries.get(0);}
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for (Query query : queries) {
			builder.add(query, Occur.MUST);
		}
		return builder.build();
	}
	
	private void addResult(List<LabelAndValueAndDim> facetResults, FacetResult result) {
		for (LabelAndValue labelAndValue : result.labelValues) {
			facetResults.add(new LabelAndValueAndDim(labelAndValue, result.dim));
		}
	}
	
	private class LabelAndValueAndDim implements Comparable<LabelAndValueAndDim> {
		private LabelAndValue labelAndValue;
		private String dim;
		private LabelAndValueAndDim(LabelAndValue labelAndValue, String dim) {
			this.labelAndValue = labelAndValue;
			this.dim = dim;
		}

		@Override
		public int compareTo(LabelAndValueAndDim o) {
			return labelAndValue.value==o.labelAndValue.value ? labelAndValue.label.compareTo(o.labelAndValue.label) : Integer.compare(o.labelAndValue.value.intValue(), labelAndValue.value.intValue());
		}
	}
	
	public static class CatalogueFacetsConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return NotebookFinder.class.isAssignableFrom(type);
		}
		
		private String getField(Document doc, String fieldName, String defaultValue) {
			String field = doc.get(fieldName);
			if (field == null) {
				return defaultValue;
			} else {
				return field;
			}
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			NotebookFinder catalogue = (NotebookFinder) source;
			
			int total = 0;
			ToolSerializer.startNode(writer, "notebooks", Map.class);
			for (Document doc : catalogue.notebookResults) {
		        writer.startNode("notebook"); // not written in JSON
		        
		        writer.startNode("id");
				writer.setValue(doc.get("id"));
				writer.endNode();
		        
				writer.startNode("author");
				writer.setValue(getField(doc, "author", ""));
				writer.endNode();
				
				writer.startNode("title");
				writer.setValue(getField(doc, "title", ""));
				writer.endNode();
				
				writer.startNode("description");
				writer.setValue(getField(doc, "description", ""));
				writer.endNode();
				
				String[] keywords = doc.getValues("keywords");
				if (keywords == null) {
					keywords = new String[] {};
				}
				ToolSerializer.startNode(writer, "keywords", List.class);
				for (String keyword : keywords) {
					writer.startNode("keyword");
					writer.setValue(keyword);
					writer.endNode();
				}
				ToolSerializer.endNode(writer);
				
				writer.startNode("language");
				writer.setValue(getField(doc, "language", ""));
				writer.endNode();
				
				writer.startNode("license");
				writer.setValue(getField(doc, "license", ""));
				writer.endNode();
				
				writer.startNode("created");
				writer.setValue(doc.get("created"));
				writer.endNode();
				
				writer.startNode("modified");
				writer.setValue(doc.get("modified"));
				writer.endNode();
				
				String catVal = doc.get("catalogue");
				if (catVal == null) {
					catVal = "false";
				}
				ToolSerializer.startNode(writer, "catalogue", Boolean.class);
				writer.setValue(catVal);
				ToolSerializer.endNode(writer);
				
				writer.endNode();
				total++;
			}
			ToolSerializer.endNode(writer);
			
			ToolSerializer.startNode(writer, "total", Integer.class);
			writer.setValue(String.valueOf(total));
			ToolSerializer.endNode(writer);
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
