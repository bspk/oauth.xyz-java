package io.bspk.oauth.xyz.data.api;

import java.net.URI;
import java.security.cert.X509Certificate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
public class KeyRequest extends HandleReplaceable<KeyRequest> {

	public enum Type {
		JWSD,
		MTLS,
		DID
		;

		@JsonCreator
		public static Type fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}

	}

	private Type type;
	private JWK jwk;
	private X509Certificate cert;
	private URI did;

}
