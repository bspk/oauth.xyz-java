package io.bspk.oauth.xyz.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SubjectIdentifier {
	private String subjectType; // TODO: make this an enum
	private String sub;
	private String iss;
	private String email;
	private String phoneNumber;
	private String uri;
}

