package io.bspk.oauth.xyz.json;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.bspk.oauth.xyz.data.BoundKey;

/**
 * @author jricher
 *
 */
@JsonComponent
public class BoundKeySerializer extends JsonSerializer<BoundKey> {

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(BoundKey value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else {
			if (value.isClientKey()) {
				// bind to client key
				gen.writeBoolean(true);
			} else if (value.getKey() == null || value.getKey().getProof() == null) {
				// unbound
				gen.writeBoolean(false);
			} else {
				gen.writeObject(value.getKey());
			}
			// TODO: string value
		}
	}

}
