/**
 * 
 */
package org.voyanttools.trombone.util;

import java.util.Locale;
import java.util.regex.Pattern;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;

/**
 * @author sgs
 *
 */
public class LangDetector {
	
	// English gets misidentified as Yoruba often enough that we're excluding it
	// https://github.com/pemistahl/lingua/issues/125
	private static final LanguageDetector detector = LanguageDetectorBuilder.fromAllLanguagesWithout(Language.YORUBA).withLowAccuracyMode().build();
	
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
		
		Language detectedLanguage = detector.detectLanguageOf(text);
		String lang1 = detectedLanguage.getIsoCode639_1().toString();
		
		if (lang1.equals("none")) {
			// check if it's Tibetan
			if (text.contains("\u0F0B")) { // TIBETAN MARK INTERSYLLABIC TSHEG
				lang1 = new Locale("bo").getLanguage();
			} else {
				lang1 = "";
			}
		}
		
		return lang1;
	}

}
