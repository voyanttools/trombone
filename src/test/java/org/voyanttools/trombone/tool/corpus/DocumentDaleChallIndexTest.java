package org.voyanttools.trombone.tool.corpus;

import org.junit.Test;
import org.voyanttools.trombone.model.DaleChallIndex;
import org.voyanttools.trombone.model.DaleChallIndexTest;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.fail;


public class DocumentDaleChallIndexTest {

    private static final String FILE_PATH_EN = "udhr/udhr-en.txt";
    private static final double EXPECTED_EN_DALE_CHALL_INDEX = 6.353838036580814;

    @Test
    public void testWithDefaultEasyWordsList() throws IOException {
        for (Storage storage : TestHelper.getDefaultTestStorages()) {
            System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());

            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"string="+DaleChallIndexTest.TEXT_1}), DaleChallIndexTest.EXPECTED_DALE_CHALL_INDEX_1);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"string="+DaleChallIndexTest.TEXT_2}), DaleChallIndexTest.EXPECTED_DALE_CHALL_INDEX_2);
            testWithGivenParameters(storage, new FlexibleParameters(new String[]{"file="+TestHelper.getResource(FILE_PATH_EN)}), EXPECTED_EN_DALE_CHALL_INDEX);
        }
    }

    @Test
    public void testWithGivenEasyWordsList() throws IOException {
        String easyWordsPath = "./src/test/resources/org/voyanttools/trombone/texts/keywords/easywords.en.txt";

        for (Storage storage : TestHelper.getDefaultTestStorages()) {
            System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());

            testWithGivenParameters(
                    storage,
                    new FlexibleParameters(new String[]{
                            "string="+DaleChallIndexTest.TEXT_1,
                            "easyWordsFile="+easyWordsPath
                    }),
                    DaleChallIndexTest.EXPECTED_DALE_CHALL_INDEX_1
            );
            testWithGivenParameters(
                    storage,
                    new FlexibleParameters(new String[]{
                            "string="+DaleChallIndexTest.TEXT_2,
                            "easyWordsFile="+easyWordsPath
                    }),
                    DaleChallIndexTest.EXPECTED_DALE_CHALL_INDEX_2
            );
            testWithGivenParameters(
                    storage,
                    new FlexibleParameters(new String[]{
                            "file="+TestHelper.getResource(FILE_PATH_EN),
                            "easyWordsFile="+easyWordsPath
                    }),
                    EXPECTED_EN_DALE_CHALL_INDEX
            );
        }
    }

    @Test
    public void testWithNonExistingPath() throws IOException {
        String nonExistingEasyWordsPath = "./non-existing-path";

        for (Storage storage : TestHelper.getDefaultTestStorages()) {
            FlexibleParameters parameters = new FlexibleParameters(new String[]{
                    "string="+DaleChallIndexTest.TEXT_1,
                    "easyWordsFile="+nonExistingEasyWordsPath
            });

            try {
                DocumentDaleChallIndex documentDaleChallIndex = new DocumentDaleChallIndex(storage, parameters);
                documentDaleChallIndex.run();
                fail("Should have raise RuntimeException");
            } catch (RuntimeException ignored) {}
        }
    }

    private void testWithGivenParameters(Storage storage, FlexibleParameters parameters, double expectedDaleChallIndex) throws IOException {
        CorpusCreator creator = new CorpusCreator(storage, parameters);
        creator.run();

        DocumentDaleChallIndex documentDaleChallIndex = new DocumentDaleChallIndex(storage, parameters);
        documentDaleChallIndex.run();

        List<DaleChallIndex> daleChallIndexes = documentDaleChallIndex.getDaleChallIndexes();

        for (DaleChallIndex daleChallIndex : daleChallIndexes) {
            assert daleChallIndex.getDaleChallIndex() == expectedDaleChallIndex;
        }
    }
}
