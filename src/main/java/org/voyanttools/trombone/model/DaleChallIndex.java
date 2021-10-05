package org.voyanttools.trombone.model;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.voyanttools.trombone.tool.util.TextParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DaleChallIndex extends ReadabilityIndex {

    @XStreamOmitField
    private List<String> easyWords;

    protected double daleChallIndex;
    protected int difficultWordsCount = 0;
    protected int easyWordsCount = 0;

    public DaleChallIndex(int documentIndex, String documentId, String textToParse, List<String> easyWords) throws IOException {
        super(documentIndex, documentId, textToParse);

        this.easyWords = easyWords;

        daleChallIndex = calculateIndex(text);
    }

    @Override
    protected double calculateIndex(TextParser text) throws IOException {
        List<String> words = Arrays.asList(text.getText().split(" "));

        // Cleaning words from ".", ",", etc.
        words = words.stream().map(this::cleanWord).collect(Collectors.toList());
        words = words.stream().filter(this::isAWord).collect(Collectors.toList());

        for (String word : words) {
            if (easyWords.contains(word) || easyWords.contains(removeSCharAtEndOfWordIfAny(word))) {
                easyWordsCount++;
            } else {
                difficultWordsCount++;
            }
        }

        double percentageOfDifficultWords = (double) difficultWordsCount / text.getWordsCount() * 100;
        double averageSentenceLength = (double) text.getWordsCount() / text.getSentencesCount();

        double readingScore = 0.1579 * percentageOfDifficultWords + 0.0496 * averageSentenceLength;

        if (!Double.isFinite(readingScore))
            return -999;

        if (percentageOfDifficultWords > 5) {
            // This is the adjusted score, from the new Dale-Chall method.
            return readingScore + 3.6365;
        }

        return readingScore;
    }


    public double getDaleChallIndex() {
        return daleChallIndex;
    }


    private String cleanWord(String word) {
        return word.replace(".", "")
                .replace(",", "")
                .replace(";", "")
                .replace(":", "")
                .toLowerCase();
    }

    private String removeSCharAtEndOfWordIfAny(String word) {
        if (word.charAt(word.length() - 1) == 's')
            return word.substring(0, word.length() - 1);

        return word;
    }

    private boolean isAWord(String word) {
        if (word.length() == 0) {
            return false;
        }

        for (char c : word.toCharArray()) {
            if (Character.isDigit(c))
                return false;
        }

        return true;
    }
}
