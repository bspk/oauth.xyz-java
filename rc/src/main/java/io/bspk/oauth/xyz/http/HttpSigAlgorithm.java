package io.bspk.oauth.xyz.http;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Signature algorithms.
 * @author jricher
 *
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpSigAlgorithm {

	public static final HttpSigAlgorithm RSAPSS = new HttpSigAlgorithm("rsa-pss-sha512");
	public static final HttpSigAlgorithm RSA15 = new HttpSigAlgorithm("rsa-v1_5-sha256");
	public static final HttpSigAlgorithm HMAC = new HttpSigAlgorithm("hmac-sha256");
	public static final HttpSigAlgorithm ECDSA = new HttpSigAlgorithm("ecdsa-p256-sha256");
	public static final HttpSigAlgorithm ED25519 = new HttpSigAlgorithm("ed25519");
	public static final HttpSigAlgorithm JOSE = new HttpSigAlgorithm(null);

	@JsonValue
	private String explicitAlg;

}
