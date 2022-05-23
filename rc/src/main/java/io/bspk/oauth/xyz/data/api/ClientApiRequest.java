package io.bspk.oauth.xyz.data.api;

import java.net.URI;
import java.util.Set;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nimbusds.jose.jwk.JWK;

import io.bspk.httpsig.HttpSigAlgorithm;
import io.bspk.oauth.xyz.data.Interact.InteractStart;
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
public class ClientApiRequest {

	private URI grantEndpoint;
	private JWK privateKey;
	private Proof proof;
	private DisplayRequest display;
	private AccessTokenRequest accessToken;
	private Set<InteractStart> interactStart;
	private boolean interactFinish;
	private UserRequest user;
	private SubjectRequest subject;
	private HttpSigAlgorithm httpSigAlgorithm;
	private String digest;

}
