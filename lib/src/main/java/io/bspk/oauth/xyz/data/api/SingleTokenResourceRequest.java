package io.bspk.oauth.xyz.data.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

	private List<RequestedResource> access = new ArrayList<>();
	private String label;
	private Set<TokenFlag> flags;

	@Override
	@JsonIgnore
	public boolean isMultiple() {
		return false;
	}

	@JsonIgnore
	public boolean isBearer() {
		return flags != null && flags.contains(TokenFlag.BEARER);
	}

	public static SingleTokenResourceRequest ofReferences(String... references) {
		return new SingleTokenResourceRequest()
			.setAccess(Arrays.stream(references)
				.map(r -> new RequestedResource().setHandle(r))
				.collect(Collectors.toList()));
	}

}
