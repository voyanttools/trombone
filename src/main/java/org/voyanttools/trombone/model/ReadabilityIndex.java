package org.voyanttools.trombone.model;

import org.voyanttools.trombone.tool.util.TextParser;

import java.io.Serializable;

public abstract class ReadabilityIndex implements Serializable {

    protected int docIndex;
    protected String docId;

    protected TextParser text;

    public ReadabilityIndex(int documentIndex, String documentId, String textToParse) {
        text = new TextParser(textToParse);

        docIndex = documentIndex;
        docId = documentId;
    }

    abstract protected double calculateIndex(TextParser text);
}
