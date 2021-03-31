package io.bspk.oauth.xyz.authserver.endpoint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenbytes.http.sfv.Dictionary;
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
import io.bspk.oauth.xyz.data.Interact.InteractStart;
import io.bspk.oauth.xyz.data.Key;
import io.bspk.oauth.xyz.data.Key.Proof;
import io.bspk.oauth.xyz.data.Subject;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.Status;
import io.bspk.oauth.xyz.data.api.AccessTokenRequest;
import io.bspk.oauth.xyz.data.api.ClientRequest;
import io.bspk.oauth.xyz.data.api.HandleAwareField;
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
		@RequestHeader(name = "Signature", required = false) Dictionary signature,
		@RequestHeader(name = "Signature-Input", required = false) Dictionary signatureInput,
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

			t.setAccessTokenRequest(incoming.getAccessToken());

			t.setCapabilitiesRequest(incoming.getCapabilities());

			return processTransaction(t, getInstanceIdIfNew(incoming, client), signature, signatureInput, digest, jwsd, dpop, oauthPop, req, null);
		}


	}

	// if the client's request did not contain an instance ID but the client has one now, return it because it's just been registered
	private String getInstanceIdIfNew(TransactionRequest incoming, Client client) {
		if (!incoming.getClient().isHandled()
			&& !Strings.isNullOrEmpty(client.getInstanceId())) {
			return client.getInstanceId();
		} else {
			return null;
		}
	}

	@PostMapping(value = "/continue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TransactionResponse> continueTransaction(@RequestBody(required = false) TransactionContinueRequest incoming,
		@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String auth,
		@RequestHeader(name = "Signature", required = false) Dictionary signature,
		@RequestHeader(name = "Signature-Input", required = false) Dictionary signatureInput,
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

				return processTransaction(t, null, signature, signatureInput, digest, jwsd, dpop, oauthPop, req, accessToken);
			}

		} else {
			return ResponseEntity.notFound().build();
		}

	}

	private ResponseEntity<TransactionResponse> processTransaction(Transaction t, String instanceId,
		Dictionary signature,
		Dictionary signatureInput,
		String digest,
		String jwsd,
		String dpop,
		String oauthPop,
		HttpServletRequest req,
		String accessToken) {

		// process the capabilities list if needed
		if (t.getCapabilitiesRequest() != null && t.getCapabilities() == null) {
			t.setCapabilities(Sets.intersection(t.getCapabilitiesRequest(), capabilities));
		}

		// validate the method signing as appropriate
		if (t.getKey() != null) {
			switch (t.getKey().getProof()) {
				case HTTPSIG:
					SignatureVerifier.ensureDigest(digest, req); // make sure the digest header is accurate
					SignatureVerifier.checkHttpMessageSignature(signature, signatureInput, req, t.getKey().getJwk());
					break;
				case JWSD:
					SignatureVerifier.checkDetachedJws(jwsd, req, t.getKey().getJwk(), accessToken);
					break;
				case DPOP:
					SignatureVerifier.checkDpop(dpop, req, t.getKey().getJwk(), accessToken);
					break;
				case OAUTHPOP:
					SignatureVerifier.checkOAuthPop(oauthPop, req, t.getKey().getJwk(), accessToken);
					break;
				case JWS:
					if (req.getMethod().equals(HttpMethod.GET.toString())
						|| req.getMethod().equals(HttpMethod.OPTIONS.toString())
						|| req.getMethod().equals(HttpMethod.DELETE.toString())
						|| req.getMethod().equals(HttpMethod.HEAD.toString())
						|| req.getMethod().equals(HttpMethod.TRACE.toString())) {

						// a body-less method was used, check the header instead
						SignatureVerifier.checkDetachedJws(jwsd, req, t.getKey().getJwk(), accessToken);
					} else {
						SignatureVerifier.checkAttachedJws(req, t.getKey().getJwk(), accessToken);
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

					if (t.getInteract().getStartMethods().contains(InteractStart.REDIRECT)) {

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

					if (t.getInteract().getStartMethods().contains(InteractStart.USER_CODE)) {
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
		if (t.getAccessTokenRequest() == null) {
			// no resources requested, no access token
		} else if (t.getAccessTokenRequest().isMultiple()) {
			// if the request was for multiple resources, create multiple access tokens
			List<AccessTokenRequest> resources = t.getAccessTokenRequest().asMultiple();
			List<AccessToken> tokens = new ArrayList<>();
			for (AccessTokenRequest req : resources) {
				if (Strings.isNullOrEmpty(req.getLabel())) {
					throw new IllegalArgumentException("Required 'label' not found");
				}
				if (req.isBearer()) {
					tokens.add(
						AccessToken.create(Duration.ofHours(1))
							.setLabel(req.getLabel())
							.setAccessRequest(req.getAccess()));
				} else {
					tokens.add(
						AccessToken.createClientBound(t.getKey())
							.setLabel(req.getLabel())
							.setAccessRequest(req.getAccess()));
				}
			}
			t.setAccessToken(tokens);
		} else {
			// otherwise set a single access token bound to the client's key
			AccessTokenRequest req = t.getAccessTokenRequest().asSingle();
			if (req.isBearer()) {
				t.setAccessToken(
					AccessToken.create(Duration.ofHours(1))
						.setAccessRequest(req.getAccess()));
			} else {
				t.setAccessToken(
					AccessToken.createClientBound(t.getKey())
						.setAccessRequest(req.getAccess()));
			}
		}
	}

	private Client processClientRequest(HandleAwareField<ClientRequest> client) {
		if (client == null) {
			// no client provided
			return null;
		} else if (client.isHandled()) {
			// look up by instance ID
			return clientRepository.findById(client.asHandle()).orElse(null);

		} else {
			// look up by key

			Key k = Key.of(client.asValue().getKey());
			if (k != null && k.getProof() != null) {
				// look up by key if possible
				Client c = clientRepository.findFirstByKey(k);

				if (c == null) {
					// we didn't find a client, register a new one
					c = Client.of(client.asValue());
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
