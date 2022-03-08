package io.bspk.oauth.xyz.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
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
public class HttpSign {

	private static final Logger log = LoggerFactory.getLogger(HttpSign.class);


	private HttpSigAlgorithm alg;
	private JWK signingKey; // TODO: other key formats?

	public byte[] sign(byte[] base) {
		try {
			if (alg.equals(HttpSigAlgorithm.RSAPSS)) {
				if (signingKey.getKeyType().equals(KeyType.RSA)) {
					PrivateKey privateKey = signingKey.toRSAKey().toPrivateKey();

					Signature signer = Signature.getInstance("RSASSA-PSS");
					signer.setParameter(
						new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1));

					MessageDigest sha = new SHA512.Digest();
					byte[] hash = sha.digest(base);

					signer.initSign(privateKey);
					signer.update(hash);
					byte[] s = signer.sign();
					return s;
				}
			} else if (alg.equals(HttpSigAlgorithm.RSA15)) {
				if (signingKey.getKeyType().equals(KeyType.RSA)) {
					PrivateKey privateKey = signingKey.toRSAKey().toPrivateKey();

					Signature signer = Signature.getInstance("SHA256withRSA");

					MessageDigest sha = new SHA256.Digest();
					byte[] hash = sha.digest(base);

					signer.initSign(privateKey);
					signer.update(hash);
					byte[] s = signer.sign();
					return s;
				}
			} else if (alg.equals(HttpSigAlgorithm.HMAC)) {
				if (signingKey.getKeyType().equals(KeyType.OCT)) {
					SecretKey secretKey = signingKey.toOctetSequenceKey().toSecretKey();

					Mac mac = Mac.getInstance("HmacSHA256");
					mac.init(secretKey);
					mac.update(base);
					byte[] s = mac.doFinal();
					return s;
				}
			} else if (alg.equals(HttpSigAlgorithm.ECDSA)) {
				if (signingKey.getKeyType().equals(KeyType.EC)) {
					PrivateKey privateKey = signingKey.toECKey().toPrivateKey();

					Signature signer = Signature.getInstance("SHA256withECDSA");

					MessageDigest sha = new SHA256.Digest();
					byte[] hash = sha.digest(base);

					signer.initSign(privateKey);
					signer.update(hash);
					byte[] rs = signer.sign();
					byte[] s = ECDSA.transcodeSignatureToConcat(rs, 64);
					return s;
				}
			} else if (alg.equals(HttpSigAlgorithm.JOSE)) {
				// do a JOSE signature based on what's in the key
				JWSSigner signer = new DefaultJWSSignerFactory().createJWSSigner(signingKey);

				// create a fake header
				JWSAlgorithm alg = new JWSAlgorithm(signingKey.getAlgorithm().getName());
				JWSHeader header = new JWSHeader.Builder(alg).build();

				Base64URL s = signer.sign(header, base);
				return s.decode();
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | SignatureException | JOSEException e) {
			log.warn("Could not sign input", e);
		}
		return null;
	}

}
