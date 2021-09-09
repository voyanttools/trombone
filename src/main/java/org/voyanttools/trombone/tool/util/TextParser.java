package org.voyanttools.trombone.tool.util;

public class TextParser {

    private int nbrOfLetters = 0;
    private int nbrOfWords = 0;
    private int nbrOfSentences = 0;

    public TextParser(String text) {
        parseText(text);
    }

    private void parseText(String text) {
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
                        if (!text.substring(i - 2, i).contains("."))
                            nbrOfSentences++;
        }
        nbrOfWords = spaceCount + 1;
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
}
