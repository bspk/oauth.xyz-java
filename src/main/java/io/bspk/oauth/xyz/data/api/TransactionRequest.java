package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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

	private String transactionHandle;
	private String clientHandle;
	private String userHandle;
	private String interactionHandle;
	private String resourceHandle;

	private InteractRequest interact;
	private ClientRequest client;
	private UserRequest user;
	private ResourceRequest resource;


}
