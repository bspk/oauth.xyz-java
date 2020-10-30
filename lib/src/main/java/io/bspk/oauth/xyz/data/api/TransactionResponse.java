package io.bspk.oauth.xyz.data.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.Capability;
import io.bspk.oauth.xyz.data.Client;
import io.bspk.oauth.xyz.data.Interact;
import io.bspk.oauth.xyz.data.Subject;
import io.bspk.oauth.xyz.data.Transaction;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionResponse {

	@JsonProperty("continue") // "continue" is a java keyword
	private ContinueResponse cont;
	private String userHandle;
	private String instanceId;
	private AccessTokenResponse accessToken;
	private Map<String, AccessTokenResponse> multipleAccessTokens;
	private String interactionUrl;
	private String shortInteractionUrl;
	private String pushbackServerNonce;
	private String callbackServerNonce;
	private String userCodeUrl;
	private String userCode;
	private Set<Capability> capabilities;
	private Subject subject;


	@Value("${oauth.xyz.root}")
	private String baseUrl;

	/**
	 * @param t
	 * @return
	 */
	public static TransactionResponse of(Transaction t, String continueUri) {
		return of(t, null, continueUri);
	}


	public static TransactionResponse of(Transaction t, Client c, String continueUri) {

		Optional<Interact> interact = Optional.ofNullable(t.getInteract());
		Optional<Client> client = Optional.ofNullable(c);

		return new TransactionResponse()
			.setAccessToken(AccessTokenResponse.of(t.getAccessToken()))
			.setMultipleAccessTokens(AccessTokenResponse.of(t.getMultipleAccessTokens()))
			.setInteractionUrl(interact
				.map(Interact::getInteractionUrl)
				.orElse(null))
			.setUserCodeUrl(interact
				.map(Interact::getUserCodeUrl)
				.orElse(null))
			.setUserCode(interact
				.map(Interact::getUserCode)
				.orElse(null))
			.setCallbackServerNonce(interact
				.map(Interact::getServerNonce)
				.orElse(null))
			.setPushbackServerNonce(interact
				.map(Interact::getServerNonce)
				.orElse(null))
			.setCont(new ContinueResponse()
				.setAccessToken(AccessTokenResponse.ofClientBoundToken(t.getContinueAccessToken()))
				.setUri(continueUri))
			.setInstanceId(client
				.map(Client::getInstanceId)
				.orElse(null))
			.setSubject(t.getSubject())
			.setCapabilities(t.getCapabilities());

	}

}
