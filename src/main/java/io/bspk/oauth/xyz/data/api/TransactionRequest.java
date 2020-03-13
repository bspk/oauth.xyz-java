package io.bspk.oauth.xyz.data.api;

import java.util.List;

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

	private String handle;

	private InteractRequest interact;
	private DisplayRequest display;
	private UserRequest user;
	@JsonSerialize(using = ResourceRequestSerializer.class)
	@JsonDeserialize(using = ResourceRequestDeserializer.class)
	private ResourceRequest resources;
	private KeyRequest keys;
	private String interactRef;
	private List<Capability> capabilities;
	private ClaimsRequest claims;

}
