package io.bspk.oauth.xyz.json;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.bspk.oauth.xyz.data.api.HandleAwareField;

/**
 * @author jricher
 *
 */
@JsonComponent
public class HandleAwareFieldSerializer<T> extends JsonSerializer<HandleAwareField<T>> {

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(HandleAwareField<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else if (value.isHandled()) {
			gen.writeString(value.asHandle());
		} else {
			gen.writeObject(value.asValue());
		}

	}

}
