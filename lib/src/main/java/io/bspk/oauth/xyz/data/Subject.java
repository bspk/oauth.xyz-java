package io.bspk.oauth.xyz.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sailpoint.ietf.subjectidentifiers.model.SubjectIdentifier;
import com.sailpoint.ietf.subjectidentifiers.model.SubjectIdentifierFormats;

import io.bspk.oauth.xyz.data.Assertion.AssertionFormat;
import io.bspk.oauth.xyz.data.api.SubjectRequest;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Subject {

	private List<SubjectIdentifier> subIds;
	private List<Assertion> assertions;
	private Instant updatedAt;

	public static Subject of(SubjectRequest request, User user) {
		Subject c = new Subject();
		List<SubjectIdentifierFormats> subIdsRequest = request.getSubIdFormats();
		List<SubjectIdentifier> subIds = new ArrayList<>();

		if (subIdsRequest != null) {
			if (subIdsRequest.contains(SubjectIdentifierFormats.ISSUER_SUBJECT)) {
				subIds.add(new SubjectIdentifier.Builder()
					.format(SubjectIdentifierFormats.ISSUER_SUBJECT)
					.subject(user.getId())
					.issuer(user.getIss())
					.build());
			}
			if (subIdsRequest.contains(SubjectIdentifierFormats.EMAIL)) {
				subIds.add(new SubjectIdentifier.Builder()
					.format(SubjectIdentifierFormats.EMAIL)
					.email(user.getEmail())
					.build());
			}
			if (subIdsRequest.contains(SubjectIdentifierFormats.PHONE_NUMBER)) {
				subIds.add(new SubjectIdentifier.Builder()
					.format(SubjectIdentifierFormats.PHONE_NUMBER)
					.phoneNumber(user.getPhone())
					.build());
			}
			if (subIdsRequest.contains(SubjectIdentifierFormats.OPAQUE)) {
				subIds.add(new SubjectIdentifier.Builder()
					.format(SubjectIdentifierFormats.OPAQUE)
					.id(user.getId())
					.build());
			}
			// TODO: add other types

			c.setSubIds(subIds);
		}

		List<AssertionFormat> assertionRequest = request.getAssertionFormats();
		List<Assertion> assertions = new ArrayList<>();

		if (assertionRequest != null) {
			if (assertionRequest.contains(AssertionFormat.OIDC_ID_TOKEN) && user.getIdToken() != null) {
				assertions.add(new Assertion()
					.setFormat(AssertionFormat.OIDC_ID_TOKEN)
					.setValue(user.getIdToken().serialize()));
			}
			// TODO, add additional formats
			c.setAssertions(assertions);
		}

		c.setUpdatedAt(user.getUpdatedAt());
		return c;
	}

}
