package io.bspk.oauth.xyz.json;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.bspk.oauth.xyz.data.api.MultipleAwareField;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jricher
 *
 */
@JsonComponent
public class MultipleAwareFieldDeserializer<T> extends StdDeserializer<MultipleAwareField<T>>  implements ContextualDeserializer {

	@Getter
	@Setter
    private JavaType valueType;

	@Getter
	@Setter
	private JavaType listValueType;

	public MultipleAwareFieldDeserializer() {
		super (MultipleAwareField.class);
	}

	@Override
	public MultipleAwareField<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonToken token = p.currentToken();

		if (token == JsonToken.START_ARRAY) {
			// it's an array, parse it as a multi token request
			List<T> value = ctxt.readValue(p, getListValueType());

			return MultipleAwareField.of(value);

		} else if (token == JsonToken.START_OBJECT) {
			T value = ctxt.readValue(p, getValueType());

			return MultipleAwareField.of(value);
		} else {
			throw new JsonParseException(p, "Couldn't convert from JSON node type");
		}
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType wrapperType = property.getType();
        JavaType valueType = wrapperType.containedType(0);

        JavaType listValueType = ctxt.getTypeFactory().constructCollectionType(List.class, valueType);

        MultipleAwareFieldDeserializer deserializer = new MultipleAwareFieldDeserializer<>();
        deserializer.setValueType(valueType);
        deserializer.setListValueType(listValueType);

        return deserializer;
	}


}
