package org.voyanttools.trombone.tool.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.voyanttools.trombone.model.Categories;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.CorpusManager;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("storedCategories")
public class StoredCategories extends StoredResource {

	public StoredCategories(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		String id = parameters.getParameterValue("retrieveResourceId", "");
		if (id.isEmpty()==false) {
			String localId = null;
			if (id.equals("auto")) {
				localId = "categories.en.txt"; // default
				if (parameters.containsKey("corpus")) {
					Corpus corpus = CorpusManager.getCorpus(storage, parameters);
					for (String lang : corpus.getLanguageCodes()) {
						try {
							Categories.class.getResourceAsStream("/org/voyanttools/trombone/categories/categories."+lang+".txt");
							localId = "categories."+lang+".txt";
						} catch (NullPointerException e) {
						}
					}
				}
			} else if (id.length()==2) {
				localId = "categories."+id+".txt"; // assume it's a language code
			} else if (id.matches("categories\\.\\w+")) { // looks like local resource
				localId = id+".txt";
			}
			if (localId!=null) {
				try(InputStream inputStream = Categories.class.getResourceAsStream("/org/voyanttools/trombone/categories/"+localId)) {
					StringWriter writer = new StringWriter();
					IOUtils.copy(inputStream, writer, Charset.forName("UTF-8"));
					resource = writer.toString();
					this.id = id;
					return;
				} catch (Exception e) {
				}
			}
		}
		super.run();
	}
}
