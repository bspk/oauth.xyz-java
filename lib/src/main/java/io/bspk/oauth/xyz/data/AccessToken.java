package io.bspk.oauth.xyz.data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.data.api.HandleAwareField;
import io.bspk.oauth.xyz.data.api.RequestedResource;
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
public class AccessToken {

	private String value;
	private Key key;
	private boolean bound;
	private boolean clientBound;
	private String manage;
	@JsonSerialize(contentUsing =  HandleAwareFieldSerializer.class)
	@JsonDeserialize(contentUsing = HandleAwareFieldDeserializer.class)
	private List<HandleAwareField<RequestedResource>> accessRequest;
	private Instant expiration;
	private String label;

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
		return create().setKey(key)
			.setBound(true)
			.setClientBound(true);
	}

}
