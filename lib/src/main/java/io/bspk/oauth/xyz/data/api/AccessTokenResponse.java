package io.bspk.oauth.xyz.data.api;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.data.AccessToken;
import io.bspk.oauth.xyz.data.Key;
import io.bspk.oauth.xyz.data.api.AccessTokenRequest.TokenFlag;
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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AccessTokenResponse {
	private String value;
	private Key key;
	private String manage;
	@JsonSerialize(contentUsing =  HandleAwareFieldSerializer.class)
	@JsonDeserialize(contentUsing = HandleAwareFieldDeserializer.class)
	private List<HandleAwareField<RequestedResource>> access;
	private Long expiresIn;
	private String label;
	private Set<TokenFlag> flags;

	public static AccessTokenResponse of(AccessToken t) {
		if (t != null) {

			Set<TokenFlag> tf = new HashSet<>();
			if (!t.isBound()) {
				tf.add(TokenFlag.BEARER);
			}
			// TODO: split and durable tokens

			return new AccessTokenResponse()
				.setValue(t.getValue())
				.setFlags(tf)
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
