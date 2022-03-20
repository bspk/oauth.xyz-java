package io.bspk.oauth.xyz.json;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.sailpoint.ietf.subjectidentifiers.model.SubjectIdentifierFormats;

/**
 * @author jricher
 *
 */
@JsonComponent
public class SubjectIdentifierFormatDeserializer extends StdDeserializer<SubjectIdentifierFormats> {

	/**
	 * @param vc
	 */
	protected SubjectIdentifierFormatDeserializer() {
		super(SubjectIdentifierFormats.class);
	}

	@Override
	public SubjectIdentifierFormats deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		JsonNode node = ctxt.readValue(p, JsonNode.class);

		return SubjectIdentifierFormats.enumByName(node.asText());

	}



}
