package io.bspk.oauth.xyz.authserver.endpoint;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import io.bspk.oauth.xyz.authserver.repository.ClientRepository;
import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.crypto.SignatureVerifier;
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
		String accessToken = SignatureVerifier.extractBoundAccessToken(auth, oauthPop);

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

		String accessToken = SignatureVerifier.extractBoundAccessToken(auth, oauthPop);

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
					SignatureVerifier.ensureDigest(digest, req); // make sure the digest header is accurate
					SignatureVerifier.checkCavageSignature(signature, req, t.getKey().getJwk());
					break;
				case JWSD:
					SignatureVerifier.checkDetachedJws(jwsd, req, t.getKey().getJwk());
					break;
				case DPOP:
					SignatureVerifier.checkDpop(dpop, req, t.getKey().getJwk());
					break;
				case OAUTHPOP:
					SignatureVerifier.checkOAuthPop(oauthPop, req, t.getKey().getJwk());
					break;
				case JWS:
					if (req.getMethod().equals(HttpMethod.GET.toString())
						|| req.getMethod().equals(HttpMethod.OPTIONS.toString())
						|| req.getMethod().equals(HttpMethod.DELETE.toString())
						|| req.getMethod().equals(HttpMethod.HEAD.toString())
						|| req.getMethod().equals(HttpMethod.TRACE.toString())) {

						// a body-less method was used, check the header instead
						SignatureVerifier.checkDetachedJws(jwsd, req, t.getKey().getJwk());
					} else {
						SignatureVerifier.checkAttachedJws(req, t.getKey().getJwk());
					}
					break;
				case MTLS:
				default:
					throw new RuntimeException("Unsupported Key Proof Type");
			}
		}

		// rotate the transaction's own handle every time it's used (this creates one on the first time through)
		t.setContinueAccessToken(AccessToken.createClientBound(t.getKey()));

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
			// otherwise set a single access token bound to the client's key
			t.setAccessToken(
				AccessToken.createClientBound(t.getKey()));
		}
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

}
