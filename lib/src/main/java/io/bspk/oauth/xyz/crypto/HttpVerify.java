package io.bspk.oauth.xyz.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
public class HttpVerify {

	private static final Logger log = LoggerFactory.getLogger(HttpVerify.class);

	private HttpSigAlgorithm alg;
	private JWK verifyKey; // TODO: other key formats?

	public boolean verify(byte[] base, byte[] signature) {
		try {
			if (alg.equals(HttpSigAlgorithm.RSAPSS)) {
				if (verifyKey.getKeyType().equals(KeyType.RSA)) {
					PublicKey publicKey = verifyKey.toRSAKey().toPublicKey();

					Signature verifier = Signature.getInstance("RSASSA-PSS");
					verifier.setParameter(
						new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1));

					MessageDigest sha = new SHA512.Digest();
					byte[] hash = sha.digest(base);

					verifier.initVerify(publicKey);
					verifier.update(hash);
					return verifier.verify(signature);
				}
			} else if (alg.equals(HttpSigAlgorithm.RSA15)) {
				if (verifyKey.getKeyType().equals(KeyType.RSA)) {
					PublicKey publicKey = verifyKey.toRSAKey().toPublicKey();

					Signature verifier = Signature.getInstance("SHA256withRSA");

					MessageDigest sha = new SHA256.Digest();
					byte[] hash = sha.digest(base);

					verifier.initVerify(publicKey);
					verifier.update(hash);
					return verifier.verify(signature);
				}
			} else if (alg.equals(HttpSigAlgorithm.HMAC)) {
				if (verifyKey.getKeyType().equals(KeyType.OCT)) {
					SecretKey secretKey = verifyKey.toOctetSequenceKey().toSecretKey();

					Mac mac = Mac.getInstance("HmacSHA256");
					mac.init(secretKey);
					mac.update(base);
					byte[] s = mac.doFinal();

					return Arrays.equals(s, signature);
				}
			} else if (alg.equals(HttpSigAlgorithm.ECDSA)) {
				if (verifyKey.getKeyType().equals(KeyType.EC)) {
					PublicKey publicKey = verifyKey.toECKey().toPublicKey();

					Signature verifier = Signature.getInstance("SHA256withECDSA");

					MessageDigest sha = new SHA256.Digest();
					byte[] hash = sha.digest(base);

					verifier.initVerify(publicKey);
					verifier.update(hash);

					byte[] s = ECDSA.transcodeSignatureToDER(signature);
					return verifier.verify(s);
				}
			} else if (alg.equals(HttpSigAlgorithm.JOSE)) {
				// create a fake header
				JWSAlgorithm alg = new JWSAlgorithm(verifyKey.getAlgorithm().getName());
				JWSHeader header = new JWSHeader.Builder(alg).build();

				Key key = null;
				if (verifyKey instanceof OctetSequenceKey) {
					key = verifyKey.toOctetSequenceKey().toSecretKey();
				} else if (verifyKey instanceof RSAKey) {
					key = verifyKey.toRSAKey().toPublicKey();
				} else if (verifyKey instanceof ECKey) {
					key = verifyKey.toECKey().toPublicKey();
				} else {
					log.warn("Unknown key type: " + verifyKey);
					return false;
				}


				// do a JOSE signature based on what's in the key
				JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(header, key);


				return verifier.verify(header, base, Base64URL.encode(signature));
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | SignatureException | JOSEException e) {
			log.warn("Could not sign input", e);
		}
		return false;
	}

}
