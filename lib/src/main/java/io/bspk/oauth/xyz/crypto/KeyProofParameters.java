package io.bspk.oauth.xyz.crypto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nimbusds.jose.jwk.JWK;

import io.bspk.httpsig.HttpSigAlgorithm;
import io.bspk.oauth.xyz.data.Key.Proof;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class KeyProofParameters {

	private JWK signingKey;
	private Proof proof;
	private String digestAlgorithm;
	private HttpSigAlgorithm httpSigAlgorithm;

}
