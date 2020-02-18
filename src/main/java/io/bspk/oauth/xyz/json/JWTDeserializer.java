package io.bspk.oauth.xyz.json;

import java.io.IOException;
import java.text.ParseException;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

/**
 * @author jricher
 *
 */
@ReadingConverter
public class JWTDeserializer extends StdDeserializer<JWT> implements Converter<String, JWT> {

	/**
	 * @param src
	 */
	public JWTDeserializer() {
		super(JWT.class);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public JWT deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		try {
			return JWTParser.parse(ctxt.readValue(p, String.class));
		} catch (ParseException e) {
			throw new JsonParseException(p, "Couldn't create JWT", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public JWT convert(String source) {
		if (source == null) {
			return null;
		} else {
			try {
				return JWTParser.parse(source);
			} catch (ParseException e) {
				return null;
			}
		}
	}

}
