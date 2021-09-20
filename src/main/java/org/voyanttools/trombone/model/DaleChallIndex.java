package org.voyanttools.trombone.model;

import org.voyanttools.trombone.tool.util.TextParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class DaleChallIndex extends ReadabilityIndex {

    protected double daleChallIndex;
    protected int nbrOfDifficultWords = 0;
    protected int nbrOfEasyWords = 0;

    public DaleChallIndex(int documentIndex, String documentId, String textToParse) throws IOException {
        super(documentIndex, documentId, textToParse);

        daleChallIndex = calculateIndex(text);
    }

    @Override
    protected double calculateIndex(TextParser text) throws IOException {
        List<String> easyWords = getEasyWords();
        String[] words = text.getText().split(" ");

        for (String word: words) {
            if (easyWords.contains(word))
                nbrOfEasyWords++;
            else
                nbrOfDifficultWords++;
        }

        double percentageOfDifficultWords = (double) nbrOfDifficultWords / text.getNbrOfWords() * 100;
        double averageSentenceLength = (double) text.getNbrOfWords() / text.getNbrOfSentences();

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
}
