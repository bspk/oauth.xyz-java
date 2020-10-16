package io.bspk.oauth.xyz.data.api;

import java.net.URI;
import java.security.cert.X509Certificate;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nimbusds.jose.jwk.JWK;

import io.bspk.oauth.xyz.data.Keys.Proof;
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

}
