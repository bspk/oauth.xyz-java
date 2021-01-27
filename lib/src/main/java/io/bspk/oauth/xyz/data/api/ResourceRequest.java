package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author jricher
 *
 */
public interface ResourceRequest {
	public enum TokenFlag {
		BEARER,
		SPLIT;

		@JsonCreator
		public static TokenFlag fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}

	}

	boolean isMultiple();

}
