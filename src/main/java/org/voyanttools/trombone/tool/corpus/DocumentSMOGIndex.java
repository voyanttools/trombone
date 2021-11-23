package org.voyanttools.trombone.tool.corpus;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.SMOGIndex;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("documentSMOGIndex")
public class DocumentSMOGIndex extends AbstractCorpusTool {

    private List<SMOGIndex> smogIndexes;

    public DocumentSMOGIndex(Storage storage, FlexibleParameters parameters) {
        super(storage, parameters);

        smogIndexes = new ArrayList<>();
    }

    @Override
    public void run(CorpusMapper corpusMapper) throws IOException {
        Corpus corpus = corpusMapper.getCorpus();

        List<String> idsList = this.getCorpusStoredDocumentIdsFromParameters(corpus);

        for (String documentId : corpus.getDocumentIds()) {
            if (idsList.contains(documentId) == false) { continue; }

            int documentIndex = corpus.getDocumentPosition(documentId);
            String text = corpus.getDocument(documentId).getDocumentString();

            smogIndexes.add(new SMOGIndex(documentIndex, documentId, text));
        }
    }

    public List<SMOGIndex> getSMOGIndexes() {
        return smogIndexes;
    }
}
