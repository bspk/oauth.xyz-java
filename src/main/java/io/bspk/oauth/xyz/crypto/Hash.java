package io.bspk.oauth.xyz.crypto;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.function.Function;

import org.bouncycastle.jcajce.provider.digest.SHA1;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

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
}
