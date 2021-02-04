package io.bspk.oauth.xyz.data.api;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.data.Capability;
import io.bspk.oauth.xyz.data.Subject;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.json.MultipleAwareFieldDeserializer;
import io.bspk.oauth.xyz.json.MultipleAwareFieldSerializer;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

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
	@JsonSerialize(using =  MultipleAwareFieldSerializer.class)
	@JsonDeserialize(using = MultipleAwareFieldDeserializer.class)
	private MultipleAwareField<AccessTokenResponse> accessToken;
	private Set<Capability> capabilities;
	private Subject subject;
	private InteractResponse interact;


	@Value("${oauth.xyz.root}")
	private String baseUrl;

	@Tolerate
	@JsonIgnore
	public TransactionResponse setAccessToken(AccessTokenResponse accessToken) {
		return setAccessToken(MultipleAwareField.of(accessToken));
	}

	@Tolerate
	@JsonIgnore
	public TransactionResponse setAccessToken(AccessTokenResponse... accessToken) {
		return setAccessToken(MultipleAwareField.of(accessToken));
	}

	public static TransactionResponse of(Transaction t, String continueUri) {
		return of(t, null, continueUri);
	}


	public static TransactionResponse of(Transaction t, String instanceId, String continueUri) {

		return new TransactionResponse()
			.setAccessToken(MultipleAwareField.of(t.getAccessToken(), AccessTokenResponse::of))
			.setInteract(InteractResponse.of(t.getInteract()))
			.setCont(new ContinueResponse()
				.setAccessToken(AccessTokenResponse.of(t.getContinueAccessToken()))
				.setUri(continueUri))
			.setInstanceId(instanceId)
			.setSubject(t.getSubject())
			.setCapabilities(t.getCapabilities());

	}

}
