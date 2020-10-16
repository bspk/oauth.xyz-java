package io.bspk.oauth.xyz.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Strings;

import io.bspk.oauth.xyz.data.api.HandleReplaceable;

/**
 * @author jricher
 *
 */
public class HandleAwareSerializer<T extends HandleReplaceable> extends JsonSerializer<T> {

	private JsonSerializer defaultSerializer;

	public HandleAwareSerializer(JsonSerializer defaultSerializer) {
		this.defaultSerializer = defaultSerializer;
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

		if (!Strings.isNullOrEmpty(value.getHandle())) {
			// it's a handle, send it back as a string
			gen.writeString(value.getHandle());
		} else {
			defaultSerializer.serialize(value, gen, serializers);
		}

	}

}
