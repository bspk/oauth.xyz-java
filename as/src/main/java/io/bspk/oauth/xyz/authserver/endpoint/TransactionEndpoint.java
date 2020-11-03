package io.bspk.oauth.xyz.authserver.endpoint;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
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

import io.bspk.oauth.xyz.authserver.repository.ClientRepository;
import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.AccessToken;
import io.bspk.oauth.xyz.data.Capability;
import io.bspk.oauth.xyz.data.Client;
import io.bspk.oauth.xyz.data.Display;
import io.bspk.oauth.xyz.data.Interact;
import io.bspk.oauth.xyz.data.Key;
import io.bspk.oauth.xyz.data.Key.Proof;
import io.bspk.oauth.xyz.data.Subject;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.Status;
import io.bspk.oauth.xyz.data.api.ClientRequest;
import io.bspk.oauth.xyz.data.api.MultiTokenResourceRequest;
import io.bspk.oauth.xyz.data.api.SingleTokenResourceRequest;
import io.bspk.oauth.xyz.data.api.TransactionContinueRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
import io.bspk.oauth.xyz.http.DigestWrappingFilter;
import io.bspk.oauth.xyz.http.JoseUnwrappingFilter;

/**
 * @author jricher
 *
 */
@Controller
@CrossOrigin
@RequestMapping("/api/as/transaction")
public class TransactionEndpoint {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final String USER_CODE_CHARS = "123456789ABCDEFGHJKLMNOPQRSTUVWXYZ";

	private Set<Capability> capabilities = Set.of(Capability.values());

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Value("${oauth.xyz.root}")
	private String baseUrl;

	@RequestMapping(method = RequestMethod.OPTIONS)
	public ResponseEntity<?> discover() {

		Map<String, Object> disco = new HashMap<>();

		disco.put("key_proofs", Proof.values());

		String txEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
			.path("/api/as/transaction")
			.build().toUriString();

		disco.put("grant_endpoint", txEndpoint);

		disco.put("capabilities", capabilities);

		disco.put("interact_methods", Set.of(
			"redirect",
			"app",
			"callback",
			"user_code"
		));

		return ResponseEntity.ok(disco);
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest incoming,
		@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth,
		@RequestHeader(name = "Signature", required = false) String signature,
		@RequestHeader(name = "Digest", required = false) String digest,
		@RequestHeader(name = "Detached-JWS", required = false) String jwsd,
		@RequestHeader(name = "DPoP", required = false) String dpop,
		@RequestHeader(name = "PoP", required = false) String oauthPop,
		HttpServletRequest req) {

		// if there's a bound access token we bail
		String accessToken = extractBoundAccessToken(auth, oauthPop);

		if (accessToken != null) {
			return ResponseEntity.badRequest().build();
		} else {
			// create a new one
			Transaction t = new Transaction();

			Client client = processClientRequest(incoming.getClient());

			Display display = client.getDisplay();

			if (display != null) {
				t.setDisplay(display);
			}

			// if there's an interaction request, copy it to the end result
			if (incoming.getInteract() != null) {
				t.setInteract(Interact.of(incoming.getInteract()));
			}

			// check key presentation
			if (client.getKey() != null) {
				t.setKey(client.getKey());
			}

			t.setSubjectRequest(incoming.getSubject());

			t.setResourceRequest(incoming.getResources());

			t.setCapabilitiesRequest(incoming.getCapabilities());

			return processTransaction(t, getInstanceIdIfNew(incoming, client), signature, digest, jwsd, dpop, oauthPop, req);
		}


	}

	// if the client's request did not contain an instance ID but the client has one now, return it because it's just been registered
	private String getInstanceIdIfNew(TransactionRequest incoming, Client client) {
		if (Strings.isNullOrEmpty(incoming.getClient().getInstanceId())
			&& !Strings.isNullOrEmpty(client.getInstanceId())) {
			return client.getInstanceId();
		} else {
			return null;
		}
	}

	@PostMapping(value = "/continue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TransactionResponse> continueTransaction(@RequestBody(required = false) TransactionContinueRequest incoming,
		@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth,
		@RequestHeader(name = "Signature", required = false) String signature,
		@RequestHeader(name = "Digest", required = false) String digest,
		@RequestHeader(name = "Detached-JWS", required = false) String jwsd,
		@RequestHeader(name = "DPoP", required = false) String dpop,
		@RequestHeader(name = "PoP", required = false) String oauthPop,
		HttpServletRequest req) {

		String accessToken = extractBoundAccessToken(auth, oauthPop);

		if (accessToken != null) {
			// load a transaction in progress

			Transaction t = transactionRepository.findFirstByContinueAccessTokenValue(accessToken);

			if (t == null) {
				return ResponseEntity.notFound().build();
			} else {

				// make sure the interaction ref matches if we're expecting one

				if (t.getInteract() != null && t.getInteract().getInteractRef() != null) {

					if (Strings.isNullOrEmpty(incoming.getInteractRef())) {
						return ResponseEntity.badRequest().build(); // missing interaction ref (one is required)
					}

					if (!incoming.getInteractRef().equals(t.getInteract().getInteractRef())) {
						return ResponseEntity.badRequest().build(); // bad interaction ref
					}

				}

				return processTransaction(t, null, signature, digest, jwsd, dpop, oauthPop, req);
			}

		} else {
			return ResponseEntity.notFound().build();
		}

	}

