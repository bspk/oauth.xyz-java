package io.bspk.oauth.xyz.authserver.endpoint;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.Display;
import io.bspk.oauth.xyz.data.Handle;
import io.bspk.oauth.xyz.data.Interact;
import io.bspk.oauth.xyz.data.Keys;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.Status;
import io.bspk.oauth.xyz.data.api.DisplayRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
import io.bspk.oauth.xyz.http.DigestWrappingFilter;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/as/transaction")
public class TransactionEndpoint {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final String USER_CODE_CHARS = "123456789ABCDEFGHJKLMNOPQRSTUVWXYZ";

	@Autowired
	private TransactionRepository transactionRepository;

	@Value("${oauth.xyz.root}api/as/")
	private String baseUrl;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TransactionResponse> transaction(@RequestBody TransactionRequest incoming,
		@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth,
		@RequestHeader(name = "Digest", required = false) String digest,
		HttpServletRequest req) {

		// always make sure the digest header fits if appropriate
		ensureDigest(digest, req);

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
			if (incoming.getKeys() != null) {
				t.setKeys(Keys.of(incoming.getKeys()));
			}

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

		// validate the method signing as appropriate
		if (t.getKeys() != null) {
			switch (t.getKeys().getProof()) {
				case DID:
					break;
				case HTTPSIG:
					checkCavageSignature(auth, req, t.getKeys().getJwk());
					break;
				case JWSD:
					break;
				case MTLS:
					break;
				default:
					break;
			}
		}

		// make sure the interaction handle matches if we're expecting one

		if (t.getInteract().getInteractHandle() != null) {

			if (Strings.isNullOrEmpty(incoming.getInteractHandle())) {
				return ResponseEntity.badRequest().build(); // missing interaction handle (one is required)
			}

			if (!incoming.getInteractHandle().equals(Hash.SHA3_512_encode(t.getInteract().getInteractHandle()))) {
				return ResponseEntity.badRequest().build(); // bad interaction handle
			}

		}

		// rotate the transaction's own handle every time it's used (this creates one on the first time through)
		t.getHandles().setTransaction(Handle.create());

		switch (t.getStatus()) {
			case AUTHORIZED:

				// it's been authorized so we can issue a token now

				// invalidate any pending interaction stuff
				t.setInteract(new Interact());

				t.setStatus(Status.ISSUED);

				t.setAccessToken(Handle.create(Duration.ofHours(1)));

				break;
			case ISSUED:

				// we've already seen this one before

				// FIXME: this is where a refresh token would come into play, right?

				transactionRepository.save(t);
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

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
							.path("/interact/" + interactId) // this is unique per transaction
							.build().toUriString();

						t.getInteract().setInteractionUrl(interactionEndpoint)
							.setInteractId(interactId);

						t.setStatus(Status.WAITING);
					}

					if (t.getInteract().getCallback() != null) {

						String serverNonce = RandomStringUtils.randomAlphanumeric(20);

						t.getInteract().setServerNonce(serverNonce);

					}

					if (t.getInteract().isCanUserCode()) {
						String userCode = RandomStringUtils.random(8, USER_CODE_CHARS);

						t.getInteract().setUserCode(userCode);

						String deviceInteractionEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
							.path("/interact/device") // this is the same every time
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

	/**
	 * @param digest
	 * @param req
	 */
	private void ensureDigest(String digest, HttpServletRequest req) {
		String digestHeader = (String) req.getAttribute(DigestWrappingFilter.DIGEST_HASH);
		if (digestHeader != null) {
			if (digestHeader.startsWith("SHA=")) {
				String hash = digestHeader.substring("SHA=".length());
				if (!digest.equals(hash)) {
					throw new RuntimeException("Bad Digest, no biscuit");
				}
			}
		}
	}

	private void checkCavageSignature(String auth, HttpServletRequest request, JWK clientKey) {
		if (auth != null && auth.toLowerCase().startsWith("signature ")) {

			try {

				String signatureHeaderPayload = auth.substring("signature ".length());

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
