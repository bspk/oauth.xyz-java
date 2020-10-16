package io.bspk.oauth.xyz.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
	private AssertionSet assertions;
	private Instant updatedAt;

	public static Subject of(SubjectRequest request, User user) {
		Subject c = new Subject();
		List<String> subIdsRequest = request.getSubIds();
		List<SubjectIdentifier> subIds = new ArrayList<>();

		if (subIdsRequest != null) {
			if (subIdsRequest.contains("iss-sub")) {
				subIds.add(new SubjectIdentifier()
					.setSubjectType("iss-sub")
					.setSub(user.getId())
					.setIss(user.getIss()));
			}
			if (subIdsRequest.contains("email")) {
				subIds.add(new SubjectIdentifier()
					.setSubjectType("email")
					.setEmail(user.getEmail()));
			}
			if (subIdsRequest.contains("phone-number")) {
				subIds.add(new SubjectIdentifier()
					.setSubjectType("phone_number")
					.setPhoneNumber(user.getPhone()));
			}
			// TODO: add other types

			c.setSubIds(subIds);
		}
		if (request.getAssertions() != null) {
			AssertionSet a = new AssertionSet();
			if (request.getAssertions().contains("oidc_id_token")) {
				a.setOidcIdToken(user.getIdToken());
			}
		}

		// TODO: this should be an additional field, somehow?
		c.setUpdatedAt(user.getUpdatedAt());
		return c;
	}

}
