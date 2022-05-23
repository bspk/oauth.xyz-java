package io.bspk.oauth.xyz.crypto;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.greenbytes.http.sfv.ByteSequenceItem;
import org.greenbytes.http.sfv.Dictionary;
import org.greenbytes.http.sfv.ListElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.bspk.httpsig.ComponentProvider;
import io.bspk.httpsig.HttpSigAlgorithm;
import io.bspk.httpsig.HttpVerify;
import io.bspk.httpsig.SignatureBaseBuilder;
import io.bspk.httpsig.SignatureParameters;
import io.bspk.httpsig.servlet.HttpServletRequestProvider;
import io.bspk.oauth.xyz.http.DigestWrappingFilter;
import io.bspk.oauth.xyz.http.JoseUnwrappingFilter;

/**
 * @author jricher
 *
 */
public class SignatureVerifier {

	private static final Logger log = LoggerFactory.getLogger(SignatureVerifier.class);

	public static void checkAttachedJws(HttpServletRequest request, JWK clientKey) {
		checkAttachedJws(request, clientKey, null);
	}

	public static void checkAttachedJws(HttpServletRequest request, JWK clientKey, String accessToken) {
		JOSEObject jose = (JOSEObject) request.getAttribute(JoseUnwrappingFilter.BODY_JOSE);

		if (jose == null) {
			throw new RuntimeException("No JOSE object detected");
		}

		try {
			if (jose instanceof JWSObject) {
				JWSObject jws = (JWSObject)jose;

				verifyJWS(jws, request, clientKey, accessToken);

				JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jws.getHeader(), extractKeyForVerify(clientKey));

				if (!jws.verify(verifier)) {
					throw new RuntimeException("Unable to verify JWS");
				}

				// note: we know the payload matches the body because of the JoseUnwrappingFilter extracted it

			}
		} catch (JOSEException e) {
			throw new RuntimeException("Bad JWS", e);
		}
	}

	public static void checkHttpMessageSignature(Dictionary signature, Dictionary signatureInput, HttpServletRequest request, JWK clientKey) {
		if (signature == null) {
			throw new RuntimeException("Missing signature header");
		}
		if (signatureInput == null) {
			throw new RuntimeException("Missing signature input header");
		}


		// all signatures have to pass, check all of them

		for (String sigId : signatureInput.get().keySet()) {

			if (!signature.get().containsKey(sigId)) {
				throw new RuntimeException("Didn't find signature for id " + sigId);
			}

			SignatureParameters sigParams = SignatureParameters.fromDictionaryEntry(signatureInput, sigId);
			ByteSequenceItem sigValue = (ByteSequenceItem) signature.get().get(sigId);

			// ensure proper coverage

			byte[] savedBody = (byte[]) request.getAttribute(DigestWrappingFilter.BODY_BYTES);

			if (savedBody != null && savedBody.length > 0
				&& !(sigParams.containsComponentIdentifier("content-digest"))) {
				// missing the content-digest header
				throw new RuntimeException("Missing content-digest header on non-empty body");
			}

			if (request.getHeader(HttpHeaders.AUTHORIZATION) != null
				&& !(sigParams.containsComponentIdentifier("authorization"))) {
				throw new RuntimeException("Missing authorizaiton header on a token-protected call");
			}

			if (sigParams.containsComponentIdentifier("@method")
				&& (sigParams.containsComponentIdentifier("@target-uri"))) { // should we allow the URI in parts?

				ComponentProvider ctx = new HttpServletRequestProvider(request);

				// collect the base string
				SignatureBaseBuilder baseBuilder = new SignatureBaseBuilder(sigParams, ctx);

				byte[] baseBytes = baseBuilder.createSignatureBase();

				HttpSigAlgorithm alg = sigParams.getAlg();
				if (alg == null) {
					alg = HttpSigAlgorithm.JOSE; // FIXME: this needs to be signaled better
				}
				HttpVerify verify = new HttpVerify(alg, clientKey);

				ByteBuffer bb = sigValue.get();
				byte[] sigBytes = new byte[bb.remaining()];
				bb.get(sigBytes);

				if (!verify.verify(baseBytes, sigBytes)) {
					throw new RuntimeException("Bad Signature, no biscuit");
				}
			} else {
				throw new RuntimeException("Missing required covered component, found: " + sigParams.getComponentIdentifiers());
			}

			log.info("++ Verified HTTP Message signature " + sigId);
		}
	}

	public static void checkCavageSignature(String signatureHeaderPayload, HttpServletRequest request, JWK clientKey) {
		if (!Strings.isNullOrEmpty(signatureHeaderPayload)) {

			try {
				Map<String, String> signatureParts = Stream.of(signatureHeaderPayload.split(","))
					.map((s) -> {
						String[] parts = s.split("=", 2);
						String noQuotes = parts[1].replaceAll("^\"([^\"]*)\"$", "$1");
						return Map.entry(parts[0], noQuotes);
					})
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
					;

				// collect the base string
				Map<String, String> signatureBlock = new LinkedHashMap<>();

				List<String> headersToSign = Stream.of(signatureParts.get("headers").split(" ")).collect(Collectors.toList());

				headersToSign.forEach((h) -> {
					if (h.equals("(request-target)")) {
						String requestTarget = request.getMethod().toLowerCase() + " " + request.getRequestURI();
						signatureBlock.put(h.toLowerCase(), requestTarget);
					} else if (request.getHeader(h) != null) {
						signatureBlock.put(h.toLowerCase(), request.getHeader(h));
					}
				});

				String input = signatureBlock.entrySet().stream()
					.map(e -> e.getKey().strip().toLowerCase() + ": " + e.getValue().strip())
					.collect(Collectors.joining("\n"));

				RSAKey rsaKey = (RSAKey)clientKey;

				Signature signature = Signature.getInstance("SHA256withRSA");

				byte[] signatureBytes = Base64.getDecoder().decode(signatureParts.get("signature"));

				signature.initVerify(rsaKey.toPublicKey());
				signature.update(input.getBytes("UTF-8"));

				if (!signature.verify(signatureBytes)) {
					throw new RuntimeException("Bad Signature, no biscuit");
				}

			} catch (NoSuchAlgorithmException | InvalidKeyException | JOSEException | SignatureException | UnsupportedEncodingException e) {
				throw new RuntimeException("Bad crypto, no biscuit", e);
			}
		}

		log.info("++ Verified Cavage signature");

	}

	public static void checkDetachedJws(String jwsd, HttpServletRequest request, JWK jwk, String accessToken) {

		if (Strings.isNullOrEmpty(jwsd)) {
			throw new RuntimeException("Missing JWS value");
		}

		try {

			Payload payload = null;

			byte[] body = (byte[])request.getAttribute(DigestWrappingFilter.BODY_BYTES);
			if (body == null || body.length == 0) {
				payload = new Payload(new byte[0]);
			} else {
				payload = new Payload(Hash.SHA256_encode_url(body));
			}

			JWSObject jwsObject = JWSObject.parse(jwsd);

			// ensure payloads match
			if (!payload.toBase64URL().equals(jwsObject.getPayload().toBase64URL())) {
				throw new RuntimeException("Body hash did not match. Expected " + payload.toBase64URL().toString() + " received " + jwsObject.getPayload().toBase64URL().toString());
			}

			verifyJWS(jwsObject, request, jwk, accessToken);

		} catch (ParseException | JOSEException e) {
			throw new RuntimeException("Bad JWS", e);
		}

		log.info("++ Verified Detached JWS signature");

	}

	private static void verifyJWS(JWSObject jwsObject, HttpServletRequest request, JWK jwk, String accessToken) throws JOSEException {
		JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jwsObject.getHeader(), extractKeyForVerify(jwk));

		if (!jwsObject.verify(verifier)) {
			throw new RuntimeException("Unable to verify JWS");
		}

		// check the URI and method
		JWSHeader header = jwsObject.getHeader();

		if (header.getCustomParam("htm") == null || !((String)header.getCustomParam("htm")).equals(request.getMethod())) {
			throw new RuntimeException("Couldn't verify method");
		}

		if (header.getCustomParam("uri") == null) {
			throw new RuntimeException("Couldn't get uri");
		} else {
			StringBuffer url = request.getRequestURL();
			if (request.getQueryString() != null) {
				url.append('?').append(request.getQueryString());
			}
			if (!header.getCustomParam("uri").equals(url.toString())) {
				throw new RuntimeException("Couldn't verify uri");
			}
		}

		if (!Strings.isNullOrEmpty(accessToken)) {
			if (header.getCustomParam("ath") == null) {
				throw new RuntimeException("Couldn't get access token hash");
			} else {
				Base64URL expected = Hash.SHA256_encode_url(accessToken.getBytes());
				Base64URL actual = Base64URL.from(header.getCustomParam("ath").toString());

				if (!expected.equals(actual)) {
					throw new RuntimeException("Access token hash does not match: " + expected + " / " + actual);
				}
			}
		}
	}

	public static void checkDpop(String dpop, HttpServletRequest request, JWK clientKey, String accessToken) {
		try {

			SignedJWT jwt = SignedJWT.parse(dpop);

			JWK jwtKey = jwt.getHeader().getJWK();

			if (!jwtKey.equals(clientKey)) {
				throw new RuntimeException("Client key did not match DPoP key");
			}

			JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jwt.getHeader(), extractKeyForVerify(clientKey));

			if (!jwt.verify(verifier)) {
				throw new RuntimeException("Unable to verify DPOP Signature");
			}

			JWTClaimsSet claims = jwt.getJWTClaimsSet();

			if (claims.getClaim("htm") == null || !claims.getClaim("htm").equals(request.getMethod())) {
				throw new RuntimeException("Couldn't verify method");
			}

			if (claims.getClaim("htu") == null) {
				throw new RuntimeException("Couldn't get uri");
			} else {
				StringBuffer url = request.getRequestURL();
				if (request.getQueryString() != null) {
					url.append('?').append(request.getQueryString());
				}
				if (!claims.getClaim("htu").equals(url.toString())) {
					throw new RuntimeException("Couldn't verify uri");
				}
			}

			if (claims.getClaim("digest") != null) {
				ensureDigest(claims.getClaim("digest").toString(), request);
			}

			if (!Strings.isNullOrEmpty(accessToken)) {
				if (claims.getClaim("ath") == null) {
					throw new RuntimeException("Couldn't get access token hash");
				} else {
					Base64URL expected = Hash.SHA256_encode_url(accessToken.getBytes());
					Base64URL actual = Base64URL.from(claims.getStringClaim("ath"));

					if (!expected.equals(actual)) {
						throw new RuntimeException("Access token hash does not match");
					}
				}
			}

		} catch (ParseException | JOSEException e) {
			throw new RuntimeException("Bad DPOP Signature", e);
		}

		log.info("++ Verified DPoP signature");

	}

	public static void checkHeaderHash(HttpServletRequest request, List<String> headersUsed, String hashUsed) {
		if (headersUsed != null && !Strings.isNullOrEmpty(hashUsed)) {
			List<String> hashBase = new ArrayList<>();

			headersUsed.forEach((h) -> {
				String first = request.getHeader(h);
				hashBase.add(h.toLowerCase()
					+ ": "
					+ first);
			});

			String hash = Hash.SHA256_encode(Joiner.on("\n").join(hashBase));

			if (!hash.equals(hashUsed)) {
				throw new RuntimeException("Couldn't validate header hash");
			}
			log.info("++ Validated header hash");
		}
	}

	public static void checkOAuthPop(String oauthPop, HttpServletRequest req, JWK jwk, String accessToken) {
		try {
			SignedJWT jwt = SignedJWT.parse(oauthPop);

			JWTClaimsSet claims = jwt.getJWTClaimsSet();

			if (claims.getClaim("q") != null) {
				List<Object> q = (List<Object>) claims.getClaim("q");
				checkQueryHash(req, (List<String>)q.get(0), (String)q.get(1));
			}

			if (claims.getClaim("h") != null) {
				List<Object> h = (List<Object>) claims.getClaim("h");
				checkHeaderHash(req, (List<String>)h.get(0), (String)h.get(1));
			}

			if (claims.getClaim("m") != null && !req.getMethod().equals(claims.getClaim("m"))) {
				throw new RuntimeException("Couldn't validate method.");
			}

			if (claims.getClaim("u") != null && !req.getServerName().equals(claims.getClaim("u"))) {
				throw new RuntimeException("Couldn't validate host.");
			}

			if (claims.getClaim("p") != null && !req.getRequestURI().equals(claims.getClaim("p"))) {
				throw new RuntimeException("Couldn't validate path.");
			}

			if (claims.getClaim("ts") != null) {
				Instant ts = Instant.ofEpochSecond(claims.getLongClaim("ts"));
				if (!Instant.now().minusSeconds(10).isBefore(ts)
					|| !Instant.now().plusSeconds(10).isAfter(ts)) {
					throw new RuntimeException("Timestamp outside of acceptable window.");
				}
			}

			if (!Strings.isNullOrEmpty(accessToken)) {
				if (claims.getClaim("at") == null) {
					throw new RuntimeException("No access token");
				} else {
					if (!claims.getClaim(accessToken).equals(accessToken)) {
						throw new RuntimeException("Access token didn't match. (And that's really weird considering how we got here.");
					}
				}
			}
		} catch (ParseException e) {
			throw new RuntimeException("Couldn't parse pop header", e);
		}

		log.info("++ Verified OAuth PoP");
	}

	private static java.security.Key extractKeyForVerify(JWK jwk) throws JOSEException {
		if (jwk instanceof OctetSequenceKey) {
			return jwk.toOctetSequenceKey().toSecretKey();
		} else if (jwk instanceof RSAKey) {
			return jwk.toRSAKey().toPublicKey();
		} else if (jwk instanceof ECKey) {
			return jwk.toECKey().toECPublicKey();
		} else if (jwk instanceof OctetKeyPair) {
			return jwk.toOctetKeyPair().toPublicKey();
		} else {
			throw new JOSEException("Unable to create signer for key: " + jwk);
		}
	}


	private static void checkQueryHash(HttpServletRequest request, List<String> paramsUsed, String hashUsed) {
		if (paramsUsed != null && !Strings.isNullOrEmpty(hashUsed)) {
			List<String> hashBase = new ArrayList<>();

			paramsUsed.forEach((q) -> {
				String first = request.getParameter(q);
				hashBase.add(UriUtils.encodeQueryParam(q, Charset.defaultCharset())
					+ "="
					+ UriUtils.encodeQueryParam(first, Charset.defaultCharset()));
			});

			String hash = Hash.SHA256_encode(Joiner.on("&").join(hashBase));

			if (!hash.equals(hashUsed)) {
				throw new RuntimeException("Couldn't validate query hash");
			}
			log.info("++ Validated query hash");
		}
	}

	/**
	 * @param digest
	 * @param req
	 */
	public static void ensureDigest(String digestHeader, HttpServletRequest req) {
		if (digestHeader != null) {
			if (digestHeader.startsWith("SHA=")) {
				byte[] savedBody = (byte[]) req.getAttribute(DigestWrappingFilter.BODY_BYTES);

				if (savedBody == null || savedBody.length == 0) {
					throw new RuntimeException("Bad Digest, no body");
				}

				String actualHash = Hash.SHA1_digest(savedBody);

				String incomingHash = digestHeader.substring("SHA=".length());

				if (!incomingHash.equals(actualHash)) {
					throw new RuntimeException("Bad Digest, no biscuit");
				}
			} else {
				throw new RuntimeException("Bad digest, unknown algorithm");
			}
		}

		log.info("++ Verified body digest");

	}


	public static void ensureContentDigest(Dictionary contentDigestHeader, HttpServletRequest req) {

		byte[] savedBody = (byte[]) req.getAttribute(DigestWrappingFilter.BODY_BYTES);

		if (savedBody == null || savedBody.length == 0) {
			if (contentDigestHeader == null) {
				return;
			} else {
				throw new RuntimeException("Bad Content Digest, no body");
			}
		} else {

			Map<String, ListElement<? extends Object>> m = contentDigestHeader.get();

			for (String alg : m.keySet()) {
				if (alg.equals("sha-512")) {
					MessageDigest sha = new SHA512.Digest();

					byte[] digest = sha.digest(savedBody);

					ByteBuffer expected = ByteBuffer.wrap(digest);
					ByteBuffer actual = ((ByteSequenceItem)m.get(alg)).get();

					if (!expected.equals(actual)) {
						throw new RuntimeException("Bad Content Digest, no biscuit");
					}

				} else if (alg.equals("sha-256")) {
					MessageDigest sha = new SHA256.Digest();

					byte[] digest = sha.digest(savedBody);

					ByteBuffer expected = ByteBuffer.wrap(digest);
					ByteBuffer actual = ((ByteSequenceItem)m.get(alg)).get();

					if (!expected.equals(actual)) {
						throw new RuntimeException("Bad Content Digest, no biscuit");
					}

				} else {
					throw new RuntimeException("Bad Content digest, unknown algorithm: " + alg);
				}
			}

			log.info("++ Verified body content-digest");

		}


	}

	public static String extractBoundAccessToken(String auth, String oauthPop) {

		// if there's an OAuth PoP style presentation, use that header's internal value
		if (!Strings.isNullOrEmpty(oauthPop)) {
			try {
				SignedJWT jwt = SignedJWT.parse(oauthPop);
				JWTClaimsSet claims = jwt.getJWTClaimsSet();
				String at = claims.getStringClaim("at");
				return Strings.emptyToNull(at);
			} catch (ParseException e) {
				log.error("Unable to parse OAuth PoP to look for token", e);
				return null;
			}
		} else if (Strings.isNullOrEmpty(auth)) {
			return null;
		} else if (!auth.startsWith("GNAP ")) {
			return null;
		} else {
			return auth.substring("GNAP ".length());
		}
	}
}
