package io.bspk.oauth.xyz.data.api;

import java.util.Set;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.data.Capability;
import io.bspk.oauth.xyz.json.ResourceRequestDeserializer;
import io.bspk.oauth.xyz.json.ResourceRequestSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionRequest {

	private InteractRequest interact;
	private ClientRequest client;
	private UserRequest user;
	@JsonSerialize(using = ResourceRequestSerializer.class)
	@JsonDeserialize(using = ResourceRequestDeserializer.class)
	private ResourceRequest accessToken;
	private Set<Capability> capabilities;
	private SubjectRequest subject;

}
