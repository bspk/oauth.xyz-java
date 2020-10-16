package io.bspk.oauth.xyz.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author jricher
 *
 */
public enum Capability {

	JWS;

	@JsonCreator
	public static Capability fromJson(String key) {
		return key == null ? null :
			valueOf(key.toUpperCase());
	}

	@JsonValue
	public String toJson() {
		return name().toLowerCase();
	}

}
