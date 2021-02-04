package io.bspk.oauth.xyz.data.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.data.AccessToken;
import io.bspk.oauth.xyz.data.Key;
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
public class AccessTokenResponse {
	private String value;
	private Key key;
	private boolean bound = true;
	private String manage;
	@JsonSerialize(contentUsing =  HandleAwareFieldSerializer.class)
	@JsonDeserialize(contentUsing = HandleAwareFieldDeserializer.class)
	private List<HandleAwareField<RequestedResource>> access;
	private Long expiresIn;
	private String label;

	public static AccessTokenResponse of(AccessToken t) {
		if (t != null) {
			return new AccessTokenResponse()
				.setValue(t.getValue())
				.setBound(t.isBound())
				.setKey(!t.isClientBound() ? t.getKey() : null)
				.setManage(t.getManage())
				.setAccess(t.getAccessRequest())
				.setLabel(t.getLabel())
				.setExpiresIn(t.getExpiration() != null ?
					Duration.between(Instant.now(), t.getExpiration()).toSeconds()
					: null);
		} else {
			return null;
		}
	}
}
