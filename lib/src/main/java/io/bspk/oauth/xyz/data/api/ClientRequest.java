package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ClientRequest extends HandleReplaceable<ClientRequest> {

	private String instanceId;
	private KeyRequest key;
	private DisplayRequest display;

	@Override
	public String getHandle() {
		return getInstanceId();
	}

	@Override
	public ClientRequest setHandle(String handle) {
		return setInstanceId(handle);
	}

}
