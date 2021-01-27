package io.bspk.oauth.xyz.data.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.base.Strings;

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

	private List<SingleTokenResourceRequest> requests = new ArrayList<>();

	@Override
	public boolean isMultiple() {
		return true;
	}

	public static MultiTokenResourceRequest of (SingleTokenResourceRequest... value) {
		return of(List.of(value));
	}

	public static MultiTokenResourceRequest of (List<SingleTokenResourceRequest> value) {
		MultiTokenResourceRequest m = new MultiTokenResourceRequest();
		value.forEach(v -> {
			if (Strings.isNullOrEmpty(v.getLabel())) {
				throw new IllegalArgumentException("Access request does not contain required 'label' field");
			}
			m.getRequests().add(v);
		});
		return m;
	}

	public static MultiTokenResourceRequest of(Map<String, List<RequestedResource>> value) {
		MultiTokenResourceRequest m = new MultiTokenResourceRequest();
		value.forEach((k, v) -> {
			m.getRequests().add(new SingleTokenResourceRequest()
				.setAccess(v)
				.setLabel(k));
		});
		return m;
	}

}
