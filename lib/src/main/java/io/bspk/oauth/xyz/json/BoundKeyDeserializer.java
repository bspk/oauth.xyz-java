package io.bspk.oauth.xyz.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;

import io.bspk.oauth.xyz.data.BoundKey;
import io.bspk.oauth.xyz.data.Key;

/**
 * @author jricher
 *
 */
public class BoundKeyDeserializer extends DelegatingDeserializer {

	public BoundKeyDeserializer(JsonDeserializer<?> defaultDeserializer) {
		super(defaultDeserializer);
	}

	@Override
	public BoundKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode node = p.getCodec().readTree(p);

		if (node.isBoolean()) {
			// if it's a boolean, handle it as client-bound or unbound but with no specified key
			return new BoundKey().setClientKey(node.asBoolean());
		} else if (node.isObject()) {
			// it's an object, parse it out as a key and set it to the explicitly bound key
			JsonParser p2 = node.traverse();
			if (p2.getCurrentToken() == null) {
				p2.nextToken();
			}
			Key key = (Key) super.deserialize(p2, ctxt);
			return new BoundKey().setKey(key);
		} else {
			throw new JsonParseException(p, "Couldn't convert from JSON node type");
		}
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer#newDelegatingInstance(com.fasterxml.jackson.databind.JsonDeserializer)
	 */
	@Override
	protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
		return new HandleAwareDeserializer<>(newDelegatee);

	}


}
