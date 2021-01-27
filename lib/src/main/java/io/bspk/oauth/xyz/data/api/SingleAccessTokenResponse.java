package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
public class SingleAccessTokenResponse implements AccessTokenResponse {

	private String value;
	@JsonSerialize(using=BoundKeySerializer.class)
	private BoundKey key;
	private String manage;
	private SingleTokenResourceRequest resources;
	private Long expiresIn;
	private String label;

	@Override
	public boolean isMultiple() {
		return false;
	}

}
