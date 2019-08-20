package io.bspk.oauth.xyz.crypto;

import java.security.MessageDigest;
import java.util.Base64;

import org.bouncycastle.jcajce.provider.digest.SHA3;

import com.google.common.base.Joiner;

/**
 * @author jricher
 *
 */
public abstract class Hash {

	public static String SHA3_512_encode(String input) {

		MessageDigest digest = new SHA3.Digest512();
		byte[] output = digest.digest(input.getBytes());

		byte[] encoded = Base64.getUrlEncoder().withoutPadding().encode(output);

		return new String(encoded);

	}

	public static String CalculateInteractHash(String clientNonce, String serverNonce, String interact) {
		return Hash.SHA3_512_encode(
			Joiner.on('\n')
			.join(clientNonce,
				serverNonce,
				interact));
	}

}
