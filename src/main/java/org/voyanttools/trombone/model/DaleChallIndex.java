package org.voyanttools.trombone.model;

import org.voyanttools.trombone.tool.util.TextParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DaleChallIndex extends ReadabilityIndex {

    protected double daleChallIndex;
    protected int difficultWordsCount = 0;
    protected int easyWordsCount = 0;

    public DaleChallIndex(int documentIndex, String documentId, String textToParse) throws IOException {
        super(documentIndex, documentId, textToParse);

        daleChallIndex = calculateIndex(text);
    }

    @Override
    protected double calculateIndex(TextParser text) throws IOException {
        List<String> easyWords = getEasyWords();
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

    private List<String> getEasyWords() throws IOException {
        URI uri;

        try {
            uri = this.getClass().getResource("/org/voyanttools/trombone/keywords/easywords.en.txt").toURI();
        } catch (URISyntaxException e) {
            throw new IOException("Failed to retrieved the easy words list.");
        }

        File file = new File(uri);
        List<String> easyWords = Files.readAllLines(file.toPath());

        // Remove comments in the files
        easyWords = easyWords.stream()
                .filter(word -> !word.contains("#"))
                .collect(Collectors.toList());

        return easyWords;
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

        for (char c: word.toCharArray()) {
            if (Character.isDigit(c))
                return false;
        }

        return true;
    }
}
