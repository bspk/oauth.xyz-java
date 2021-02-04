package io.bspk.oauth.xyz.data.api;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.data.Capability;
import io.bspk.oauth.xyz.json.HandleAwareFieldDeserializer;
import io.bspk.oauth.xyz.json.HandleAwareFieldSerializer;
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
public class TransactionRequest {

	private InteractRequest interact;
	@JsonSerialize(using =  HandleAwareFieldSerializer.class)
	@JsonDeserialize(using = HandleAwareFieldDeserializer.class)
	private HandleAwareField<ClientRequest> client;
	@JsonSerialize(using =  HandleAwareFieldSerializer.class)
	@JsonDeserialize(using = HandleAwareFieldDeserializer.class)
	private HandleAwareField<UserRequest> user;
	@JsonSerialize(using =  MultipleAwareFieldSerializer.class)
	@JsonDeserialize(using = MultipleAwareFieldDeserializer.class)
	private MultipleAwareField<ResourceRequest> accessToken;
	private Set<Capability> capabilities;
	private SubjectRequest subject;

	@Tolerate
	@JsonIgnore
	public TransactionRequest setClient(ClientRequest client) {
		return setClient(HandleAwareField.of(client));
	}

	@Tolerate
	@JsonIgnore
	public TransactionRequest setClient(String client) {
		return setClient(HandleAwareField.of(client));
	}

	@Tolerate
	@JsonIgnore
	public TransactionRequest setUser(UserRequest user) {
		return setUser(HandleAwareField.of(user));
	}

	@Tolerate
	@JsonIgnore
	public TransactionRequest setUser(String user) {
		return setUser(HandleAwareField.of(user));
	}

	@Tolerate
	@JsonIgnore
	public TransactionRequest setAccessToken(ResourceRequest accessToken) {
		return setAccessToken(MultipleAwareField.of(accessToken));
	}

	@Tolerate
	@JsonIgnore
	public TransactionRequest setAccessToken(ResourceRequest... accessToken) {
		return setAccessToken(MultipleAwareField.of(accessToken));
	}

}
