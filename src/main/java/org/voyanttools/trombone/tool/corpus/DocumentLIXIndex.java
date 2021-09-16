package org.voyanttools.trombone.tool.corpus;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.LIXIndex;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("documentLIXIndex")
public class DocumentLIXIndex extends AbstractCorpusTool {

    private List<LIXIndex> lixIndexes;

    public DocumentLIXIndex(Storage storage, FlexibleParameters parameters) {
        super(storage, parameters);

        lixIndexes = new ArrayList<>();
    }

    @Override
    public void run(CorpusMapper corpusMapper) throws IOException {
        Corpus corpus = corpusMapper.getCorpus();

        for (String documentId : corpus.getDocumentIds()) {
            int documentIndex = corpus.getDocumentPosition(documentId);
            String text = corpus.getDocument(documentId).getDocumentString();

            lixIndexes.add(new LIXIndex(documentIndex, documentId, text));
        }
    }

    public List<LIXIndex> getLIXIndexes() {
        return lixIndexes;
    }
}
