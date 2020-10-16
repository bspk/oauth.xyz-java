package io.bspk.oauth.xyz.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.bspk.oauth.xyz.data.api.MultiTokenResourceRequest;
import io.bspk.oauth.xyz.data.api.RequestedResource;
import io.bspk.oauth.xyz.data.api.ResourceRequest;
import io.bspk.oauth.xyz.data.api.SingleTokenResourceRequest;

/**
 * @author jricher
 *
 */
public class ResourceRequestDeserializer extends StdDeserializer<ResourceRequest> {

	public ResourceRequestDeserializer() {
		super (ResourceRequest.class);
	}

	@Override
	public ResourceRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonToken token = p.currentToken();

		if (token == JsonToken.START_ARRAY) {
			List<RequestedResource> value = p.readValueAs(new TypeReference<List<RequestedResource>>() {});

			return new SingleTokenResourceRequest()
				.setResources(value);

		} else if (token == JsonToken.START_OBJECT) {
			// it's an object, parse it out as a multiple token request
			Map<String, List<RequestedResource>> value = p.readValueAs(new TypeReference<Map<String, List<RequestedResource>>>() {});

			return MultiTokenResourceRequest.of(value);
		} else {
			throw new JsonParseException(p, "Couldn't convert from JSON node type");
		}
	}

}
