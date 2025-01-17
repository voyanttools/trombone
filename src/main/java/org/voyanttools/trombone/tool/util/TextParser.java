package org.voyanttools.trombone.tool.util;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {

    @XStreamOmitField
    private final String text;

    private int lettersCount = 0;
    private int wordsCount = 0;
    private int sentencesCount = 0;
    private int wordsWithMoreThanSixLettersCount = 0;
    private int wordsWithMoreThanTwoSyllablesCount = 0;

    public TextParser(String textToParse) {
        text = cleanText(textToParse);

        parseText(text);
    }

    private void parseText(String text) {
        StringBuilder wordBuilder = new StringBuilder();
        int charCount = 0;
        int spaceCount = 0;

        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                lettersCount++;
                wordBuilder.append(c);

                charCount++;
                if (charCount == 7)
                    wordsWithMoreThanSixLettersCount++;

            } else {
                // The word has ended, reset char counter to 0 and check syllables
                charCount = 0;

                String word = wordBuilder.toString();
                if (hasMoreThanTwoSyllables(word)) {
                    wordsWithMoreThanTwoSyllablesCount++;
                }
                wordBuilder = new StringBuilder();

                if (c == ' ') {
                    spaceCount++;

                } else if (c == '.') {
                    if (i == length - 1) // This is the end of the text
                        sentencesCount++;

                    /* This logic excludes the acronym with two dot (e.g. "The U.S. Office is here.").
                       It looks for another dot two characters before a dot with a following space ". ".
                    */
                    else if (text.charAt(i + 1) == ' ') {
                        if (i != 1 && i != 2) {
                            if (!text.substring(i - 2, i).contains("."))
                                sentencesCount++;
                        }
                    }
                }
            }
        }

        wordsCount = spaceCount + 1;
    }

    private boolean hasMoreThanTwoSyllables(String word) {
        // This regex method has been inspired from here: https://stackoverflow.com/a/46879336
        String regex = "[aâàáäiîìíïoôòóöuûùúüyêèéë]+e*|e(?!d$|ly).|[td]ed|le$";
        Matcher matcher = Pattern.compile(regex).matcher(word);

        int count = 0;

        while (matcher.find()) {
            count++;
        }

        // Cover cases where the a "y" is between 2 vowels. E.g. "payable" has 3 syllables, but count as 2 with the above logic.
        regex = "[aâàáäiîìíïoôòóöuûùúüyeêèéë]y[aâàáäiîìíïoôòóöuûùúüyeêèéë][^$]";
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

    public int getLettersCount() {
        return lettersCount;
    }

    public int getWordsCount() {
        return wordsCount;
    }

    public int getSentencesCount() {
        return sentencesCount;
    }

    public int getWordsWithMoreThanSixLettersCount() {
        return wordsWithMoreThanSixLettersCount;
    }

    public int getWordsWithMoreThanTwoSyllablesCount() {
        return wordsWithMoreThanTwoSyllablesCount;
    }

    public String getText() {
        return text;
    }
}
