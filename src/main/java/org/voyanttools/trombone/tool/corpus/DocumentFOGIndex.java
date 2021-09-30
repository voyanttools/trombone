package org.voyanttools.trombone.tool.corpus;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.FOGIndex;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocumentFOGIndex extends AbstractCorpusTool {

    private List<FOGIndex> fogIndexes;

    public DocumentFOGIndex(Storage storage, FlexibleParameters parameters) {
        super(storage, parameters);

        fogIndexes = new ArrayList<>();
    }

    @Override
    public void run(CorpusMapper corpusMapper) throws IOException {
        Corpus corpus = corpusMapper.getCorpus();

        for (String documentId : corpus.getDocumentIds()) {
            int documentIndex = corpus.getDocumentPosition(documentId);
            String text = corpus.getDocument(documentId).getDocumentString();

            fogIndexes.add(new FOGIndex(documentIndex, documentId, text));
        }
    }

    public List<FOGIndex> getFOGIndexes() {
        return fogIndexes;
    }
}
