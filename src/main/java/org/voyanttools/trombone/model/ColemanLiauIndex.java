package org.voyanttools.trombone.model;

import java.io.Serializable;

public class ColemanLiauIndex implements Serializable {

    protected int docIndex;
    protected String docId;

    protected int nbrOfLetters = 0;
    protected int nbrOfWords = 0;
    protected int nbrOfSentences = 0;

    protected double colemanLiauIndex;

    public ColemanLiauIndex(int documentIndex, String documentId, String text) {
        docIndex = documentIndex;
        docId = documentId;
        colemanLiauIndex = calculateColemanLiauIndex(text);
    }

    private double calculateColemanLiauIndex(String text) {
        int spaceCount = 0;

        text = cleanText(text);
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (Character.isLetterOrDigit(c))
                nbrOfLetters++;

            else if (c == ' ')
                spaceCount++;

            else if (c == '.')
                if (i == length - 1) // This is the end of the text
                    nbrOfSentences++;

                // This logic excludes the acronym with two dot (e.g. "The U.S. Office is here.").
                // It looks for another dot two characters before a dot with a following space ". ".
                else if (text.charAt(i + 1) == ' ')
                    if (i != 1 && i != 2)
                        if (!text.substring(i-2, i).contains("."))
                            nbrOfSentences++;
        }
        nbrOfWords = spaceCount + 1;

        double l = (double) nbrOfLetters / (double) nbrOfWords * 100;
        double s = (double) nbrOfSentences / (double) nbrOfWords * 100;

        return 0.0588 * l - 0.296 * s - 15.8;
    }

    private String cleanText(String text) {
        text = text.replace("\n", "");
        text = text.replace("\t", "");
        text = text.replace("<p>", "");
        text = text.replace("</p>", "");
        text = removeGroupsOfWhiteSpace(text);

        return text;
    }

    private String removeGroupsOfWhiteSpace(String text) {
        text = text.trim();

        while (text.contains("  ")) {
            text = text.replace("  ", " ");
        }

        return text;
    }

    public int getNbrOfLetters() {
        return nbrOfLetters;
    }

    public int getNbrOfWords() {
        return nbrOfWords;
    }

    public int getNbrOfSentences() {
        return nbrOfSentences;
    }

    public double getColemanLiauIndex() {
        return colemanLiauIndex;
    }
}
