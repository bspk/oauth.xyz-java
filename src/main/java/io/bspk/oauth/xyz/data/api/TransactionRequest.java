package io.bspk.oauth.xyz.data.api;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.Capability;
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

	private String handle;

	private InteractRequest interact;
	private DisplayRequest display;
	private UserRequest user;
	private List<ResourceRequest> resources;
	private KeyRequest keys;
	private String interactHandle;
	private List<Capability> capabilities;

}
