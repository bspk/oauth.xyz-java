package io.bspk.oauth.xyz.data.api;

import lombok.Data;

/**
 * @author jricher
 *
 */
@Data
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
