package io.bspk.oauth.xyz.json;

import java.io.IOException;
import java.text.ParseException;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.nimbusds.jose.jwk.JWK;

/**
 * @author jricher
 *
 */
@ReadingConverter
public class JWKDeserializer extends StdDeserializer<JWK> implements Converter<String, JWK> {

	/**
	 * @param src
	 */
	public JWKDeserializer() {
		super(JWK.class);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public JWK deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		JsonNode node = ctxt.readValue(p, JsonNode.class);

		// TODO: this might be a hack?
		try {
			return JWK.parse(node.toString());
		} catch (ParseException e) {
			throw new JsonParseException(p, "Couldn't create JWK", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public JWK convert(String source) {
		if (source == null) {
			return null;
		} else {
			try {
				return JWK.parse(source);
			} catch (ParseException e) {
				return null;
			}
		}
	}

}
