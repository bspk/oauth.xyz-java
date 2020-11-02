package io.bspk.oauth.xyz.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.crypto.Hash.HashMethod;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Callback {
	public enum CallbackMethod {
		REDIRECT,
		PUSHBACK,
		;

		@JsonCreator
		public static CallbackMethod fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}
	}

	private String uri;
	private String nonce;
	private CallbackMethod method;
	private HashMethod hashMethod = HashMethod.SHA3;

	public static Callback redirect() {
		return new Callback().setMethod(CallbackMethod.REDIRECT);
	}

	public static Callback pushback() {
		return new Callback().setMethod(CallbackMethod.PUSHBACK);
	}
}