package org.voyanttools.trombone.tool.notebook;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
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
@XStreamConverter(CatalogueFacets.CatalogueFacetsConverter.class)
public class CatalogueFacets extends AbstractTool {

	private Map<String, LabelAndValue[]> facetResults = new HashMap<String, LabelAndValue[]>();
	
	private final String[] defaultFacetFields = new String[] { "facet.author", "facet.keywords", "facet.language", "facet.license"};
	
	public CatalogueFacets(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}
	
	public float getVersion() {
		return super.getVersion()+1;
	}

	@Override
	public void run() throws IOException {
		facetResults.clear();
		
		DirectoryReader indexReader = DirectoryReader.open(storage.getNotebookLuceneManager().getIndexWriter(""));
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		// TODO store this because apparently init is costly?
		SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(indexReader);
		
		String[] facetQuery = parameters.getParameterValues("query");
		
		Query q;
		if (facetQuery.length == 0) {
			q = new MatchAllDocsQuery();
		} else {
			FacetsConfig config = new FacetsConfig();
			q = new DrillDownQuery(config);
			for (String facet : facetQuery) {
				String[] keyval = facet.split("=");
				if (keyval.length == 2) {
					((DrillDownQuery) q).add(keyval[0], keyval[1]);
				}
			}
		}
		
		String[] facetFields = parameters.getParameterValues("facets", defaultFacetFields);
		
	    int maxDocs = indexReader.maxDoc();
	    
		FacetsCollector fc = new FacetsCollector();
	    FacetsCollector.search(indexSearcher, q, maxDocs, fc);
	    Facets facets = new SortedSetDocValuesFacetCounts(state, fc);

	    for (String facetFieldName : facetFields) {
	    	FacetResult result = null;
	    	try {
	    		result = facets.getTopChildren(maxDocs, facetFieldName);
	    	} catch (IllegalArgumentException e) {
	    		// try/catch handling for non-indexed dimensions
	    	}
	    	if (result != null) {
	    		facetResults.put(facetFieldName, result.labelValues);
		    } else {
		    	facetResults.put(facetFieldName, new LabelAndValue[0]);
		    }
	    }
	}
	
	public Map<String, LabelAndValue[]> getFacetResults() {
		return facetResults;
	}

	public static class CatalogueFacetsConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return CatalogueFacets.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			CatalogueFacets catalogue = (CatalogueFacets) source;
			
	        ToolSerializer.startNode(writer, "facets", Map.class);
	        
			for (Entry<String, LabelAndValue[]> facetResult : catalogue.facetResults.entrySet()) {
		        writer.startNode("facets"); // not written in JSON
		        
		        writer.startNode("facet");
				writer.setValue(facetResult.getKey());
				writer.endNode();
				
				ToolSerializer.startNode(writer, "results", Map.class);
				for (LabelAndValue lv : facetResult.getValue()) {
					writer.startNode("result");
					
					writer.startNode("label");
					writer.setValue(lv.label);
					writer.endNode();
					
					ToolSerializer.startNode(writer, "count", Integer.class);
					writer.setValue(String.valueOf(lv.value));
					ToolSerializer.endNode(writer);
					
					writer.endNode();
				}
				ToolSerializer.endNode(writer);
				
				writer.endNode();
			}
			ToolSerializer.endNode(writer);
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
