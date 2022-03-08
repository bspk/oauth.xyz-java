package io.bspk.oauth.xyz.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
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

	/**
	 * @param item
	 * @return
	 */
	@JsonCreator
	public static HttpSigAlgorithm of(String alg) {
		if (alg == null) {
			return null;
		} else if (alg.equalsIgnoreCase("jose")) {
			return JOSE;
		} else {
			return new HttpSigAlgorithm(alg);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		HttpSigAlgorithm other = (HttpSigAlgorithm) obj;
		if (explicitAlg == null) {
			if (other.explicitAlg != null) {
				return false;
			}
		} else if (!explicitAlg.equals(other.explicitAlg)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((explicitAlg == null) ? 0 : explicitAlg.hashCode());
		return result;
	}

}
