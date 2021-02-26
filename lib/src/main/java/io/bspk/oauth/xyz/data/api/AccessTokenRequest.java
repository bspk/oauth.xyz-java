package io.bspk.oauth.xyz.data.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.json.HandleAwareFieldDeserializer;
import io.bspk.oauth.xyz.json.HandleAwareFieldSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AccessTokenRequest {
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

	@JsonSerialize(contentUsing =  HandleAwareFieldSerializer.class)
	@JsonDeserialize(contentUsing = HandleAwareFieldDeserializer.class)
	private List<HandleAwareField<RequestedResource>> access = new ArrayList<>();
	private String label;
	private Set<TokenFlag> flags;

	@JsonIgnore
	public boolean isBearer() {
		return flags != null && flags.contains(TokenFlag.BEARER);
	}

	public static AccessTokenRequest ofReferences(String... references) {
		return new AccessTokenRequest()
			.setAccess(Arrays.stream(references)
				.map(r -> HandleAwareField.<RequestedResource>of(r))
				.collect(Collectors.toList()));
	}
}
