/**
 * 
 */
package org.voyanttools.trombone.util;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	
	private static final LanguageDetector detector = LanguageDetectorBuilder.fromAllLanguagesWithout(
			// English gets misidentified often enough that we're excluding more rare languages
			// https://github.com/pemistahl/lingua/issues/125
			Language.ESPERANTO,
			Language.SHONA, Language.SOTHO, Language.SWAHILI,
			Language.TAGALOG, Language.TSWANA, Language.TSONGA,
			Language.XHOSA, Language.YORUBA
		)
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
		if (textLength <= 120) {
			// special handling for 120 character limit and low accuracy mode ( https://github.com/pemistahl/lingua/?tab=readme-ov-file#915-low-accuracy-mode-versus-high-accuracy-mode )
			StringBuilder sb = new StringBuilder(text);
			sb.append(text);
			sb.append(text);
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

}
