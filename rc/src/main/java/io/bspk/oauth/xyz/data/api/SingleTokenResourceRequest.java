package io.bspk.oauth.xyz.data.api;

import java.util.ArrayList;
import java.util.List;

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
public class SingleTokenResourceRequest implements ResourceRequest {

	private List<RequestedResource> resources = new ArrayList<>();

	@Override
	public boolean isMultiple() {
		return false;
	}

}
