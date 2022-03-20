package io.bspk.oauth.xyz.data.api;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sailpoint.ietf.subjectidentifiers.model.SubjectIdentifierFormats;

import io.bspk.oauth.xyz.data.Assertion.AssertionFormat;
import io.bspk.oauth.xyz.json.SubjectIdentifierFormatDeserializer;
import io.bspk.oauth.xyz.json.SubjectIdentifierFormatSerializer;
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

	@JsonSerialize(contentUsing = SubjectIdentifierFormatSerializer.class)
	@JsonDeserialize(contentUsing = SubjectIdentifierFormatDeserializer.class)
	private List<SubjectIdentifierFormats> subIdFormats;
	private List<AssertionFormat> assertionFormats;

	public static SubjectRequest ofSubjectFormats(SubjectIdentifierFormats... formats) {
		return new SubjectRequest()
			.setSubIdFormats(List.of(formats));
	}
}
