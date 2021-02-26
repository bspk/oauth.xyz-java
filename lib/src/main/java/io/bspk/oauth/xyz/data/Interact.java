package io.bspk.oauth.xyz.data;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.crypto.Hash.HashMethod;
import io.bspk.oauth.xyz.data.InteractFinish.CallbackMethod;
import io.bspk.oauth.xyz.data.api.InteractRequest;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Interact {

	public enum InteractStart {
		REDIRECT,
		APP,
		USER_CODE;

		@JsonCreator
		public static InteractStart fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}
	}

	private Set<InteractStart> startMethods = Collections.emptySet();
	private String interactionUrl;
	private String appUrl;
	private String interactId;
	private String serverNonce;
	private String clientNonce;
	private String callbackUri;
	private String interactRef;
	private String userCode;
	private String userCodeUrl;
	private CallbackMethod callbackMethod;
	private HashMethod callbackHashMethod;

	/**
	 * @param interact
	 * @return
	 */
	public static Interact of(InteractRequest interact) {

		Optional<InteractFinish> interactFinish = Optional.ofNullable(interact.getFinish());

		return new Interact()
			.setStartMethods(Optional.ofNullable(interact.getStart()).orElse(Collections.emptySet()))
			.setCallbackMethod(interactFinish.map(InteractFinish::getMethod).orElse(null))
			.setCallbackUri(interactFinish.map(InteractFinish::getUri).orElse(null))
			.setClientNonce(interactFinish.map(InteractFinish::getNonce).orElse(null))
			.setCallbackHashMethod(interactFinish.map(InteractFinish::getHashMethod).orElse(null));
	}

}
