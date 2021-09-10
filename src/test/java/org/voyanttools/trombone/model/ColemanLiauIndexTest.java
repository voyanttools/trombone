package org.voyanttools.trombone.model;

import org.junit.Test;

public class ColemanLiauIndexTest {

    public static final String TEXT = "Existing computer programs that measure readability are based largely upon subroutines which estimate number of syllables, usually by counting vowels. The shortcoming in estimating syllables is that it necessitates keypunching the prose into the computer. There is no need to estimate syllables since word length in letters is a better predictor of readability than word length in syllables. Therefore, a new readability formula was computed that has for its predictors letters per 100 words and sentences per 100 words. Both predictors can be counted by an optical scanning device, and thus the formula makes it economically feasible for an organization such as the U.S. Office of Education to calibrate the readability of all textbooks for the public school system.";

    private static final int A_DOCUMENT_INDEX = 0;
    private static final String A_DOCUMENT_ID = "A_DOCUMENT_ID";

    public static final double EXPECTED_COLEMAN_LIAU_INDEX = 14.53042016806722;

    @Test
    public void test() {
        ColemanLiauIndex colemanLiauIndex = new ColemanLiauIndex(A_DOCUMENT_INDEX, A_DOCUMENT_ID, TEXT);

        assert colemanLiauIndex.docIndex == A_DOCUMENT_INDEX;
        assert colemanLiauIndex.docId.equals(A_DOCUMENT_ID);
        assert colemanLiauIndex.getColemanLiauIndex() == EXPECTED_COLEMAN_LIAU_INDEX;
    }
}
