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

    // Found here https://gist.github.com/Drainet/89cfbd78dcb96bdd39108cb4469e3c0b
    private double calculateColemanLiauIndex(String text) {
        int spaceCount = 0;

        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                nbrOfLetters++;
            } else if (c == ' ') {
                spaceCount++;
            } else if (c == '.') {
                if (i != length - 1 && text.charAt(i + 1) == ' ') {
                    nbrOfSentences++;
                }
            }

        }
        nbrOfWords = spaceCount + 1;

        double l = (double) nbrOfLetters / (double) nbrOfWords * 100;
        double s = (double) nbrOfSentences / (double) nbrOfWords * 100;

        return 0.0588 * l - 0.296 * s - 15.8;
    }
}
