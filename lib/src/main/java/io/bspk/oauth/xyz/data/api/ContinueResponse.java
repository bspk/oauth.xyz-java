package io.bspk.oauth.xyz.data.api;

import java.net.URI;

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
public class ContinueResponse {

	private Integer wait;
	private URI uri;
	private AccessTokenResponse accessToken;

}
