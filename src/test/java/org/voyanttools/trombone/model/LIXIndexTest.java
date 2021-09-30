package org.voyanttools.trombone.model;

import org.junit.Test;

public class LIXIndexTest {

    public static final String TEXT = "Where the amount of the annuity derived by the taxpayer during a year of income is more than, or less than, the amount payable for a whole year, the amount to be exclude from the amount so derived is the amount which bears to the amount which, but for this sub-section, would be the amount to be so, excluded the same proportion as the amount so derived bears to the amount payable for the whole year.";

    private static final int A_DOCUMENT_INDEX = 0;
    private static final String A_DOCUMENT_ID = "A_DOCUMENT_ID";

    public static final double EXPECTED_LIX_INDEX = 90.47368421052632;

    @Test
    public void testWithNormalText() {
        LIXIndex lixIndex = new LIXIndex(A_DOCUMENT_INDEX, A_DOCUMENT_ID, TEXT);

        assert lixIndex.docIndex == A_DOCUMENT_INDEX;
        assert lixIndex.docId.equals(A_DOCUMENT_ID);
        assert lixIndex.getLIXIndex() == EXPECTED_LIX_INDEX;
    }
}
