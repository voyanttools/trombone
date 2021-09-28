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
        double a = (double) text.getWordsCount() / text.getSentencesCount();
        double b = (double) text.getWordsWithMoreThanTwoSyllablesCount() / text.getWordsCount();

        double result =  0.4 * (a + 100 * b);

        if (Double.isFinite(result))
            return result;

        // When the calculation fails (result is not a finite number)
        return -999;
    }

    public double getFOGIndex() {
        return fogIndex;
    }
}
