package io.bspk.oauth.xyz.json;

import java.io.IOException;
import java.time.Instant;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * @author jricher
 *
 */
@ReadingConverter
public class InstantDeserializer extends StdDeserializer<Instant> implements Converter<String, Instant> {

	public InstantDeserializer() {
		super(Instant.class);
	}

	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String node = ctxt.readValue(p, String.class);
		Instant i = Instant.parse(node);
		return i;
	}

	/* (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public Instant convert(String source) {
		if (source != null) {
			return Instant.parse(source);
		} else {
			return null;
		}
	}

}
