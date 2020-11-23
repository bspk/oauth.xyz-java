package io.bspk.oauth.xyz.data.api;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.Capability;
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
	private Set<Capability> capabilities;
	private Subject subject;
	private InteractResponse interact;


	@Value("${oauth.xyz.root}")
	private String baseUrl;

	/**
	 * @param t
	 * @return
	 */
	public static TransactionResponse of(Transaction t, String continueUri) {
		return of(t, null, continueUri);
	}


	public static TransactionResponse of(Transaction t, String instanceId, String continueUri) {

		return new TransactionResponse()
			.setAccessToken(AccessTokenResponse.of(t.getAccessToken()))
			.setMultipleAccessTokens(AccessTokenResponse.of(t.getMultipleAccessTokens()))
			.setInteract(InteractResponse.of(t.getInteract()))
			.setCont(new ContinueResponse()
				.setAccessToken(AccessTokenResponse.of(t.getContinueAccessToken()))
				.setUri(continueUri))
			.setInstanceId(instanceId)
			.setSubject(t.getSubject())
			.setCapabilities(t.getCapabilities());

	}

}
