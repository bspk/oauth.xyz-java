package io.bspk.oauth.xyz.crypto;

import java.security.MessageDigest;

import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.springframework.util.Base64Utils;

/**
 * @author jricher
 *
 */
public abstract class Hash {

	public static String SHA3_512_encode(String input) {

		MessageDigest digest = new SHA3.Digest512();
		byte[] output = digest.digest(input.getBytes());
		byte[] encoded = Base64Utils.encodeUrlSafe(output);
		return new String(encoded);

	}
}
