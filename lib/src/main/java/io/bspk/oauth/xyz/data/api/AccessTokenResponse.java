package io.bspk.oauth.xyz.data.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Maps;

import io.bspk.oauth.xyz.data.AccessToken;
import io.bspk.oauth.xyz.data.BoundKey;
import io.bspk.oauth.xyz.json.BoundKeySerializer;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AccessTokenResponse {

	private String value;
	@JsonSerialize(using=BoundKeySerializer.class)
	private BoundKey key;
	private String manage;
	private SingleTokenResourceRequest resources;
	private Long expiresIn;

	public static AccessTokenResponse of(AccessToken t) {
		if (t != null) {
			return new AccessTokenResponse()
				.setValue(t.getValue())
				.setKey(t.getKey())
				.setManage(t.getManage())
				.setResources(t.getResourceRequest())
				.setExpiresIn(t.getExpiration() != null ?
					Duration.between(Instant.now(), t.getExpiration()).toSeconds()
					: null);
		} else {
			return null;
		}
	}

	public static Map<String, AccessTokenResponse> of(Map<String, AccessToken> tokens) {
		if (tokens != null) {
			return Maps.transformValues(tokens,
					v -> AccessTokenResponse.of(v));
		} else {
			return null;
		}
	}

}
