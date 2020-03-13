package io.bspk.oauth.xyz.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.bspk.oauth.xyz.data.api.MultiTokenResourceRequest;
import io.bspk.oauth.xyz.data.api.RequestedResource;
import io.bspk.oauth.xyz.data.api.SingleTokenResourceRequest;

/**
 * @author jricher
 *
 */
public class ResourceRequestSerializer<ResourceRequest> extends JsonSerializer<ResourceRequest> {

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(ResourceRequest value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else if (value instanceof MultiTokenResourceRequest) {
			gen.writeObject(((MultiTokenResourceRequest) value).getRequests());
		} else if (value instanceof SingleTokenResourceRequest) {
			gen.writeStartArray();
			for (RequestedResource r : ((SingleTokenResourceRequest) value).getResources()) {
				gen.writeObject(r);
			}
			gen.writeEndArray();
		} else {
			throw new JsonGenerationException("Couldn't serialize resource request", gen);
		}

	}

}
