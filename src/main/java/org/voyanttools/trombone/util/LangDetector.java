/**
 * 
 */
package org.voyanttools.trombone.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.google.common.base.Splitter;

/**
 * @author sgs
 *
 */
public class LangDetector {
	
	private final static int MAX_CHARS_PER_TEXT_CHUNK = 100000;
	
	private static final LanguageDetector detector = LanguageDetectorBuilder.fromAllLanguages()
		// https://github.com/pemistahl/lingua/?tab=readme-ov-file#915-low-accuracy-mode-versus-high-accuracy-mode
		.withLowAccuracyMode()
		.build();
	
	private static Pattern tagStripper = Pattern.compile("<.+?>", Pattern.DOTALL);
	
	public static String detect(String text, FlexibleParameters parameters) {
		return parameters.containsKey("language") ? new Locale(parameters.getParameterValue("language")).getLanguage() : detect(text);
	}
	public static String detect(String text) {

		if (text == null) return "";
		
		text = text.trim();
		
		// quick and dirty tags stripper
		if (text.startsWith("<")) {
			text = tagStripper.matcher(text).replaceAll("").trim();
		}
		
		// determine chunk size
		// aiming for around 3 chunks of MAX_CHARS_PER_TEXT_CHUNK each
		int chunkLength = MAX_CHARS_PER_TEXT_CHUNK;
		int textLength = text.length();
		if (textLength <= 240) {
			// special handling for small text size and low accuracy mode ( https://github.com/pemistahl/lingua/?tab=readme-ov-file#915-low-accuracy-mode-versus-high-accuracy-mode )
			StringBuilder sb = new StringBuilder(text);
			while (sb.length() < 500) {
				sb.append(" ");
				sb.append(text);
			}
			text = sb.toString();
			chunkLength = text.length();
		} else if (textLength < MAX_CHARS_PER_TEXT_CHUNK * 3) {
			chunkLength = (int) Math.ceil(textLength / 3);
		}
		
		// detect the language for each chunk
		// then determine the most frequent language (mode)
		List<String> chunkLangs = Splitter.fixedLength(chunkLength).splitToStream(text).limit(3)
			.map(t -> { return detector.detectLanguageOf(t).getIsoCode639_1().toString(); }).collect(Collectors.toList());
		String modeLang = chunkLangs.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
			.entrySet().stream().max(Comparator.comparing(Entry::getValue)).get().getKey();
		
		if (modeLang.equals("none")) {
			// check if it's Tibetan
			if (text.contains("\u0F0B")) { // TIBETAN MARK INTERSYLLABIC TSHEG
				modeLang = new Locale("bo").getLanguage();
			} else {
				modeLang = "";
			}
		}
		
		return modeLang;
	}
	
	public static void main(String args[]) throws IOException {
		String text = "";
		URL url;
		URLConnection c;
		InputStream is = null;
		try {
			url = new URL("https://www.gutenberg.org/cache/epub/158/pg158.txt"); // Emma by Jane Austen
			c = url.openConnection();
			is = c.getInputStream();
			text = IOUtils.toString(is, "UTF-8");
		} finally {
			if (is != null) {
				is.close();
			}
		}
		
		LanguageDetector testDetector = LanguageDetectorBuilder.fromAllLanguages()
//			.withLowAccuracyMode() // even without low accuracy mode it's still wrong
			.build();
			
		Language detectedLanguage = testDetector.detectLanguageOf(text);
		String lang1 = detectedLanguage.getIsoCode639_1().toString(); // should be "yo"
		System.out.println("no chunking: "+lang1);
		
		String lang2 = LangDetector.detect(text); // should be "en"
		System.out.println("chunking: "+lang2);
	}

}
