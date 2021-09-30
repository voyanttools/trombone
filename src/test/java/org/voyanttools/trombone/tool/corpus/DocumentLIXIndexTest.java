package org.voyanttools.trombone.tool.corpus;

import org.junit.Test;
import org.voyanttools.trombone.model.LIXIndex;
import org.voyanttools.trombone.model.LIXIndexTest;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import java.io.IOException;
import java.util.List;


public class DocumentLIXIndexTest {

    private static final String FILE_PATH_FR = "udhr/udhr-fr.txt";
    private static final double EXPECTED_FR_LIX_INDEX = 46.681352923906374;

    private static final String FILE_PATH_EN = "udhr/udhr-en.txt";
    private static final double EXPECTED_EN_LIX_INDEX = 33.97387084733109;

    @Test
    public void test() throws IOException {
        for (Storage storage : TestHelper.getDefaultTestStorages()) {
            System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());

            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"string="+LIXIndexTest.TEXT}), LIXIndexTest.EXPECTED_LIX_INDEX);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_FR)}), EXPECTED_FR_LIX_INDEX);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_EN)}), EXPECTED_EN_LIX_INDEX);
        }
    }

    private void testWithGivenParameters(Storage storage, FlexibleParameters parameters, double expectedLIXIndex) throws IOException {
        CorpusCreator creator = new CorpusCreator(storage, parameters);
        creator.run();

        DocumentLIXIndex documentLIXIndex = new DocumentLIXIndex(storage, parameters);
        documentLIXIndex.run();

        List<LIXIndex> lixIndexes = documentLIXIndex.getLIXIndexes();

        for (LIXIndex lixIndex : lixIndexes) {
            assert lixIndex.getLIXIndex() == expectedLIXIndex;
        }
    }
}
