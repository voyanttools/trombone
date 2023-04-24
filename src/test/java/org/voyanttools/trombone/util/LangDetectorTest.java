package org.voyanttools.trombone.util;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;

public class LangDetectorTest {
	@Test
	public void testLang() throws IOException {
		for (Storage storage : TestHelper.getDefaultTestStorages()) {
			System.out.println("Testing with "+storage.getClass().getSimpleName()+": "+storage.getLuceneManager().getClass().getSimpleName());
			testLang(storage);
		}
	}
	
	public void testLang(Storage storage) throws IOException {
		String enText = IOUtils.toString(new FileInputStream(TestHelper.getResource("udhr/udhr-en.txt")), StandardCharsets.UTF_8);
		String esText = IOUtils.toString(new FileInputStream(TestHelper.getResource("udhr/udhr-es.txt")), StandardCharsets.UTF_8);
		String frText = IOUtils.toString(new FileInputStream(TestHelper.getResource("udhr/udhr-fr.txt")), StandardCharsets.UTF_8);
		String bo1Text = IOUtils.toString(new FileInputStream(TestHelper.getResource("i18n/bo_tibetan_segmented_utf8.txt")), StandardCharsets.UTF_8);
		String bo2Text = IOUtils.toString(new FileInputStream(TestHelper.getResource("i18n/bo_tibetan_utf8.txt")), StandardCharsets.UTF_8);
		String grText1 = IOUtils.toString(new FileInputStream(TestHelper.getResource("i18n/voyant_test_el.txt")), StandardCharsets.UTF_8);
		String grText2 = IOUtils.toString(new FileInputStream(TestHelper.getResource("i18n/voyant_test_grc_oxia.txt")), StandardCharsets.UTF_8);
		String grText3 = IOUtils.toString(new FileInputStream(TestHelper.getResource("i18n/voyant_test_grc_tonos_nfc.txt")), StandardCharsets.UTF_8);
		String zhText1 = IOUtils.toString(new FileInputStream(TestHelper.getResource("i18n/zh_segmented_utf8.txt")), StandardCharsets.UTF_8);
		String zhText2 = IOUtils.toString(new FileInputStream(TestHelper.getResource("i18n/zh_utf8.txt")), StandardCharsets.UTF_8);
		String enXml = IOUtils.toString(new FileInputStream(TestHelper.getResource("xml/fictionbook.xml")), StandardCharsets.UTF_8);
		
		assertEquals(new Locale("en").getLanguage(), LangDetector.detect(enText));
		assertEquals(new Locale("es").getLanguage(), LangDetector.detect(esText));
		assertEquals(new Locale("fr").getLanguage(), LangDetector.detect(frText));
		
		assertEquals(new Locale("bo").getLanguage(), LangDetector.detect(bo1Text));
		assertEquals(new Locale("bo").getLanguage(), LangDetector.detect(bo2Text));
		
		assertEquals(new Locale("el").getLanguage(), LangDetector.detect(grText1));
		assertEquals(new Locale("el").getLanguage(), LangDetector.detect(grText2));
		assertEquals(new Locale("el").getLanguage(), LangDetector.detect(grText3));
		
		assertEquals(new Locale("zh").getLanguage(), LangDetector.detect(zhText1));
		assertEquals(new Locale("zh").getLanguage(), LangDetector.detect(zhText2));
		
		assertEquals(new Locale("en").getLanguage(), LangDetector.detect(enXml));
	}
}
