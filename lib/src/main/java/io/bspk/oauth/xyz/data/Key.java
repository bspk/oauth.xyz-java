package io.bspk.oauth.xyz.data;

import java.net.URI;
import java.security.cert.X509Certificate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nimbusds.jose.jwk.JWK;

import io.bspk.oauth.xyz.data.api.KeyRequest;
import io.bspk.oauth.xyz.json.JWKDeserializer;
import io.bspk.oauth.xyz.json.JWKSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Key {
	public enum Proof {
		JWSD,
		MTLS,
		HTTPSIG,
		DPOP,
		OAUTHPOP,
		JWS
		;

		@JsonCreator
		public static Proof fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}

	}

	private Proof proof;
	@JsonSerialize(using = JWKSerializer.class)
	@JsonDeserialize(using = JWKDeserializer.class)
	private JWK jwk;
	private X509Certificate cert;
	private URI did;


	public static Key of(KeyRequest request) {
		return new Key()
			.setProof(request.getProof())
			.setCert(request.getCert())
			.setDid(request.getDid())
			.setJwk(request.getJwk());
	}

}

