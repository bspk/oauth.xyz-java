package io.bspk.oauth.xyz.http;

import org.greenbytes.http.sfv.Dictionary;
import org.greenbytes.http.sfv.Parser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * @author jricher
 *
 */
@Component
public class StructuredDictionaryConverter implements Converter<String, Dictionary> {

	@Override
	public Dictionary convert(String source) {
		if (!Strings.isNullOrEmpty(source)) {
			return Parser.parseDictionary(source);
		} else {
			return null;
		}
	}

}
