package io.bspk.oauth.xyz.json;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.bspk.oauth.xyz.data.api.HandleAwareField;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jricher
 *
 */
@JsonComponent
public class HandleAwareFieldDeserializer<T> extends StdDeserializer<HandleAwareField<T>> implements ContextualDeserializer {

	@Getter
	@Setter
    private JavaType valueType;

	public HandleAwareFieldDeserializer() {
		super (HandleAwareField.class);
	}

	@Override
	public HandleAwareField<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonToken token = p.currentToken();

		if (token == JsonToken.VALUE_STRING) {
			// it's an array, parse it as a multi token request
			String value = p.readValueAs(new TypeReference<String>() {});

			return HandleAwareField.of(value);

		} else if (token == JsonToken.START_OBJECT) {
			T value = ctxt.readValue(p, getValueType());

			return HandleAwareField.of(value);
		} else {
			throw new JsonParseException(p, "Couldn't convert from JSON node type");
		}
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType wrapperType = property.getType();
        JavaType valueType = wrapperType.containedType(0);
        HandleAwareFieldDeserializer deserializer = new HandleAwareFieldDeserializer<>();
        deserializer.setValueType(valueType);
        return deserializer;
	}

}
