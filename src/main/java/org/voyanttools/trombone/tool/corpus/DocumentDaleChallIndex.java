package org.voyanttools.trombone.tool.corpus;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.apache.commons.io.FileUtils;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DaleChallIndex;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@XStreamAlias("documentDaleChallIndex")
public class DocumentDaleChallIndex extends AbstractCorpusTool {

    @XStreamOmitField
    public static String DEFAULT_EASY_WORDS_FILE_PATH = "/org/voyanttools/trombone/readability/easywords.en.txt";

    private List<DaleChallIndex> daleChallIndexes;

    public DocumentDaleChallIndex(Storage storage, FlexibleParameters parameters) {
        super(storage, parameters);

        daleChallIndexes = new ArrayList<>();
    }

    @Override
    public void run(CorpusMapper corpusMapper) throws IOException {
        List<String> easyWords;

        if (parameters.containsKey("easyWordsFile")) {
            try {
                String easyWordsPath = parameters.getParameterValue("easyWordsFile");
                File easyWordsFile = new File(easyWordsPath);

                easyWords = getEasyWords(easyWordsFile);
            } catch (NoSuchFileException e) {
                throw new RuntimeException("Failed to find the easy words list file.");
            }

        } else
            try {
                File easyWordsFile = File.createTempFile("tmp", "tmp");
                easyWordsFile.deleteOnExit();

                InputStream inputStream = this.getClass().getResourceAsStream(DEFAULT_EASY_WORDS_FILE_PATH);
                FileUtils.copyInputStreamToFile(inputStream, easyWordsFile);

                easyWords = getEasyWords(easyWordsFile);
            } catch (NullPointerException e) {
                throw new RuntimeException("Failed to retrieved the easy words list.");
            }

        Corpus corpus = corpusMapper.getCorpus();

        for (String documentId : corpus.getDocumentIds()) {
            int documentIndex = corpus.getDocumentPosition(documentId);
            String text = corpus.getDocument(documentId).getDocumentString();

            daleChallIndexes.add(new DaleChallIndex(documentIndex, documentId, text, easyWords));
        }
    }

    public List<DaleChallIndex> getDaleChallIndexes() {
        return daleChallIndexes;
    }

    public static List<String> getEasyWords(File easyWordsFile) throws IOException {
        List<String> easyWords = Files.readAllLines(easyWordsFile.toPath());

        easyWords = removeComments(easyWords);

        return easyWords;
    }

    private static List<String> removeComments(List<String> easyWords) {
        return  easyWords.stream()
                .filter(word -> !word.contains("#"))
                .collect(Collectors.toList());
    }
}