	private ResponseEntity<TransactionResponse> processTransaction(Transaction t, String instanceId,
		String signature,
		String digest,
		String jwsd,
		String dpop,
		String oauthPop,
		HttpServletRequest req) {

		// process the capabilities list if needed
		if (t.getCapabilitiesRequest() != null && t.getCapabilities() == null) {
			t.setCapabilities(Sets.intersection(t.getCapabilitiesRequest(), capabilities));
		}

		// validate the method signing as appropriate
		if (t.getKey() != null) {
			switch (t.getKey().getProof()) {
				case HTTPSIG:
					ensureDigest(digest, req); // make sure the digest header is accurate
					checkCavageSignature(signature, req, t.getKey().getJwk());
					break;
				case JWSD:
					checkDetachedJws(jwsd, req, t.getKey().getJwk());
					break;
				case DPOP:
					checkDpop(dpop, req, t.getKey().getJwk());
					break;
				case OAUTHPOP:
					checkOAuthPop(oauthPop, req, t.getKey().getJwk());
					break;
				case JWS:
					if (req.getMethod().equals(HttpMethod.GET.toString())
						|| req.getMethod().equals(HttpMethod.OPTIONS.toString())
						|| req.getMethod().equals(HttpMethod.DELETE.toString())
						|| req.getMethod().equals(HttpMethod.HEAD.toString())
						|| req.getMethod().equals(HttpMethod.TRACE.toString())) {

						// a body-less method was used, check the header instead
						checkDetachedJws(jwsd, req, t.getKey().getJwk());
					} else {
						checkAttachedJws(req, t.getKey().getJwk());
					}
					break;
				case MTLS:
				default:
					throw new RuntimeException("Unsupported Key Proof Type");
			}
		}

		// rotate the transaction's own handle every time it's used (this creates one on the first time through)
		t.setContinueAccessToken(AccessToken.create(t.getKey()));

		switch (t.getStatus()) {
			case AUTHORIZED:

				// it's been authorized so we can issue a token now

				// invalidate any pending interaction stuff
				t.setInteract(null);

				t.setStatus(Status.ISSUED);

				createNewAccessTokens(t);

				// if we're issuing any claims, add them here
				if (t.getSubjectRequest() != null) {
					t.setSubject(Subject.of(t.getSubjectRequest(), t.getUser()));

					// remove the claims request so that we don't issue claims on refresh
					t.setSubjectRequest(null);
				}

				break;
			case ISSUED:

				// we've already seen this one before, send out a new access token

				createNewAccessTokens(t);

				//break;
			case WAITING:

				// we're still waiting for an authorization, not much to do here

				// TODO: see if the client should back off

				transactionRepository.save(t);
				return ResponseEntity.status(HttpStatus.ACCEPTED).body(TransactionResponse.of(t, baseUrl + "/api/as/transaction/continue"));

				//break;
			case DENIED:

				// the user said no, not much to do here

				transactionRepository.save(t);
				return ResponseEntity.ok(TransactionResponse.of(t, baseUrl + "/api/as/transaction/continue"));

			case NEW:

				// it's a new transaction, see if we need interaction

				// FIXME: right now assume that we need a user

				if (t.getInteract() != null) {

					if (t.getInteract().isCanRedirect()) {

						String interactId = RandomStringUtils.randomAlphanumeric(10);

						String interactionEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
							.path("/api/as/interact/" + interactId) // this is unique per transaction
							.build().toUriString();

						t.getInteract().setInteractionUrl(interactionEndpoint)
							.setInteractId(interactId);

						t.setStatus(Status.WAITING);
					}

					// there's a callback nonce, we create a server nonce to go with it
					if (t.getInteract().getClientNonce() != null) {

						String serverNonce = RandomStringUtils.randomAlphanumeric(20);

						t.getInteract().setServerNonce(serverNonce);

					}

					if (t.getInteract().isCanUserCode()) {
						String userCode = RandomStringUtils.random(8, USER_CODE_CHARS);

						t.getInteract().setUserCode(userCode);

						String deviceInteractionEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
							.path("/device") // this is the same every time
							.build().toUriString();


						t.getInteract().setUserCodeUrl(deviceInteractionEndpoint);

						t.setStatus(Status.WAITING);
					}

				}

				break;

			default:

				// this should never happen
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		transactionRepository.save(t);
		return ResponseEntity.ok(TransactionResponse.of(t, instanceId, baseUrl + "/api/as/transaction/continue"));
	}

	private void createNewAccessTokens(Transaction t) {
		if (t.getResourceRequest() == null) {
			// no resources requested, no access token
		} else if (t.getResourceRequest().isMultiple()) {
			// if the request was for multiple resources, create multiple access tokens
			Map<String, SingleTokenResourceRequest> resources = ((MultiTokenResourceRequest)t.getResourceRequest()).getRequests();
			Map<String, AccessToken> tokens = new HashMap<>();
			for (String key : resources.keySet()) {
				tokens.put(key, AccessToken.create(Duration.ofHours(1)));
			}
			t.setMultipleAccessTokens(tokens);
		} else {
			// otherwise set a single access token
			t.setAccessToken(
				AccessToken.create(Duration.ofHours(1)));
		}
	}

	private String extractBoundAccessToken(String auth, String oauthPop) {

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

	/**
	 * @param oauthPop
	 * @param req
	 * @param jwk
	 */
	private void checkOAuthPop(String oauthPop, HttpServletRequest req, JWK jwk) {
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

		} catch (ParseException e) {
			throw new RuntimeException("Couldn't parse pop header", e);
		}

		log.info("++ Verified OAuth PoP");
	}

	private void checkQueryHash(HttpServletRequest request, List<String> paramsUsed, String hashUsed) {
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

	private void checkHeaderHash(HttpServletRequest request, List<String> headersUsed, String hashUsed) {
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


	/**
	 * @param digest
	 * @param req
	 */
	private void ensureDigest(String digestHeader, HttpServletRequest req) {
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

	private void checkCavageSignature(String signatureHeaderPayload, HttpServletRequest request, JWK clientKey) {
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

	private void checkDetachedJws(String jwsd, HttpServletRequest request, JWK clientKey) {
		if (Strings.isNullOrEmpty(jwsd)) {
			throw new RuntimeException("Missing JWS value");
		}

		try {

			Base64URL[] parts = JOSEObject.split(jwsd);
			Payload payload = new Payload((byte[])request.getAttribute(DigestWrappingFilter.BODY_BYTES));

			//log.info("<< " + payload.toBase64URL().toString());

			JWSObject jwsObject = new JWSObject(parts[0], payload, parts[2]);

			JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jwsObject.getHeader(), extractKeyForVerify(clientKey));

			if (!jwsObject.verify(verifier)) {
				throw new RuntimeException("Unable to verify JWS");
			}

			// check the URI and method
			JWSHeader header = jwsObject.getHeader();

			if (header.getCustomParam("htm") == null || !((String)header.getCustomParam("htm")).equals(request.getMethod())) {
				throw new RuntimeException("Couldn't verify method");
			}

			if (header.getCustomParam("htu") == null) {
				throw new RuntimeException("Couldn't get uri");
			} else {
				StringBuffer url = request.getRequestURL();
				if (request.getQueryString() != null) {
					url.append('?').append(request.getQueryString());
				}
				if (!header.getCustomParam("htu").equals(url.toString())) {
					throw new RuntimeException("Couldn't verify uri");
				}
			}

		} catch (ParseException | JOSEException e) {
			throw new RuntimeException("Bad JWS", e);
		}

		log.info("++ Verified Detached JWS signature");

	}

	private void checkAttachedJws(HttpServletRequest request, JWK clientKey) {
		JOSEObject jose = (JOSEObject) request.getAttribute(JoseUnwrappingFilter.BODY_JOSE);

		if (jose == null) {
			throw new RuntimeException("No JOSE object detected");
		}

		try {
			if (jose instanceof JWSObject) {
				JWSObject jws = (JWSObject)jose;

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

	private void checkDpop(String dpop, HttpServletRequest request, JWK clientKey) {
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

		} catch (ParseException | JOSEException e) {
			throw new RuntimeException("Bad DPOP Signature", e);
		}

		log.info("++ Verified DPoP signature");

	}

	private Client processClientRequest(ClientRequest client) {
		if (client == null) {
			return null;
		} else if (!Strings.isNullOrEmpty(client.getInstanceId())) {

			return clientRepository.findById(client.getInstanceId()).orElse(null);

		} else {
			if (client.getKey() != null && client.getKey().getProof() != null) {

				Key k = Key.of(client.getKey());

				Client c = clientRepository.findFirstByKey(k);

				if (c == null) {
					c = Client.of(client);
					return clientRepository.save(c);
				} else {
					return c;
				}

			} else {
				return null;
			}
		}
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


}
