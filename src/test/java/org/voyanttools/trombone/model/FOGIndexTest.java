package org.voyanttools.trombone.model;

import org.junit.Test;

public class FOGIndexTest {

    public static final String TEXT_EN = "The rule of rhythm in prose is not so intricate. Here, too, we write in groups, or phrases, as I prefer to call them, for the prose phrase is greatly longer and is much more nonchalantly uttered than the group in verse; so that not only is there a greater interval of continuous sound between the pauses, but, for that very reason, word is linked more readily to word by a more summary enunciation. Still, the phrase is the strict analogue of the group, and successive phrases, like successive groups, must differ openly in length and rhythm. The rule of scansion in verse is to suggest no measure but the one in hand; in prose, to suggest no measure at all. Prose must be rhythmical, and it may be as much so as you will; but it must not be metrical. It may be anything, but it must not be verse.";
    public static final String TEXT_FR = "Je réservai une chambre et y laissai mes bagages, puis je me mis en marche sur une route de terre. Il faisait beau. Les raysons du soleil jouaient dans le feuillage mouvant. L'air était pur et frais et il me montait au nez des odeurs  de mousse et de champignons. Il était bon de marcher dans avoir rien à vendre, si bien que je laissai passer les premières voitures sans leur faire signe.";

    private static final int A_DOCUMENT_INDEX = 0;
    private static final String A_DOCUMENT_ID = "A_DOCUMENT_ID";

    public static final double EXPECTED_FOG_INDEX_EN = 13.775275938189845;
    public static final double EXPECTED_FOG_INDEX_FR = 9.127671232876713;

    @Test
    public void testWithTextInEnglish() {
        FOGIndex fogIndex = new FOGIndex(A_DOCUMENT_INDEX, A_DOCUMENT_ID, TEXT_EN);

        assert fogIndex.docIndex == A_DOCUMENT_INDEX;
        assert fogIndex.docId.equals(A_DOCUMENT_ID);
        assert fogIndex.getFOGIndex() == EXPECTED_FOG_INDEX_EN;
    }

    @Test
    public void testWithTextInFrench() {
        FOGIndex fogIndex = new FOGIndex(A_DOCUMENT_INDEX, A_DOCUMENT_ID, TEXT_FR);

        assert fogIndex.docIndex == A_DOCUMENT_INDEX;
        assert fogIndex.docId.equals(A_DOCUMENT_ID);
        assert fogIndex.getFOGIndex() == EXPECTED_FOG_INDEX_FR;
    }
}
