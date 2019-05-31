package io.bspk.oauth.xyz.json;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;

import io.bspk.oauth.xyz.data.api.HandleReplaceable;

/**
 * @author jricher
 *
 */
public class HandleDeserializer<T extends HandleReplaceable> extends DelegatingDeserializer {

	public HandleDeserializer(JsonDeserializer<?> defaultDeserializer) {
		super(defaultDeserializer);
	}

	@Override
	public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode node = p.getCodec().readTree(p);

		if (node.isTextual()) {
			try {
				// if it's a plain string, treat it as a handle
				T newInstance = (T) handledType().getDeclaredConstructor().newInstance();
				return (T) newInstance.setHandle(node.asText());
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
				throw new JsonParseException(p, e.getMessage(), e);
			}
		} else if (node.isObject()) {
			// it's an object, parse it out as usual
			JsonParser p2 = node.traverse();
			if (p2.getCurrentToken() == null) {
				p2.nextToken();
			}
			return (T) super.deserialize(p2, ctxt);
		} else {
			throw new JsonParseException(p, "Couldn't convert from JSON node type");
		}
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer#newDelegatingInstance(com.fasterxml.jackson.databind.JsonDeserializer)
	 */
	@Override
	protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
		return new HandleDeserializer<>(newDelegatee);

	}

}
