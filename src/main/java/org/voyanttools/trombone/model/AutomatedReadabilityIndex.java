package org.voyanttools.trombone.model;

import org.voyanttools.trombone.tool.util.TextParser;

public class AutomatedReadabilityIndex extends ReadabilityIndex {

    protected double automatedReadabilityIndex;

    public AutomatedReadabilityIndex(int documentIndex, String documentId, String textToParse) {
        super(documentIndex, documentId, textToParse);

        automatedReadabilityIndex = calculateIndex(text);
    }

    @Override
    protected double calculateIndex(TextParser text) {
        return 4.71 * ((double) text.getNbrOfLetters() / (double) text.getNbrOfWords()) + 0.5 * ((double) text.getNbrOfWords() / (double) text.getNbrOfSentences()) - 21.43;
    }

    public double getAutomatedReadabilityIndex() {
        return automatedReadabilityIndex;
    }
}
