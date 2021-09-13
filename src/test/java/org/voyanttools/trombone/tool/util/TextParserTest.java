package org.voyanttools.trombone.tool.util;

import org.junit.Test;

public class TextParserTest {

    public static final String TEXT_1 = "The rule of rhythm in prose is not so intricate. Here, too, we write in groups, or phrases, as I prefer to call them, for the prose phrase is greatly longer and is much more nonchalantly uttered than the group in verse; so that not only is there a greater interval of continuous sound between the pauses, but, for that very reason, word is linked more readily to word by a more summary enunciation. Still, the phrase is the strict analogue of the group, and successive phrases, like successive groups, must differ openly in length and rhythm. The rule of scansion in verse is to suggest no measure but the one in hand; in prose, to suggest no measure at all. Prose must be rhythmical, and it may be as much so as you will; but it must not be metrical. It may be anything, but it must not be verse.";
    public static final String TEXT_1_WITH_MARKUP = "<p>The rule of rhythm in prose is not so intricate.\n Here, too, we write in groups, or phrases, as I prefer to call them, for the prose phrase is greatly longer and is much more nonchalantly uttered than the group in verse; so that not only is there a greater interval of continuous sound between the pauses, but, for that very reason, word is linked more readily to word by a more summary enunciation.\n Still, the phrase is the strict analogue of the group, and successive phrases, like successive groups, must differ openly in length and rhythm.\n The rule of scansion in verse is to suggest no measure but the one in hand; in prose, to suggest no measure at all. Prose must be rhythmical, and it may be as much so as you will; but it must not be metrical.\n It may be anything, but it must not be verse.\n\n</p>";

    private static final int EXPECTED_1_NUMBER_OF_LETTERS = 623;
    private static final int EXPECTED_1_NUMBER_OF_WORDS = 151;
    private static final int EXPECTED_1_NUMBER_OF_WORDS_WITH_MORE_THAN_SIX_LETTERS = 24;
    private static final int EXPECTED_1_NUMBER_OF_SENTENCES = 6;
    private static final int EXPECTED_1_NUMBER_OF_WORDS_WITH_MORE_THAN_TWO_SYLLABLES = 14;


    public static final String TEXT_2 = "Where the amount of the annuity derived by the taxpayer during a year of income is more than, or less than, the amount payable for a whole year, the amount to be exclude from the amount so derived is the amount which bears to the amount which, but for this sub-section, would be the amount to be so, excluded the same proportion as the amount so derived bears to the amount payable for the whole year.";
    public static final String TEXT_2_WITH_MARKUP = "<p>Where the amount of the annuity derived by the taxpayer during a year of income is more than,\n or less than,\n the amount payable for a whole year, the amount to be exclude from the amount so derived is the amount which bears to the amount which, but for this sub-section,\n would be the amount to be so,\n excluded the same proportion as the amount so derived bears to the amount payable for the whole year.\n</p>";

    private static final int EXPECTED_2_NUMBER_OF_LETTERS = 318;
    private static final int EXPECTED_2_NUMBER_OF_WORDS = 76;
    private static final int EXPECTED_2_NUMBER_OF_WORDS_WITH_MORE_THAN_SIX_LETTERS = 11;
    private static final int EXPECTED_2_NUMBER_OF_SENTENCES = 1;
    private static final int EXPECTED_2_NUMBER_OF_WORDS_WITH_MORE_THAN_TWO_SYLLABLES = 6;

    public static final String TEXT_3 = "Existing computer programs that measure readability are based largely upon subroutines which estimate number of syllables, usually by counting vowels. The shortcoming in estimating syllables is that it necessitates keypunching the prose into the computer. There is no need to estimate syllables since word length in letters is a better predictor of readability than word length in syllables. Therefore, a new readability formula was computed that has for its predictors letters per 100 words and sentences per 100 words. Both predictors can be counted by an optical scanning device, and thus the formula makes it economically feasible for an organization such as the U.S. Office of Education to calibrate the readability of all textbooks for the public school system.";
    public static final String TEXT_3_WITH_MARKUP = "<p>Existing computer programs that measure readability are based largely upon subroutines which estimate number of syllables, usually by counting vowels.\n The shortcoming in estimating syllables is that it necessitates keypunching the prose into the computer.\n There is no need to estimate syllables since word length in letters is a better predictor of readability than word length in syllables.\n Therefore, a new readability formula was computed that has for its predictors letters per 100 words and sentences per 100 words.\n Both predictors can be counted by an optical scanning device, and thus the formula makes it economically feasible for an organization such as the U.S. Office of Education to calibrate the readability of all textbooks for the public school system.\n</p>";

