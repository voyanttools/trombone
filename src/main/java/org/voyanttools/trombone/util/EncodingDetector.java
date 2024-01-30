package org.voyanttools.trombone.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import com.sigpwned.chardet4j.Chardet;

public class EncodingDetector {
	public static Charset detect(InputStream input) throws IOException {
		Optional<Charset> cs = Chardet.detectCharset(IOUtils.toByteArray(input));
		if (cs.isPresent()) return cs.get();
		else return Charset.forName("UTF-8");
	}
}
