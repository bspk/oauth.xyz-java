package io.bspk.oauth.xyz.data;

import java.net.URI;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;

import io.bspk.oauth.xyz.data.api.HandleAwareField;
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

	@JsonIgnore
	@Transient
	private final Logger log = LoggerFactory.getLogger(this.getClass());


	private Proof proof;
	@JsonSerialize(using = JWKSerializer.class)
	@JsonDeserialize(using = JWKDeserializer.class)
	private JWK jwk;
	private X509Certificate cert;
	private URI did;


	public static Key of(HandleAwareField<KeyRequest> request) {

		if (request == null) {
			return null;
		}

		if (request.isHandled()) {
			// TODO: dereference keys using a service
			return null;
		}

		return new Key()
			.setProof(request.asValue().getProof())
			.setCert(request.asValue().getCert())
			.setDid(request.asValue().getDid())
			.setJwk(request.asValue().getJwk());
	}

	public String getHash() {
		if (getJwk() != null) {
			try {
				return getJwk().computeThumbprint().toString();
			} catch (JOSEException e) {
				log.error("Couldn't compute thumbprint.", e);
				return null;
			}
		} else if (getCert() != null) {
			// TODO: calculate cert thumbprint
			return null;
		} else {
			// couldn't find key to thumbprint
			return null;
		}
	}


}

