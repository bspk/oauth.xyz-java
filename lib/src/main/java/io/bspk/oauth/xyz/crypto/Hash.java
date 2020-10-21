package io.bspk.oauth.xyz.crypto;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Function;

import org.bouncycastle.jcajce.provider.digest.SHA1;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA256.Digest;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jcajce.provider.digest.SHA384;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Joiner;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.util.Base64URL;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author jricher
 *
 */
public abstract class Hash {

	private static final Logger log = LoggerFactory.getLogger(Hash.class);

	@AllArgsConstructor
	public enum Method {
		SHA3("sha3", Hash::SHA3_512_encode),
		SHA2("sha2", Hash::SHA2_512_encode)
		;

		@Getter private String name;
		@Getter private Function<String, String> function;

		@JsonCreator
		public static Method fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}

	}

	public static String SHA3_512_encode(String input) {
		MessageDigest digest = new SHA3.Digest512();
		byte[] output = digest.digest(input.getBytes());

		byte[] encoded = Base64.getUrlEncoder().withoutPadding().encode(output);

		return new String(encoded);

	}

	public static String SHA2_512_encode(String input) {
		MessageDigest digest = new SHA512.Digest();
		byte[] output = digest.digest(input.getBytes());

		byte[] encoded = Base64.getUrlEncoder().withoutPadding().encode(output);

		return new String(encoded);

	}

	public static String CalculateInteractHash(String clientNonce, String serverNonce, String interact, Method method) {
		return method.getFunction().apply(
			Joiner.on('\n')
			.join(clientNonce,
				serverNonce,
				interact));
	}

	public static String SHA256_encode(String input) {
		if (input == null || input.isEmpty()) {
			return null;
		}

		MessageDigest digest = new SHA256.Digest();
		byte[] output = digest.digest(input.getBytes());

		byte[] encoded = Base64.getUrlEncoder().withoutPadding().encode(output);

		return new String(encoded);
	}

	public static String SHA1_digest(byte[] input) {
		if (input == null || input.length == 0) {
			return null;
		}

		MessageDigest digest = new SHA1.Digest();
		byte[] output = digest.digest(input);

		byte[] encoded = Base64.getEncoder().encode(output);

		return new String(encoded);
	}

	// This is the OIDC "at_hash" algorithm, used with JOSE-based signing mechanisms
	public static Base64URL getAtHash(Algorithm signingAlg, byte[] bytes) {
	
		//Switch based on the given signing algorithm - use SHA-xxx with the same 'xxx' bitnumber
		//as the JWSAlgorithm to hash the token.
	
		MessageDigest hasher = null;
		if (signingAlg.equals(JWSAlgorithm.HS256) || signingAlg.equals(JWSAlgorithm.ES256) || signingAlg.equals(JWSAlgorithm.RS256) || signingAlg.equals(JWSAlgorithm.PS256)) {
			hasher = new SHA256.Digest();
		}
	
		else if (signingAlg.equals(JWSAlgorithm.ES384) || signingAlg.equals(JWSAlgorithm.HS384) || signingAlg.equals(JWSAlgorithm.RS384) || signingAlg.equals(JWSAlgorithm.PS384)) {
			hasher = new SHA384.Digest();
		}
	
		else if (signingAlg.equals(JWSAlgorithm.ES512) || signingAlg.equals(JWSAlgorithm.HS512) || signingAlg.equals(JWSAlgorithm.RS512) || signingAlg.equals(JWSAlgorithm.PS512)) {
			hasher = new SHA512.Digest();
		}
	
		if (hasher != null) {
			hasher.reset();
			hasher.update(bytes);
	
			byte[] hashBytes = hasher.digest();
			byte[] hashBytesLeftHalf = Arrays.copyOf(hashBytes, hashBytes.length / 2);
			Base64URL encodedHash = Base64URL.encode(hashBytesLeftHalf);
	
			return encodedHash;
		}
	
		return null;
	}
}
