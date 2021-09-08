package org.voyanttools.trombone.tool.corpus;

import org.junit.Test;
import org.voyanttools.trombone.model.AutomatedReadabilityIndex;
import org.voyanttools.trombone.model.AutomatedReadabilityIndexTest;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.build.RealCorpusCreator;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import java.io.IOException;
import java.util.List;


public class DocumentAutomatedReadabilityIndexTest {

    // Expected values for "udhr/udhr-fr.txt"
    private static final int EXPECTED_FR_NUMBER_OF_LETTERS = 7889;
    private static final int EXPECTED_FR_NUMBER_OF_WORDS = 1629;
    private static final int EXPECTED_FR_NUMBER_OF_SENTENCES = 92;
    private static final double EXPECTED_FR_COLEMAN_LIAU_INDEX = 10.233076707502605;

    // Expected values for "udhr/udhr-en.txt"
    private static final int EXPECTED_EN_NUMBER_OF_LETTERS = 4616;
    private static final int EXPECTED_EN_NUMBER_OF_WORDS = 1128;
    private static final int EXPECTED_EN_NUMBER_OF_SENTENCES = 57;
    private static final double EXPECTED_EN_COLEMAN_LIAU_INDEX = 7.738992161254199;

    @Test
    public void test() throws IOException {
        for (Storage storage : TestHelper.getDefaultTestStorages()) {
            System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
            testWithRawText(storage);
            testWithFiles(
                    storage,
                    "udhr/udhr-fr.txt",
                    EXPECTED_FR_NUMBER_OF_LETTERS,
                    EXPECTED_FR_NUMBER_OF_WORDS,
                    EXPECTED_FR_NUMBER_OF_SENTENCES,
                    EXPECTED_FR_COLEMAN_LIAU_INDEX
            );
            testWithFiles(
                    storage,
                    "udhr/udhr-en.txt",
                    EXPECTED_EN_NUMBER_OF_LETTERS,
                    EXPECTED_EN_NUMBER_OF_WORDS,
                    EXPECTED_EN_NUMBER_OF_SENTENCES,
                    EXPECTED_EN_COLEMAN_LIAU_INDEX
            );
        }
    }

    public void testWithFiles(
            Storage storage,
            String fileName,
            int expectedNbrOfLetters,
            int expectedNbrOfWords,
            int expectedNbrOfSentences,
            double expectedAutomatedReadabilityIndex) throws IOException {
        FlexibleParameters parameters = new FlexibleParameters(new String[]{"file="+TestHelper.getResource(fileName)});
        CorpusCreator creator = new CorpusCreator(storage, parameters);
        creator.run();

        DocumentAutomatedReadabilityIndex documentAutomatedReadabilityIndex = new DocumentAutomatedReadabilityIndex(storage, parameters);
        documentAutomatedReadabilityIndex.run();

        List<AutomatedReadabilityIndex> automatedReadabilityIndexes = documentAutomatedReadabilityIndex.getAutomatedReadabilityIndexes();

        for (AutomatedReadabilityIndex automatedReadabilityIndex : automatedReadabilityIndexes) {
            assert automatedReadabilityIndex.getNbrOfLetters() == expectedNbrOfLetters;
            assert automatedReadabilityIndex.getNbrOfWords() == expectedNbrOfWords;
            assert automatedReadabilityIndex.getNbrOfSentences() == expectedNbrOfSentences;
            assert automatedReadabilityIndex.getAutomatedReadabilityIndex() == expectedAutomatedReadabilityIndex;
        }
    }

    public void testWithRawText(Storage storage) throws IOException {
        FlexibleParameters parameters;
        parameters = new FlexibleParameters();
        parameters.addParameter("string", AutomatedReadabilityIndexTest.TEXT);

        RealCorpusCreator creator = new RealCorpusCreator(storage, parameters);
        creator.run();

        parameters = new FlexibleParameters();
        parameters.setParameter("corpus", creator.getStoredId());
        parameters.setParameter("tool", "corpus.DocumentAutomatedReadabilityIndex");

        DocumentAutomatedReadabilityIndex documentAutomatedReadabilityIndex = new DocumentAutomatedReadabilityIndex(storage, parameters);
        documentAutomatedReadabilityIndex.run();

        List<AutomatedReadabilityIndex> automatedReadabilityIndexes = documentAutomatedReadabilityIndex.getAutomatedReadabilityIndexes();

        for (AutomatedReadabilityIndex automatedReadabilityIndex : automatedReadabilityIndexes)
            // Assert with the expected values of the provided text above.
            AutomatedReadabilityIndexTest.assertAutomatedReadabilityIndexValues(automatedReadabilityIndex);
    }
}
