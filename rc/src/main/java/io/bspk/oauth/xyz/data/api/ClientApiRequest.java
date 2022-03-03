package io.bspk.oauth.xyz.data.api;

import java.net.URI;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nimbusds.jose.jwk.JWK;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ClientApiRequest {

	private URI grantEndpoint;
	private JWK privateKey;
	private TransactionRequest request;

}
