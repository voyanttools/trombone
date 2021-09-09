package org.voyanttools.trombone.model;

import org.voyanttools.trombone.tool.util.TextParser;

public class ColemanLiauIndex extends ReadabilityIndex {

    protected double colemanLiauIndex;

    public ColemanLiauIndex(int documentIndex, String documentId, String textToParse) {
        super(documentIndex, documentId, textToParse);

        colemanLiauIndex = calculateIndex(text);
    }

    @Override
    protected double calculateIndex(TextParser text) {
        double l = (double) text.getNbrOfLetters() / (double) text.getNbrOfWords() * 100;
        double s = (double) text.getNbrOfSentences() / (double) text.getNbrOfWords() * 100;

        return 0.0588 * l - 0.296 * s - 15.8;
    }

    public double getColemanLiauIndex() {
        return colemanLiauIndex;
    }
}
