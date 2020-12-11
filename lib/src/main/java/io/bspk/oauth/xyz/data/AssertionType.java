package io.bspk.oauth.xyz.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssertionType {
	ID_TOKEN,
	SAML2;

	@JsonCreator
	public static AssertionType fromJson(String key) {
		return key == null ? null :
			valueOf(key.toUpperCase());
	}

	@JsonValue
	public String toJson() {
		return name().toLowerCase();
	}

}