    private static final int EXPECTED_3_NUMBER_OF_LETTERS = 639;
    private static final int EXPECTED_3_NUMBER_OF_WORDS = 119;
    private static final int EXPECTED_3_NUMBER_OF_WORDS_WITH_MORE_THAN_SIX_LETTERS = 42;
    private static final int EXPECTED_3_NUMBER_OF_SENTENCES = 5;
    private static final int EXPECTED_3_NUMBER_OF_WORDS_WITH_MORE_THAN_TWO_SYLLABLES = 32;


    @Test
    public void testWithText1() {
        TextParser textParser = new TextParser(TEXT_1);
        assertText1ParserValues(textParser);

        textParser = new TextParser(TEXT_1_WITH_MARKUP);
        assertText1ParserValues(textParser);
    }

    @Test
    public void testWithText2() {
        TextParser textParser = new TextParser(TEXT_2);
        assertText2ParserValues(textParser);

        textParser = new TextParser(TEXT_2_WITH_MARKUP);
        assertText2ParserValues(textParser);
    }

    @Test
    public void testWithText3() {
        TextParser textParser = new TextParser(TEXT_3);
        assertText3ParserValues(textParser);

        textParser = new TextParser(TEXT_3_WITH_MARKUP);
        assertText3ParserValues(textParser);
    }

    private static void assertText1ParserValues(TextParser textParser) {
        assert textParser.getNbrOfLetters() == EXPECTED_1_NUMBER_OF_LETTERS;
        assert textParser.getNbrOfWords() == EXPECTED_1_NUMBER_OF_WORDS;
        assert textParser.getNbrOfSentences() == EXPECTED_1_NUMBER_OF_SENTENCES;
        assert textParser.getNbrOfWordsWithMoreThanSixLetters() == EXPECTED_1_NUMBER_OF_WORDS_WITH_MORE_THAN_SIX_LETTERS;
        assert textParser.getNbrOfWordsWithMoreThanTwoSyllables() == EXPECTED_1_NUMBER_OF_WORDS_WITH_MORE_THAN_TWO_SYLLABLES;
    }

    private static void assertText2ParserValues(TextParser textParser) {
        assert textParser.getNbrOfLetters() == EXPECTED_2_NUMBER_OF_LETTERS;
        assert textParser.getNbrOfWords() == EXPECTED_2_NUMBER_OF_WORDS;
        assert textParser.getNbrOfSentences() == EXPECTED_2_NUMBER_OF_SENTENCES;
        assert textParser.getNbrOfWordsWithMoreThanSixLetters() == EXPECTED_2_NUMBER_OF_WORDS_WITH_MORE_THAN_SIX_LETTERS;
        assert textParser.getNbrOfWordsWithMoreThanTwoSyllables() == EXPECTED_2_NUMBER_OF_WORDS_WITH_MORE_THAN_TWO_SYLLABLES;
    }

    private static void assertText3ParserValues(TextParser textParser) {
        assert textParser.getNbrOfLetters() == EXPECTED_3_NUMBER_OF_LETTERS;
        assert textParser.getNbrOfWords() == EXPECTED_3_NUMBER_OF_WORDS;
        assert textParser.getNbrOfSentences() == EXPECTED_3_NUMBER_OF_SENTENCES;
        assert textParser.getNbrOfWordsWithMoreThanSixLetters() == EXPECTED_3_NUMBER_OF_WORDS_WITH_MORE_THAN_SIX_LETTERS;
        assert textParser.getNbrOfWordsWithMoreThanTwoSyllables() == EXPECTED_3_NUMBER_OF_WORDS_WITH_MORE_THAN_TWO_SYLLABLES;
    }
}
