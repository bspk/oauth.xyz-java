package io.bspk.oauth.xyz.json;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.bspk.oauth.xyz.data.api.AccessTokenResponse;
import io.bspk.oauth.xyz.data.api.ResourceRequest;
import io.bspk.oauth.xyz.data.api.SingleAccessTokenResponse;

/**
 * @author jricher
 *
 */
public class AccessTokenResponseDeserializer extends StdDeserializer<AccessTokenResponse> {

	public AccessTokenResponseDeserializer() {
		super (ResourceRequest.class);
	}

	@Override
	public AccessTokenResponse deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonToken token = p.currentToken();

		if (token == JsonToken.START_ARRAY) {
			// it's an array, parse it as a multi token request
			List<SingleAccessTokenResponse> value = p.readValueAs(new TypeReference<List<SingleAccessTokenResponse>>() {});

			return AccessTokenResponse.of(value);

		} else if (token == JsonToken.START_OBJECT) {
			SingleAccessTokenResponse value = p.readValueAs(new TypeReference<SingleAccessTokenResponse>() {});

			return value;
		} else {
			throw new JsonParseException(p, "Couldn't convert from JSON node type");
		}
	}

}
