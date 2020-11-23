package io.bspk.oauth.xyz.data;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.api.SingleTokenResourceRequest;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AccessToken {

	private String value;
	private BoundKey key;
	private String manage;
	private SingleTokenResourceRequest resourceRequest;
	private Instant expiration;

	/**
	 * Create a handle with a random value and no expiration
	 */
	public static AccessToken create() {
		return new AccessToken().setValue(RandomStringUtils.randomAlphanumeric(64));
	}

	/**
	 * Create a handle with a random value and an expiration based on the lifetime
	 */
	public static AccessToken create(Duration lifetime) {
		return create().setExpiration(Instant.now().plus(lifetime));
	}

	public static AccessToken createClientBound(Key key) {
		return create().setKey(new BoundKey().setKey(key).setClientKey(true));
	}

}
