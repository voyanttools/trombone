package org.voyanttools.trombone.model;

import org.voyanttools.trombone.tool.util.TextParser;

public class FOGIndex extends ReadabilityIndex {

    protected double fogIndex;

    public FOGIndex(int documentIndex, String documentId, String textToParse) {
        super(documentIndex, documentId, textToParse);

        fogIndex = calculateIndex(text);
    }

    @Override
    protected double calculateIndex(TextParser text) {
        double a = (double) text.getNbrOfWords() / (double) text.getNbrOfSentences();
        double b = (double) text.getNbrOfWordsWithMoreThanTwoSyllables() / (double) text.getNbrOfWords();

        return 0.4 * (a + 100 * b);
    }

    public double getFOGIndex() {
        return fogIndex;
    }
}
