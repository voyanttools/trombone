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
        double l = (double) text.getLettersCount() / text.getWordsCount() * 100;
        double s = (double) text.getSentencesCount() / text.getWordsCount() * 100;

        double result = 0.0588 * l - 0.296 * s - 15.8;

        if (Double.isFinite(result))
            return result;

        // When the calculation fails (result is not a finite number)
        return -999;
    }

    public double getColemanLiauIndex() {
        return colemanLiauIndex;
    }
}
