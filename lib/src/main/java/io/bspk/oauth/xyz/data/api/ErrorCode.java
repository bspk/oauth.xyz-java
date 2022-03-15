package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorCode {
	INVALID_REQUEST,
	INVALID_CLIENT,
	USER_DENIED,
	TOO_FAST,
	UNKNOWN_REQUEST,
	REQUEST_DENIED;

	@JsonCreator
	public static ErrorCode fromJson(String key) {
		return key == null ? null :
			valueOf(key.toUpperCase());
	}

	@JsonValue
	public String toJson() {
		return name().toLowerCase();
	}

}