package io.bspk.oauth.xyz.data.api;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Optional;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nimbusds.jose.jwk.JWK;

import io.bspk.oauth.xyz.data.Key;
import io.bspk.oauth.xyz.data.Key.Proof;
import io.bspk.oauth.xyz.json.JWKDeserializer;
import io.bspk.oauth.xyz.json.JWKSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class KeyRequest extends HandleReplaceable<KeyRequest> {

	private Proof proof;
	@JsonSerialize(using = JWKSerializer.class)
	@JsonDeserialize(using = JWKDeserializer.class)
	private JWK jwk;
	private X509Certificate cert;
	private URI did;

	public static KeyRequest of (Key key) {
		return new KeyRequest()
			.setJwk(Optional.ofNullable(key.getJwk()).map(JWK::toPublicJWK).orElse(null)) // make sure we only ever pass a public key in the request
			.setCert(key.getCert())
			.setDid(key.getDid())
			.setProof(key.getProof());
	}
}
