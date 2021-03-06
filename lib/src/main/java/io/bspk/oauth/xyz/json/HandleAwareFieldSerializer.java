package io.bspk.oauth.xyz.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.bspk.oauth.xyz.data.api.HandleAwareField;

/**
 * @author jricher
 *
 */
public class HandleAwareFieldSerializer<T> extends JsonSerializer<HandleAwareField<T>> {

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(HandleAwareField<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else if (value.isHandled()) {
			// it's a handle, write a string
			gen.writeString(value.asHandle());
		} else {
			// it's a value, write the object
			gen.writeObject(value.asValue());
		}

	}

}
