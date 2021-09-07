package org.voyanttools.trombone.tool.corpus;

import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.AbstractCorpusTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ColemanLiauIndex extends AbstractCorpusTool {

    private List<String> documentIds;

    public ColemanLiauIndex(Storage storage, FlexibleParameters parameters) {
        super(storage, parameters);

        documentIds = new ArrayList<String>();
    }

    @Override
    public void run(CorpusMapper corpusMapper) throws IOException {
        Corpus corpus = corpusMapper.getCorpus();

        int numberOfLetters = calculateNumberOfLetters();
        int numberOfWords = calculateNumberOfWords();
        int numberOfSentences = calculateNumberOfSentences();


        for (String id : this.getCorpusStoredDocumentIdsFromParameters(corpusMapper.getCorpus())) {
            documentIds.add(id);
            String string = corpus.getDocument(id).getDocumentString();
        }

        log("test");

    }

    private int calculateNumberOfLetters() {
        return 0;
    }

    private int calculateNumberOfWords() {
        return 0;
    }

    private int calculateNumberOfSentences() {
        return 0;
    }


    /*
        Coleman-Liau Index calculation:
            CLI = 0.0588 * L - 0.296 * S - 15.8
            where
                L: numberOfLetters / numberOfWords  * 100
                S: numberOfSentences /  * 100

            Therefore,
                CLI = (0.0588 * numberOfLetters - 0.296 * numberOfSentences) * 100 / numberOfWords - 15.8
     */
    private double calculateColemanLiauIndex(int numberOfLetters, int numberOfWords, int numberOfSentences) {
        return (0.0588 * numberOfLetters - 0.296 * numberOfSentences) * 100 / numberOfWords - 15.8;
    }
}
