/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package postaggersalanguage.five;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.voyanttools.trombone.nlp.PosLemmas;

import com.shef.ac.uk.util.Util;

import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

/**
 *
 * @author ahmetaker
 */
public class POSTaggersALanguage {

	private String lang;
    private POSModel itsPOSModel = null;
    private SentenceModel itsSentenceModel = null;
    private TokenizerModel itsTokenizerModel = null;
    private Map<String, String> nounDic;
    private Map<String, String> adjDic;
    private Map<String, String> advDic;
    private Map<String, String> verbDic;
    private Map<String, String> detDic;
    private Map<String, String> pronDic;
    private Map<String, String> posMap;


    public POSTaggersALanguage(String lang) throws IOException {
    	this.lang = lang;
        nounDic = Util.loadDictionary("/postaggersalanguage/five/dictionaries/" + lang + "/nounDic.txt");
        adjDic = Util.loadDictionary("/postaggersalanguage/five/dictionaries/" + lang + "/adjDic.txt");
        advDic = Util.loadDictionary("/postaggersalanguage/five/dictionaries/" + lang + "/advDic.txt");
        verbDic = Util.loadDictionary("/postaggersalanguage/five/dictionaries/" + lang + "/verbDic.txt");
        detDic = Util.loadDictionary("/postaggersalanguage/five/dictionaries/" + lang + "/detDic.txt");
        pronDic = Util.loadDictionary("/postaggersalanguage/five/dictionaries/" + lang + "/pronounDic.txt");
        posMap = Util.getFileContentAsMap("/postaggersalanguage/five/universal-pos-tags/" + lang + "POSMapping.txt", "######", true);
	}

    public Span[] tokenizePos(String aSentence) throws InvalidFormatException, IOException {
        if (itsTokenizerModel == null) {
            InputStream is = Util.class.getResourceAsStream("/postaggersalanguage/five/tokenizerModels/" + lang + "-token.bin");
            itsTokenizerModel = new TokenizerModel(is);
            is.close();
        }
        Tokenizer tokenizer = new TokenizerME(itsTokenizerModel);
        Span[] tokens = tokenizer.tokenizePos(aSentence);


        //now apply also some rules!
        ArrayList<Span> array = new ArrayList<Span>();
        for (int i = 0; i < tokens.length; i++) {
            String token = aSentence.substring(tokens[i].getStart(), tokens[i].getEnd());
            if ("".equals(token)) {
                continue;
            }
            char chraters[] = token.toCharArray();
            Vector<String> take = new Vector<String>();
            StringBuffer buffer = new StringBuffer();
            for (int j = 0; j < chraters.length; j++) {
                String c = chraters[j] + "";
                if (Heuristics.isPunctuation(c)) {
                    String str = buffer.toString().trim();
                    if (!str.equals("")) {
                        take.add(buffer.toString());
                    }
                    buffer = new StringBuffer();
                    take.add(c);
                } else {
                    buffer.append(c);
                }
            }
            if (!buffer.toString().equals("")) {
                take.add(buffer.toString());
            }
            for (int j = 0; j < take.size(); j++) {
                String string = take.get(j);
                array.add(new Span(tokens[i].getStart(), tokens[i].getEnd(), string));
            }
        }

        Span a[] = new Span[array.size()];
        return array.toArray(a);

    }

    public Span[] sentenceDetectPos(String aText) throws InvalidFormatException, IOException {
    	if (itsSentenceModel == null) {
            InputStream is = Util.class.getResourceAsStream("/postaggersalanguage/five/setenceDetectionModels/" + lang + "-sent.bin");
            itsSentenceModel = new SentenceModel(is);
            is.close();
        }
        SentenceDetectorME sdetector = new SentenceDetectorME(itsSentenceModel);

        Span[] sentences = sdetector.sentPosDetect(aText);
        return sentences;
    }

    public String[] posTag(String aSentence[]) throws IOException {
        String posTaggedVersion[] = null;
        if (itsPOSModel == null) {
        	InputStream is = Util.class.getResourceAsStream("/postaggersalanguage/five/posModels/" + lang + "-pos-maxent.bin");
            itsPOSModel = new POSModel(is);
            is.close();
        }
        //PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
        POSTaggerME tagger = new POSTaggerME(itsPOSModel);

        posTaggedVersion = tagger.tag(aSentence);
        return posTaggedVersion;
    }
    
    public PosLemmas getLemmatized(String text) throws IOException {
    	PosLemmas posLemmas = new PosLemmas(text);
    	Span[] sentences = sentenceDetectPos(text);
    	for (Span sentence : sentences) {
    		int sentenceStart = sentence.getStart();
    		String sentenceString = text.substring(sentenceStart, sentence.getEnd());
    		Span[] tokens = tokenizePos(sentenceString);
    		String[] strings = Span.spansToStrings(tokens, sentenceString);
    		String[] pos = posTag(strings);
    		for (int i=0; i<tokens.length; i++) {
                String token = strings[i];
                String lemma = null;
                String posType = pos[i];
                if ("it".equalsIgnoreCase(lang)) {
                    posType = posType.substring(0, 1);
                }
                String generalType = posMap.get(posType.toLowerCase());
                
                if (Heuristics.isNumber(token)==false && Heuristics.isPunctuation(token)==false) {
                    
                    if (generalType != null) {
                        if ("NOUN".equalsIgnoreCase(generalType)) {
                            lemma = nounDic.get(token.toLowerCase());
                        } else if ("VERB".equalsIgnoreCase(generalType)) {
                            lemma = verbDic.get(token.toLowerCase());
                        } else if ("ADJ".equalsIgnoreCase(generalType)) {
                            lemma = adjDic.get(token.toLowerCase());
                        } else if ("ADV".equalsIgnoreCase(generalType)) {
                            lemma = advDic.get(token.toLowerCase());
                        } else if ("PRON".equalsIgnoreCase(generalType)) {
                            lemma = pronDic.get(token.toLowerCase());

                        }
                        if (!"nl".equalsIgnoreCase(lang) && lemma == null) {
                            try {
                                lemma = Lemmatizer.getLemma(token, lang, generalType);
                            } catch (Exception e) {
                                try {
                                    lemma = Lemmatizer.getLemma(token.toLowerCase(), lang, generalType);
                                } catch (Exception e2) {
                                }
                            }
                        }
                    }
                	posLemmas.add(token, generalType, lemma, sentenceStart+tokens[i].getStart(), sentenceStart+tokens[i].getEnd());
                }
//                if (lemma!=null) {
//                	posLemmas.add(token, generalType, lemma, sentenceStart+tokens[i].getStart(), sentenceStart+tokens[i].getEnd());
//                	spans.add(new Span(sentenceStart+tokens[i].getStart(), sentenceStart+tokens[i].getEnd(), lemma));
//                }
    			
    		}
    	}
    	return posLemmas;
    }

    public static void main(String args[]) throws InvalidFormatException, IOException {
    	String lang = "en";
        POSTaggersALanguage posTagger = new POSTaggersALanguage(lang);
        String text = "This time, it’s your turn: advise Parliament in the first LinkedIn discussion on an ongoing report. The rapporteur wants to hear your views @...(read more). --- Keywords ---";
        PosLemmas lemmas = posTagger.getLemmatized(text);
        Iterator<PosLemmas> iterator = lemmas.iterator();
        while (iterator.hasNext()) {
        	iterator.next();
        	System.out.println(lemmas.getCurrentTerm()+"-"+lemmas.getCurrentLemma());
        }
    }
}
