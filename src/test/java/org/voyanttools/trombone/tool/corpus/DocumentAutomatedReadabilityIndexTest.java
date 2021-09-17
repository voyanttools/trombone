package org.voyanttools.trombone.tool.corpus;

import org.junit.Test;
import org.voyanttools.trombone.model.AutomatedReadabilityIndex;
import org.voyanttools.trombone.model.AutomatedReadabilityIndexTest;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import java.io.IOException;
import java.util.List;


public class DocumentAutomatedReadabilityIndexTest {

    private static final String FILE_PATH_FR = "udhr/udhr-fr.txt";
    private static final double EXPECTED_FR_AUTOMATED_READABILITY_INDEX = 10.233076707502605;

    private static final String FILE_PATH_EN = "udhr/udhr-en.txt";
    private static final double EXPECTED_EN_AUTOMATED_READABILITY_INDEX = 7.738992161254199;

    @Test
    public void test() throws IOException {
        for (Storage storage : TestHelper.getDefaultTestStorages()) {
            System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());

            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"string="+ AutomatedReadabilityIndexTest.TEXT}), AutomatedReadabilityIndexTest.EXPECTED_AUTOMATED_READABILITY_INDEX);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_FR)}), EXPECTED_FR_AUTOMATED_READABILITY_INDEX);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_EN)}), EXPECTED_EN_AUTOMATED_READABILITY_INDEX);
        }
    }

    private void testWithGivenParameters(Storage storage, FlexibleParameters parameters, double expectedAutomatedReadabilityIndex) throws IOException {
        CorpusCreator creator = new CorpusCreator(storage, parameters);
        creator.run();

        DocumentAutomatedReadabilityIndex documentAutomatedReadabilityIndex = new DocumentAutomatedReadabilityIndex(storage, parameters);
        documentAutomatedReadabilityIndex.run();

        List<AutomatedReadabilityIndex> automatedReadabilityIndexes = documentAutomatedReadabilityIndex.getAutomatedReadabilityIndexes();

        for (AutomatedReadabilityIndex automatedReadabilityIndex : automatedReadabilityIndexes) {
            assert automatedReadabilityIndex.getAutomatedReadabilityIndex() == expectedAutomatedReadabilityIndex;
        }
    }
}
