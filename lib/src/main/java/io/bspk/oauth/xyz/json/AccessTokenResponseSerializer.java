package io.bspk.oauth.xyz.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.bspk.oauth.xyz.data.api.AccessTokenResponse;
import io.bspk.oauth.xyz.data.api.MultiAccessTokenResponse;

/**
 * @author jricher
 *
 */
public class AccessTokenResponseSerializer extends JsonSerializer<AccessTokenResponse> {

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(AccessTokenResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else if (value.isMultiple()) {
			gen.writeObject(((MultiAccessTokenResponse) value).getResponses());
		} else {
			gen.writeObject(value);
		}

	}

}
