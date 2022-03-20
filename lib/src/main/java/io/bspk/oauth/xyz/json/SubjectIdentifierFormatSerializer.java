package io.bspk.oauth.xyz.json;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sailpoint.ietf.subjectidentifiers.model.SubjectIdentifierFormats;

/**
 * @author jricher
 *
 */
@JsonComponent
public class SubjectIdentifierFormatSerializer extends JsonSerializer<SubjectIdentifierFormats> {

	@Override
	public void serialize(SubjectIdentifierFormats value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeObject(value.toString());
	}

}
