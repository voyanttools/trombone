package org.voyanttools.trombone.model;

import org.junit.Test;

public class AutomatedReadabilityIndexTest {

    public static final String TEXT = "The rule of rhythm in prose is not so intricate. Here, too, we write in groups, or phrases, as I prefer to call them, for the prose phrase is greatly longer and is much more nonchalantly uttered than the group in verse; so that not only is there a greater interval of continuous sound between the pauses, but, for that very reason, word is linked more readily to word by a more summary enunciation. Still, the phrase is the strict analogue of the group, and successive phrases, like successive groups, must differ openly in length and rhythm. The rule of scansion in verse is to suggest no measure but the one in hand; in prose, to suggest no measure at all. Prose must be rhythmical, and it may be as much so as you will; but it must not be metrical. It may be anything, but it must not be verse.";
    public static final String TEXT_WITH_MARKUP = "<p>The rule of rhythm in prose is not so intricate.\n Here, too, we write in groups, or phrases, as I prefer to call them, for the prose phrase is greatly longer and is much more nonchalantly uttered than the group in verse; so that not only is there a greater interval of continuous sound between the pauses, but, for that very reason, word is linked more readily to word by a more summary enunciation.\n Still, the phrase is the strict analogue of the group, and successive phrases, like successive groups, must differ openly in length and rhythm.\n The rule of scansion in verse is to suggest no measure but the one in hand; in prose, to suggest no measure at all. Prose must be rhythmical, and it may be as much so as you will; but it must not be metrical.\n It may be anything, but it must not be verse.\n\n</p>";

    private static final int A_DOCUMENT_INDEX = 0;
    private static final String A_DOCUMENT_ID = "A_DOCUMENT_ID";

    private static final int EXPECTED_NUMBER_OF_LETTERS = 623;
    private static final int EXPECTED_NUMBER_OF_WORDS = 151;
    private static final int EXPECTED_NUMBER_OF_SENTENCES = 6;
    private static final double EXPECTED_AUTOMATED_READABILITY_INDEX = 10.585982339955848;

    @Test
    public void testWithNormalText() {
        AutomatedReadabilityIndex automatedReadabilityIndex = new AutomatedReadabilityIndex(A_DOCUMENT_INDEX, A_DOCUMENT_ID, TEXT);

        assert automatedReadabilityIndex.docIndex == A_DOCUMENT_INDEX;
        assert automatedReadabilityIndex.docId.equals(A_DOCUMENT_ID);
        assertAutomatedReadabilityIndexValues(automatedReadabilityIndex);
    }

    @Test
    public void testWithTextWithMarkup() {
        AutomatedReadabilityIndex automatedReadabilityIndex = new AutomatedReadabilityIndex(A_DOCUMENT_INDEX, A_DOCUMENT_ID, TEXT_WITH_MARKUP);

        assert automatedReadabilityIndex.docIndex == A_DOCUMENT_INDEX;
        assert automatedReadabilityIndex.docId.equals(A_DOCUMENT_ID);
        assertAutomatedReadabilityIndexValues(automatedReadabilityIndex);
    }


    public static void assertAutomatedReadabilityIndexValues(AutomatedReadabilityIndex automatedReadabilityIndex) {
        assert automatedReadabilityIndex.getNbrOfLetters() == EXPECTED_NUMBER_OF_LETTERS;
        assert automatedReadabilityIndex.getNbrOfWords() == EXPECTED_NUMBER_OF_WORDS;
        assert automatedReadabilityIndex.getNbrOfSentences() == EXPECTED_NUMBER_OF_SENTENCES;
        assert automatedReadabilityIndex.getAutomatedReadabilityIndex() == EXPECTED_AUTOMATED_READABILITY_INDEX;
    }
}
