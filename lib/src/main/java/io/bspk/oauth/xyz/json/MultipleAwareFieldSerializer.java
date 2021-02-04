package io.bspk.oauth.xyz.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.bspk.oauth.xyz.data.api.MultipleAwareField;

/**
 * @author jricher
 *
 */
public class MultipleAwareFieldSerializer<T> extends JsonSerializer<MultipleAwareField<T>> {

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(MultipleAwareField<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else if (value.isMultiple()) {
			// it's a multiple-value object, write out the array
			// since "asMultiple" returns a List, this outputs an array, not a JSON object
			gen.writeObject(value.asMultiple());
		} else {
			// it's a single value, write out the value itself (usually an object)
			gen.writeObject(value.asSingle());
		}

	}

}
