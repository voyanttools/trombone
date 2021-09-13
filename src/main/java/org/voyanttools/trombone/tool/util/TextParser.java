package org.voyanttools.trombone.tool.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {

    private int nbrOfLetters = 0;
    private int nbrOfWords = 0;
    private int nbrOfSentences = 0;
    private int nbrOfWordsWithMoreThanSixLetters = 0;
    private int nbrOfWordsWithMoreThanTwoSyllables = 0;

    public TextParser(String text) {
        parseText(text);
    }

    private void parseText(String text) {
        StringBuilder wordBuilder = new StringBuilder();
        int charCount = 0;
        int spaceCount = 0;

        text = cleanText(text);
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                nbrOfLetters++;
                wordBuilder.append(c);

                charCount++;
                if (charCount == 7)
                    nbrOfWordsWithMoreThanSixLetters++;

            } else {
                // Word is now ended, reset char counter to 0 and check syllables
                charCount = 0;

                String word = wordBuilder.toString();
                if (hasMoreThanTwoSyllables(word)) {
                    nbrOfWordsWithMoreThanTwoSyllables++;
                }
                wordBuilder = new StringBuilder();

                if (c == ' ') {
                    spaceCount++;

                } else if (c == '.') {
                    if (i == length - 1) // This is the end of the text
                        nbrOfSentences++;

                    /* This logic excludes the acronym with two dot (e.g. "The U.S. Office is here.").
                       It looks for another dot two characters before a dot with a following space ". ".
                    */
                    else if (text.charAt(i + 1) == ' ') {
                        if (i != 1 && i != 2) {
                            if (!text.substring(i - 2, i).contains("."))
                                nbrOfSentences++;
                        }
                    }
                }
            }
        }

        nbrOfWords = spaceCount + 1;
    }

    private boolean hasMoreThanTwoSyllables(String word) {
        // This regex method has been found here https://stackoverflow.com/a/46879336
        String regex = "[aiouy]+e*|e(?!d$|ly).|[td]ed|le$";
        Matcher matcher = Pattern.compile(regex).matcher(word);

        int count = 0;

        while (matcher.find()) {
            count++;
        }

        // Cover cases where the a "y" is between 2 vowels. E.g. "payable" has 3 syllables, but count as 2 with the above logic.
        regex = "[aioue]y[aioue][^$]";
        matcher = Pattern.compile(regex).matcher(word);
        while (matcher.find()) {
            count++;
        }

        return count > 2;
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

    public int getNbrOfWordsWithMoreThanSixLetters() {
        return nbrOfWordsWithMoreThanSixLetters;
    }

    public int getNbrOfWordsWithMoreThanTwoSyllables() {
        return nbrOfWordsWithMoreThanTwoSyllables;
    }
}
