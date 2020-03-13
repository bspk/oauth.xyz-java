package io.bspk.oauth.xyz.data.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class MultiTokenResourceRequest implements ResourceRequest {

	private Map<String, SingleTokenResourceRequest> requests = new HashMap<>();

	@Override
	public boolean isMultiple() {
		return true;
	}

	public static MultiTokenResourceRequest of(Map<String, List<RequestedResource>> value) {
		MultiTokenResourceRequest m = new MultiTokenResourceRequest();
		value.forEach((k, v) -> {
			m.requests.put(k, new SingleTokenResourceRequest().setResources(v));
		});
		return m;
	}

}
