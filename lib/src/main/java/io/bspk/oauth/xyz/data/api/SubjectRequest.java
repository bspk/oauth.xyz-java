package io.bspk.oauth.xyz.data.api;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.sailpoint.ietf.subjectidentifiers.model.SubjectIdentifierFormats;

import io.bspk.oauth.xyz.data.Assertion.AssertionFormat;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SubjectRequest {

	private static class SubjectIdentifierSerializer extends StdConverter<SubjectIdentifierFormats, String> {
		@Override
		public String convert(SubjectIdentifierFormats value) {
			return value.name().toLowerCase();
		}
	}

	private static class SubjectIdentifierDeserializer extends StdConverter<String, SubjectIdentifierFormats> {
		@Override
		public SubjectIdentifierFormats convert(String value) {
			return value == null ? null :
				SubjectIdentifierFormats.valueOf(value.toUpperCase());
		}
	}

	@JsonSerialize(contentConverter = SubjectIdentifierSerializer.class)
	@JsonDeserialize(contentConverter = SubjectIdentifierDeserializer.class)
	private List<SubjectIdentifierFormats> subIdFormats;
	private List<AssertionFormat> assertionFormats;

	public static SubjectRequest ofSubjectFormats(SubjectIdentifierFormats... formats) {
		return new SubjectRequest()
			.setSubIdFormats(List.of(formats));
	}
}
