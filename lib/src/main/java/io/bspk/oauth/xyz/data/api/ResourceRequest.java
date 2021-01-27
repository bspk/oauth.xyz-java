package io.bspk.oauth.xyz.data.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Strings;

/**
 * @author jricher
 *
 */
public interface ResourceRequest {
	public enum TokenFlag {
		BEARER,
		SPLIT;

		@JsonCreator
		public static TokenFlag fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}

	}

	boolean isMultiple();

	public static SingleTokenResourceRequest ofReferences(String... references) {
		return new SingleTokenResourceRequest()
			.setAccess(Arrays.stream(references)
				.map(r -> new RequestedResource().setHandle(r))
				.collect(Collectors.toList()));
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

	public static MultiTokenResourceRequest of (SingleTokenResourceRequest... value) {
		return of(List.of(value));
	}

}
