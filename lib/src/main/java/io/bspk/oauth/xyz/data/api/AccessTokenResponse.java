package io.bspk.oauth.xyz.data.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.bspk.oauth.xyz.data.AccessToken;

/**
 * @author jricher
 *
 */
public interface AccessTokenResponse {

	public boolean isMultiple();

	public static AccessTokenResponse oneOf(AccessToken t, Map<String, AccessToken> tokens) {
		if (t == null) {
			if (tokens == null) {
				return null;
			} else {
				// multi token
				return of(tokens);
			}
		} else {
			if (tokens == null) {
				// single token
				return of(t);
			} else {
				// error, can't have both single and multi
				throw new IllegalArgumentException("Found both single and multi access tokens issued to same transaction");
			}
		}
	}

	public static SingleAccessTokenResponse of(AccessToken t) {
		if (t != null) {
			return new SingleAccessTokenResponse()
				.setValue(t.getValue())
				.setKey(t.getKey())
				.setManage(t.getManage())
				.setResources(t.getResourceRequest())
				.setLabel(t.getLabel())
				.setExpiresIn(t.getExpiration() != null ?
					Duration.between(Instant.now(), t.getExpiration()).toSeconds()
					: null);
		} else {
			return null;
		}
	}

	public static MultiAccessTokenResponse of(List<SingleAccessTokenResponse> responses) {
		if (responses == null) {
			return null;
		} else {
			return new MultiAccessTokenResponse()
				.setResponses(responses);
		}
	}

	public static MultiAccessTokenResponse of(Map<String, AccessToken> tokens) {
		if (tokens != null) {
			MultiAccessTokenResponse m = new MultiAccessTokenResponse();

			tokens.entrySet().forEach(e -> {
				m.getResponses().add(AccessTokenResponse.of(e.getValue())
					.setLabel(e.getKey())); // override label if necessary
			});

			return m;
		} else {
			return null;
		}
	}

}
