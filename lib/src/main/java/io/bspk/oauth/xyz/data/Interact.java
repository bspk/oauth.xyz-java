package io.bspk.oauth.xyz.data;

import java.util.Optional;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.crypto.Hash.HashMethod;
import io.bspk.oauth.xyz.data.Callback.CallbackMethod;
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

	/** request parameters */
	private boolean canRedirect;
	private boolean canUserCode;
	private boolean canDidComm;
	private boolean canDidCommQuery;

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

		Optional<Callback> callback = Optional.ofNullable(interact.getCallback());

		return new Interact()
			.setCanDidComm(Optional.ofNullable(interact.getDidComm()).orElse(Boolean.FALSE))
			.setCanDidCommQuery(Optional.ofNullable(interact.getDidCommQuery()).orElse(Boolean.FALSE))
			.setCanUserCode(Optional.ofNullable(interact.getUserCode()).orElse(Boolean.FALSE))
			.setCanRedirect(Optional.ofNullable(interact.getRedirect()).orElse(Boolean.FALSE))
			.setCallbackMethod(callback.map(Callback::getMethod).orElse(null))
			.setCallbackUri(callback.map(Callback::getUri).orElse(null))
			.setClientNonce(callback.map(Callback::getNonce).orElse(null))
			.setCallbackHashMethod(callback.map(Callback::getHashMethod).orElse(null));
	}

}
