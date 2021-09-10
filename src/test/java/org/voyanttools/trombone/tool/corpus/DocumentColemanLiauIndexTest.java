package org.voyanttools.trombone.tool.corpus;

import org.junit.Test;
import org.voyanttools.trombone.model.ColemanLiauIndex;
import org.voyanttools.trombone.model.ColemanLiauIndexTest;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import java.io.IOException;
import java.util.List;


public class DocumentColemanLiauIndexTest {

    private static final String FILE_PATH_FR = "udhr/udhr-fr.txt";
    private static final double EXPECTED_FR_COLEMAN_LIAU_INDEX = 11.004248004910991;

    private static final String FILE_PATH_EN = "udhr/udhr-en.txt";
    private static final double EXPECTED_EN_COLEMAN_LIAU_INDEX = 6.766382978723399;

    @Test
    public void test() throws IOException {
        for (Storage storage : TestHelper.getDefaultTestStorages()) {
            System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());

            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"string="+ColemanLiauIndexTest.TEXT}), ColemanLiauIndexTest.EXPECTED_COLEMAN_LIAU_INDEX);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_FR)}), EXPECTED_FR_COLEMAN_LIAU_INDEX);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_EN)}), EXPECTED_EN_COLEMAN_LIAU_INDEX);
        }
    }

    private void testWithGivenParameters(Storage storage, FlexibleParameters parameters, double expectedColemanLiauIndex) throws IOException {
        CorpusCreator creator = new CorpusCreator(storage, parameters);
        creator.run();

        DocumentColemanLiauIndex documentColemanLiauIndex = new DocumentColemanLiauIndex(storage, parameters);
        documentColemanLiauIndex.run();

        List<ColemanLiauIndex> colemanLiauIndexes = documentColemanLiauIndex.getColemanLiauIndexes();

        for (ColemanLiauIndex colemanLiauIndex : colemanLiauIndexes) {
            assert colemanLiauIndex.getColemanLiauIndex() == expectedColemanLiauIndex;
        }
    }
}
