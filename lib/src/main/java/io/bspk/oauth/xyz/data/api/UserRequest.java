package io.bspk.oauth.xyz.data.api;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sailpoint.ietf.subjectidentifiers.model.SubjectIdentifier;

import io.bspk.oauth.xyz.data.Assertion;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserRequest {

	private List<SubjectIdentifier> subIds;
	private List<Assertion> assertions;

}
