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
        double wordsCount = text.getWordsCount();
        double sentencesCount = text.getSentencesCount();
        double longWords = text.getWordsWithMoreThanSixLettersCount();

        return wordsCount / sentencesCount + longWords * 100.0 / wordsCount;
    }

    public double getLIXIndex() {
        return lixIndex;
    }
}
