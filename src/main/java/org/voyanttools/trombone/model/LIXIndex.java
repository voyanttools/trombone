package org.voyanttools.trombone.model;

import org.voyanttools.trombone.tool.util.TextParser;

public class LIXIndex extends ReadabilityIndex {

    protected double lixIndex;

    public LIXIndex(int documentIndex, String documentId, String textToParse) {
        super(documentIndex, documentId, textToParse);

        lixIndex = calculateIndex(text);
    }

    @Override
    protected double calculateIndex(TextParser text) {
        double a = text.getNbrOfWords();
        double b = text.getNbrOfSentences();
        double c = text.getNbrOfWordsWithMoreThanSixLetters();

        return a / b + c * 100.0 / a;
    }

    public double getLIXIndex() {
        return lixIndex;
    }
}
