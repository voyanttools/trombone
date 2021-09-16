package org.voyanttools.trombone.tool.corpus;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DaleChallIndex;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocumentDaleChallIndex extends AbstractCorpusTool {

    private List<DaleChallIndex> daleChallIndexes;

    public DocumentDaleChallIndex(Storage storage, FlexibleParameters parameters) {
        super(storage, parameters);

        daleChallIndexes = new ArrayList<>();
    }

    @Override
    public void run(CorpusMapper corpusMapper) throws IOException {
        Corpus corpus = corpusMapper.getCorpus();

        for (String documentId : corpus.getDocumentIds()) {
            int documentIndex = corpus.getDocumentPosition(documentId);
            String text = corpus.getDocument(documentId).getDocumentString();

            daleChallIndexes.add(new DaleChallIndex(documentIndex, documentId, text));
        }
    }

    public List<DaleChallIndex> getDaleChallIndexes() {
        return daleChallIndexes;
    }
}
