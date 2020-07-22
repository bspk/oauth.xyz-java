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
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.Capability;
import io.bspk.oauth.xyz.data.Display;
import io.bspk.oauth.xyz.data.Handle;
import io.bspk.oauth.xyz.data.Interact;
import io.bspk.oauth.xyz.data.Keys;
import io.bspk.oauth.xyz.data.Keys.Proof;
import io.bspk.oauth.xyz.data.Subject;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.Status;
import io.bspk.oauth.xyz.data.api.DisplayRequest;
import io.bspk.oauth.xyz.data.api.MultiTokenResourceRequest;
import io.bspk.oauth.xyz.data.api.SingleTokenResourceRequest;
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

	@Value("${oauth.xyz.root}")
	private String baseUrl;

	@RequestMapping(method = RequestMethod.OPTIONS)
	public ResponseEntity<?> discover() {

		Map<String, Object> disco = new HashMap<>();

		disco.put("key_proofs", Proof.values());

		String txEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
			.path("/api/as/transaction")
			.build().toUriString();

		disco.put("tx_endpoint", txEndpoint);

		disco.put("capabilities", capabilities);

		disco.put("interact_methods", Set.of(
			"redirect",
			"callback",
			"user_code"
		));

		return ResponseEntity.ok(disco);
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TransactionResponse> transaction(@RequestBody TransactionRequest incoming,
		@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth,
		@RequestHeader(name = "Signature", required = false) String signature,
		@RequestHeader(name = "Digest", required = false) String digest,
		@RequestHeader(name = "Detached-JWS", required = false) String jwsd,
		@RequestHeader(name = "DPoP", required = false) String dpop,
		@RequestHeader(name = "PoP", required = false) String oauthPop,
		HttpServletRequest req) {

		Transaction t = null;

		if (incoming.getHandle() != null) {
			// load a transaction in progress

			t = transactionRepository.findFirstByHandlesTransactionValue(incoming.getHandle());

			if (t == null) {
				return ResponseEntity.notFound().build();
			}

		} else {
			// create a new one
			t = new Transaction();

			DisplayRequest displayRequest = processDisplayRequest(incoming.getDisplay());

			if (displayRequest != null) {
				t.setDisplay(Display.of(displayRequest));
			}

			/*
			t.setClient(null) // process client
				.setInteract(null) // process interact
				.setResource(null) // process resource
				.setUser(null) // process user
				;
			*/

			// if there's an interaction request, copy it to the end result
			if (incoming.getInteract() != null) {
				t.setInteract(Interact.of(incoming.getInteract()));
			}

			// check key presentation
			if (incoming.getKey() != null) {
				t.setKeys(Keys.of(incoming.getKey()));
			}

			t.setSubjectRequest(incoming.getSubject());

			t.setResourceRequest(incoming.getResources());

			t.setCapabilitiesRequest(incoming.getCapabilities());

			/*
			if (t.getClient() != null && t.getHandles().getClient() == null) {
				t.getHandles().setClient(Handle.create()); // create a new handle to represent this client, equivalent to client id/secret
			}

			if (t.getInteract() != null && t.getHandles().getInteract() == null) {
				t.getHandles().setInteract(Handle.create()); // create a handle for the interaction methods, previously embedded in client id/secret
			}

			if (t.getUser() != null && t.getHandles().getUser() == null) {
				t.getHandles().setUser(Handle.create()); // create a handle for the user, equivalent to PCT
			}

			if (t.getResource() != null && t.getHandles().getResource() == null) {
				t.getHandles().setResource(Handle.create()); // create a handle for the resource, equivalent to scopes, resource sets, etc
			}
			*/
		}

		// process the capabilities list if needed
		if (t.getCapabilitiesRequest() != null && t.getCapabilities() == null) {
			t.setCapabilities(Sets.intersection(t.getCapabilitiesRequest(), capabilities));
		}

		// validate the method signing as appropriate
		if (t.getKeys() != null) {
			switch (t.getKeys().getProof()) {
				case HTTPSIG:
					ensureDigest(digest, req); // make sure the digest header is accurate
					checkCavageSignature(signature, req, t.getKeys().getJwk());
					break;
				case JWSD:
					checkDetachedJws(jwsd, req, t.getKeys().getJwk());
					break;
				case DPOP:
					checkDpop(dpop, req, t.getKeys().getJwk());
					break;
				case OAUTHPOP:
					checkOAuthPop(oauthPop, req, t.getKeys().getJwk());
					break;
				case JWS:
					checkAttachedJws(req, t.getKeys().getJwk());
					break;
				case MTLS:
				default:
					throw new RuntimeException("Unsupported Key Proof Type");
			}
		}

		// make sure the interaction ref matches if we're expecting one

		if (t.getInteract() != null && t.getInteract().getInteractRef() != null) {

			if (Strings.isNullOrEmpty(incoming.getInteractRef())) {
				return ResponseEntity.badRequest().build(); // missing interaction ref (one is required)
			}

			if (!incoming.getInteractRef().equals(t.getInteract().getInteractRef())) {
				return ResponseEntity.badRequest().build(); // bad interaction ref
			}

		}

		// rotate the transaction's own handle every time it's used (this creates one on the first time through)
		t.getHandles().setTransaction(Handle.create());

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
				return ResponseEntity.status(HttpStatus.ACCEPTED).body(TransactionResponse.of(t));

				//break;
			case DENIED:

				// the user said no, not much to do here

				transactionRepository.save(t);
				return ResponseEntity.ok(TransactionResponse.of(t));

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

					if (t.getInteract().getCallback() != null && t.getInteract().getCallback().getUri() != null) {

						String serverNonce = RandomStringUtils.randomAlphanumeric(20);

						t.getInteract().setServerNonce(serverNonce);

					}

					if (t.getInteract().isCanUserCode()) {
						String userCode = RandomStringUtils.random(8, USER_CODE_CHARS);

						t.getInteract().setUserCode(userCode);

						String deviceInteractionEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
							.path("/api/as/interact/device") // this is the same every time
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
		return ResponseEntity.ok(TransactionResponse.of(t));
	}

	private void createNewAccessTokens(Transaction t) {
		if (t.getResourceRequest() == null) {
			// no resources requested, no access token
		} else if (t.getResourceRequest().isMultiple()) {
			// if the request was for multiple resources, create multiple access tokens
			Map<String, SingleTokenResourceRequest> resources = ((MultiTokenResourceRequest)t.getResourceRequest()).getRequests();
			Map<String, Handle> tokens = new HashMap<>();
			for (String key : resources.keySet()) {
				tokens.put(key, Handle.create(Duration.ofHours(1)));
			}
			t.setMultipleAccessTokens(tokens);
		} else {
			// otherwise set a single access token
			t.setAccessToken(Handle.create(Duration.ofHours(1)));
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
		try {

			Base64URL[] parts = JOSEObject.split(jwsd);
			Payload payload = new Payload((byte[])request.getAttribute(DigestWrappingFilter.BODY_BYTES));

			//log.info("<< " + payload.toBase64URL().toString());

			JWSObject jwsObject = new JWSObject(parts[0], payload, parts[2]);

			JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jwsObject.getHeader(),
				((RSAKey)clientKey).toRSAPublicKey());

			if (!jwsObject.verify(verifier)) {
				throw new RuntimeException("Unable to verify JWS");
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

				JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jws.getHeader(),
					((RSAKey)clientKey).toRSAPublicKey());

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

			RSASSAVerifier verifier = new RSASSAVerifier((RSAKey)clientKey);

			if (!jwt.verify(verifier)) {
				throw new RuntimeException("Unable to verify DPOP Signature");
			}

			JWTClaimsSet claims = jwt.getJWTClaimsSet();

			if (claims.getClaim("http_method") == null || !claims.getClaim("http_method").equals(request.getMethod())) {
				throw new RuntimeException("Couldn't verify method");
			}

			if (claims.getClaim("http_uri") == null) {
				throw new RuntimeException("Couldn't get uri");
			} else {
				StringBuffer url = request.getRequestURL();
				if (request.getQueryString() != null) {
					url.append('?').append(request.getQueryString());
				}
				if (!claims.getClaim("http_uri").equals(url.toString())) {
					throw new RuntimeException("Couldn't verify uri");
				}
			}

		} catch (ParseException | JOSEException e) {
			throw new RuntimeException("Bad DPOP Signature", e);
		}

		log.info("++ Verified DPoP signature");

	}

	/**
	 * @param display
	 * @return
	 */
	private DisplayRequest processDisplayRequest(DisplayRequest display) {

		if (display == null) {
			return null;
		} else if (!Strings.isNullOrEmpty(display.getHandle())) {
			// client passed by reference, try to look it up
			// TODO
			return null;
		} else {
			// otherwise it's an incoming client request
			return display;
		}
	}

}
