package io.bspk.oauth.xyz.rs;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.greenbytes.http.sfv.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import io.bspk.oauth.xyz.crypto.SignatureVerifier;
import io.bspk.oauth.xyz.data.AccessToken;
import io.bspk.oauth.xyz.data.Key;
import io.bspk.oauth.xyz.data.Key.Proof;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.api.MultipleAwareField;

/**
 * @author jricher
 *
 */
@Controller
@CrossOrigin
@RequestMapping("/api/rs")
public class ResourceEndpoint {

	@Autowired
	private TokenRepository tokenRepository;


	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getResource(
		@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth,
		@RequestHeader(name = "Signature", required = false) Dictionary signature,
		@RequestHeader(name = "Signature-Input", required = false) Dictionary signatureInput,
		@RequestHeader(name = "Content-Digest", required = false) Dictionary contentDigest,
		@RequestHeader(name = "Digest", required = false) String digest,
		@RequestHeader(name = "Detached-JWS", required = false) String jwsd,
		@RequestHeader(name = "DPoP", required = false) String dpop,
		@RequestHeader(name = "PoP", required = false) String oauthPop,
		HttpServletRequest req) {

		String tokenValue = SignatureVerifier.extractBoundAccessToken(auth, oauthPop);

		if (Strings.isNullOrEmpty(tokenValue)) {
			return ResponseEntity.notFound().build();
		}

		Transaction t = tokenRepository.findFirstByAccessTokenDataValue(tokenValue);

		if (t == null || t.getAccessToken() == null) {
			return ResponseEntity.badRequest().build();
		}

		// get the specificToken
		MultipleAwareField<AccessToken> tokens = t.getAccessToken();
		AccessToken token = null;
		if (tokens.isMultiple()) {
			token = tokens.asMultiple().stream()
				.filter(v -> v.getValue().equals(tokenValue))
				.findFirst()
				.orElse(null);
		} else {
			token = tokens.asSingle();
		}

		// find the validation method for the token
		Key k = token.getKey();
		if (k != null) {
			// if there's a key then it's not a bearer token
			if (k.getProof() == null) {
				// no proof method? shouldn't happen
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			} else {
				switch (k.getProof()) {
					case HTTPSIG:
						SignatureVerifier.ensureContentDigest(contentDigest, req); // make sure the digest header is accurate
						SignatureVerifier.checkHttpMessageSignature(signature, signatureInput, req, k.getJwk());
						break;
					case JWSD:
						SignatureVerifier.checkDetachedJws(jwsd, req, k.getJwk(), token.getValue());
						break;
					case DPOP:
						SignatureVerifier.checkDpop(dpop, req, k.getJwk(), token.getValue());
						break;
					case OAUTHPOP:
						SignatureVerifier.checkOAuthPop(oauthPop, req, k.getJwk(), token.getValue());
						break;
					case JWS:
						if (req.getMethod().equals(HttpMethod.GET.toString())
							|| req.getMethod().equals(HttpMethod.OPTIONS.toString())
							|| req.getMethod().equals(HttpMethod.DELETE.toString())
							|| req.getMethod().equals(HttpMethod.HEAD.toString())
							|| req.getMethod().equals(HttpMethod.TRACE.toString())) {

							// a body-less method was used, check the header instead
							SignatureVerifier.checkDetachedJws(jwsd, req, k.getJwk(), token.getValue());
						} else {
							SignatureVerifier.checkAttachedJws(req, k.getJwk(), token.getValue());
						}
						break;
					case MTLS:
					default:
						throw new RuntimeException("Unsupported Key Proof Type");
				}
			}
		}

		// if we get here, it's either a bearer token or we have survived the proofing process for the appropriate key

		Map<String, Object> res = ImmutableMap.of(
			"date", Instant.now(),
			"overall_access", t.getAccessTokenRequest(),
			"token_access", token.getAccessRequest(),
			"proof", Optional.ofNullable(k)
				.map(Key::getProof)
				.map(Proof::name).orElse("bearer")
		);


		return ResponseEntity.ok(res);

	}

}
