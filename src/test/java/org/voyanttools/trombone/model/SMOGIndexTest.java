package org.voyanttools.trombone.model;

import org.junit.Test;

public class SMOGIndexTest {

    public static final String TEXT = "The rule of rhythm in prose is not so intricate. Here, too, we write in groups, or phrases, as I prefer to call them, for the prose phrase is greatly longer and is much more nonchalantly uttered than the group in verse; so that not only is there a greater interval of continuous sound between the pauses, but, for that very reason, word is linked more readily to word by a more summary enunciation. Still, the phrase is the strict analogue of the group, and successive phrases, like successive groups, must differ openly in length and rhythm. The rule of scansion in verse is to suggest no measure but the one in hand; in prose, to suggest no measure at all. Prose must be rhythmical, and it may be as much so as you will; but it must not be metrical. It may be anything, but it must not be verse.";

    private static final int A_DOCUMENT_INDEX = 0;
    private static final String A_DOCUMENT_ID = "A_DOCUMENT_ID";

    public static final double EXPECTED_SMOG_INDEX = 11.855464076750408;

    @Test
    public void test() {
        SMOGIndex smogIndex = new SMOGIndex(A_DOCUMENT_INDEX, A_DOCUMENT_ID, TEXT);

        assert smogIndex.docIndex == A_DOCUMENT_INDEX;
        assert smogIndex.docId.equals(A_DOCUMENT_ID);
        assert smogIndex.getSMOGIndex() == EXPECTED_SMOG_INDEX;
    }
}
