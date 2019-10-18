package io.bspk.oauth.xyz.json;

import java.io.IOException;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nimbusds.jose.jwk.JWK;

/**
 * @author jricher
 *
 */
@WritingConverter
public class JWKSerializer extends JsonSerializer<JWK> implements Converter<JWK, String>{

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(JWK value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeObject(value.toJSONObject());
	}

	/* (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public String convert(JWK source) {
		if (source == null) {
			return null;
		} else {
			return source.toJSONString();
		}
	}

}
