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
        double lettersPerWord = (double) text.getLettersCount() / text.getWordsCount();
        double wordsPerSentence = (double) text.getWordsCount() / text.getSentencesCount();

        double result = 4.71 * lettersPerWord + 0.5 * wordsPerSentence - 21.43;

        if (Double.isFinite(result))
            return result;

        // When the calculation fails (result is not a finite number)
        return -999;
    }

    public double getAutomatedReadabilityIndex() {
        return automatedReadabilityIndex;
    }
}
