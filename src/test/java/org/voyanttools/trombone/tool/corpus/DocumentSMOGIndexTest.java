package org.voyanttools.trombone.tool.corpus;

import org.junit.Test;
import org.voyanttools.trombone.model.SMOGIndex;
import org.voyanttools.trombone.model.SMOGIndexTest;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import java.io.IOException;
import java.util.List;


public class DocumentSMOGIndexTest {

    private static final String FILE_PATH_FR = "udhr/udhr-fr.txt";
    private static final double EXPECTED_FR_SMOG_INDEX = 12.96994396234918;

    private static final String FILE_PATH_EN = "udhr/udhr-en.txt";
    private static final double EXPECTED_EN_SMOG_INDEX = 10.227310004160055;

    @Test
    public void test() throws IOException {
        for (Storage storage : TestHelper.getDefaultTestStorages()) {
            System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());

            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"string="+SMOGIndexTest.TEXT}), SMOGIndexTest.EXPECTED_SMOG_INDEX);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_FR)}), EXPECTED_FR_SMOG_INDEX);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_EN)}), EXPECTED_EN_SMOG_INDEX);
        }
    }

    private void testWithGivenParameters(Storage storage, FlexibleParameters parameters, double expectedSMOGIndex) throws IOException {
        CorpusCreator creator = new CorpusCreator(storage, parameters);
        creator.run();

        DocumentSMOGIndex documentSMOGIndex = new DocumentSMOGIndex(storage, parameters);
        documentSMOGIndex.run();

        List<SMOGIndex> smogIndexes = documentSMOGIndex.getSMOGIndexes();

        for (SMOGIndex smogIndex : smogIndexes) {
            assert smogIndex.getSMOGIndex() == expectedSMOGIndex;
        }
    }
}
