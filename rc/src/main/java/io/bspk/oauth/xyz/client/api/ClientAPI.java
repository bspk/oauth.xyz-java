package io.bspk.oauth.xyz.client.api;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.nimbusds.jose.jwk.JWK;
import com.sailpoint.ietf.subjectidentifiers.model.SubjectIdentifierFormats;

import io.bspk.httpsig.HttpSigAlgorithm;
import io.bspk.oauth.xyz.client.repository.PendingTransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.crypto.KeyProofParameters;
import io.bspk.oauth.xyz.data.Interact.InteractStart;
import io.bspk.oauth.xyz.data.InteractFinish;
import io.bspk.oauth.xyz.data.InteractFinish.CallbackMethod;
import io.bspk.oauth.xyz.data.Key;
import io.bspk.oauth.xyz.data.Key.Proof;
import io.bspk.oauth.xyz.data.PendingTransaction;
import io.bspk.oauth.xyz.data.PendingTransaction.Entry;
import io.bspk.oauth.xyz.data.api.AccessTokenRequest;
import io.bspk.oauth.xyz.data.api.ClientApiRequest;
import io.bspk.oauth.xyz.data.api.ClientRequest;
import io.bspk.oauth.xyz.data.api.DisplayRequest;
import io.bspk.oauth.xyz.data.api.InteractRequest;
import io.bspk.oauth.xyz.data.api.KeyRequest;
import io.bspk.oauth.xyz.data.api.PushbackRequest;
import io.bspk.oauth.xyz.data.api.SubjectRequest;
import io.bspk.oauth.xyz.data.api.TransactionContinueRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
import io.bspk.oauth.xyz.data.api.UserRequest;
import io.bspk.oauth.xyz.http.SigningRestTemplateService;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/client")
public class ClientAPI {

	private static final String AUTH_CODE_ID = "authCodeId";
	private static final String DEVICE_ID = "deviceId";
	private static final String SCANNABLE_ID = "scannableId";

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${oauth.xyz.root}/api/client/callback")
	private String callbackBaseUrl;

	@Value("${oauth.xyz.root}/api/client/pushback")
	private String pushbackBaseUrl;

	@Value("${oauth.xyz.asEndpoint}")
	private String asEndpoint;// = "http://localhost:3000/transaction";
	//private String asEndpoint = "http://localhost:8080/openid-connect-server-webapp/transaction";
	//private String asEndpoint = "http://localhost:8080/as/transaction";

	@Value("${oauth.xyz.rsEndpoint}")
	private String rsEndpoint;

	@Value("${oauth.xyz.root}")
	private String clientPage;

	@Autowired
	private PendingTransactionRepository pendingTransactionRepository;

	@Autowired
	private SigningRestTemplateService requestSigners;

	@Autowired
	@Qualifier("clientKey")
	private JWK clientKey;

	@Autowired
	@Qualifier("clientKey2")
	private JWK clientKey2;

	@PostMapping(path = "/parameterized", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startParameterizedFlow(@RequestBody ClientApiRequest req, HttpSession session) {

		InteractRequest interact = new InteractRequest();

		if (req.getInteractStart() != null) {
			interact.setStart(req.getInteractStart());
		}

		String callbackId = RandomStringUtils.randomAlphanumeric(30);
		String nonce = RandomStringUtils.randomAlphanumeric(20);
		if (req.isInteractFinish()) {
			interact.setFinish(new InteractFinish()
				.setMethod(CallbackMethod.REDIRECT)
				.setNonce(nonce)
				.setUri(URI.create(callbackBaseUrl + "/" + callbackId)));
		}

		// translate our key to a public key and set the proofing parameters
		KeyRequest key = new KeyRequest()
			.setJwk(req.getPrivateKey().toPublicJWK())
			.setProof(req.getProof());
		ClientRequest client = new ClientRequest()
			.setDisplay(req.getDisplay())
			.setKey(key);


		TransactionRequest request = new TransactionRequest()
			.setSubject(req.getSubject())
			.setAccessToken(req.getAccessToken())
			.setUser(req.getUser())
			.setClient(client)
			.setInteract(interact);


		KeyProofParameters proofParams = new KeyProofParameters()
			.setHttpSigAlgorithm(Optional.ofNullable(req.getHttpSigAlgorithm()).orElse(HttpSigAlgorithm.JOSE))
			.setSigningKey(req.getPrivateKey())
			.setProof(req.getProof())
			.setDigestAlgorithm(req.getDigest());

		RestTemplate restTemplate = requestSigners.getSignerFor(proofParams);

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(req.getGrantEndpoint(), request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();


		PendingTransaction pending = new PendingTransaction(req.getGrantEndpoint())
			.setOwner(session.getId())
			.setProofParams(proofParams)
			.add(request, response);

		if (req.isInteractFinish()) {
			pending
				.setCallbackId(callbackId)
				.setClientNonce(nonce)
				.setHashMethod(request.getInteract().getFinish().getHashMethod());
		}

		if (!Strings.isNullOrEmpty(response.getInstanceId())) {
			session.setAttribute(AUTH_CODE_ID, response.getInstanceId());
		}

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/authcode", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startAuthorizationCodeFlow(HttpSession session) {

		String callbackId = RandomStringUtils.randomAlphanumeric(30);

		String nonce = RandomStringUtils.randomAlphanumeric(20);

		log.info("In create: " + session.getId());

		/*
		TransactionRequest request = new TransactionRequest()
			.setDisplay(new DisplayRequest()
				.setName("XYZ Redirect Client")
				.setUri("http://host.docker.internal:9834/c"))
			.setInteract(new InteractRequest()
				.setCallback(new InteractRequest.Callback()
					.setUri(callbackBaseUrl + "/" + callbackId)
					.setNonce(nonce))
				.setRedirect(true))
			.setResources(new AccessTokenRequest()
				.setResources(List.of(new RequestedResource().setHandle("foo"))))
			.setClaims(new SubjectRequest()
				.setEmail(true)
				.setSubject(true))
			.setUser(new UserRequest())
			.setKeys(new KeyRequest()
				.setJwk(clientKey.toPublicJWK())
				.setProof(Proof.JWSD));
		 */

		Key key = new Key()
			.setJwk(clientKey)
			.setProof(Proof.HTTPSIG);

		TransactionRequest request = new TransactionRequest()
			.setInteract(new InteractRequest()
				.setFinish(InteractFinish.redirect()
					.setUri(URI.create(callbackBaseUrl + "/" + callbackId))
					.setNonce(nonce))
				.setStart(InteractStart.REDIRECT))
			.setAccessToken(AccessTokenRequest.ofReferences(
					"openid",
					"profile",
					"email",
					"phone"
					))
			.setSubject(SubjectRequest.ofSubjectFormats(
					SubjectIdentifierFormats.EMAIL,
					SubjectIdentifierFormats.OPAQUE,
					SubjectIdentifierFormats.ISSUER_SUBJECT
				));
//			.setKeys(new KeyRequest()
//				.setHandle("client"));


		// load a known instance ID from this session
		String instanceId = (String) session.getAttribute(AUTH_CODE_ID);
		if (!Strings.isNullOrEmpty(instanceId)) {
			request.setClient(instanceId);
		} else {
			request.setClient(new ClientRequest()
				.setDisplay(new DisplayRequest()
					.setName("XYZ Redirect Client")
					.setUri(clientPage))
				.setKey(KeyRequest.of(key)));
		}

		KeyProofParameters params = new KeyProofParameters()
			.setSigningKey(clientKey)
			.setProof(Proof.HTTPSIG)
			.setDigestAlgorithm("sha-512")
			.setHttpSigAlgorithm(HttpSigAlgorithm.RSAPSS);

		RestTemplate restTemplate = requestSigners.getSignerFor(params, null);

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		PendingTransaction pending = new PendingTransaction(URI.create(asEndpoint))
			.setCallbackId(callbackId)
			.setClientNonce(nonce)
			.setHashMethod(request.getInteract().getFinish().getHashMethod())
			.setOwner(session.getId())
			.setProofParams(params)
			.add(request, response);

		if (!Strings.isNullOrEmpty(response.getInstanceId())) {
			session.setAttribute(AUTH_CODE_ID, response.getInstanceId());
		}

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();

	}

	@PostMapping(path = "/device", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startDeviceFlow(HttpSession session) {

		Key key = new Key()
			.setJwk(clientKey)
			.setProof(Proof.HTTPSIG);

		TransactionRequest request = new TransactionRequest()
			.setInteract(new InteractRequest()
				.setStart(InteractStart.USER_CODE))
			.setAccessToken(AccessTokenRequest.ofReferences("foo"))
			.setUser(new UserRequest());

		// load a known instance ID from this session
		String instanceId = (String) session.getAttribute(DEVICE_ID);
		if (!Strings.isNullOrEmpty(instanceId)) {
			request.setClient(instanceId);
		} else {
			request.setClient(new ClientRequest()
				.setDisplay(new DisplayRequest()
					.setName("XYZ Device Client")
					.setUri(clientPage))
				.setKey(KeyRequest.of(key)));
		}

		KeyProofParameters params = new KeyProofParameters()
			.setSigningKey(clientKey)
			.setProof(Proof.HTTPSIG)
			.setDigestAlgorithm("sha-512")
			.setHttpSigAlgorithm(HttpSigAlgorithm.RSAPSS);


		RestTemplate restTemplate = requestSigners.getSignerFor(params, null);

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		if (!Strings.isNullOrEmpty(response.getInstanceId())) {
			session.setAttribute(DEVICE_ID, response.getInstanceId());
		}

		PendingTransaction pending = new PendingTransaction(URI.create(asEndpoint))
			.setOwner(session.getId())
			.setProofParams(params)
			.add(request, response);

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/scannable", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startScannableDeviceFlow(HttpSession session) {

		Key key = new Key()
			.setJwk(clientKey)
			.setProof(Proof.JWSD);

		String callbackId = RandomStringUtils.randomAlphanumeric(30);

		String nonce = RandomStringUtils.randomAlphanumeric(20);


		TransactionRequest request = new TransactionRequest()
			.setInteract(new InteractRequest()
				.setFinish(InteractFinish.pushback()
					.setUri(URI.create(callbackBaseUrl + "/" + callbackId))
					.setNonce(nonce))
				.setStart(InteractStart.USER_CODE, InteractStart.REDIRECT))
			.setAccessToken(
				AccessTokenRequest.ofReferences("read", "write", "dolphin")
					.setLabel("blanktoken"),
				AccessTokenRequest.ofReferences("foo", "bar", "baz")
					.setLabel("magic"))
			.setUser(new UserRequest());

		// load a known instance ID from this session
		String instanceId = (String) session.getAttribute(SCANNABLE_ID);
		if (!Strings.isNullOrEmpty(instanceId)) {
			request.setClient(instanceId);
		} else {
			request.setClient(new ClientRequest()
				.setDisplay(new DisplayRequest()
					.setName("XYZ Scannable Client")
					.setUri("http://host.docker.internal:9843/c"))
				.setKey(KeyRequest.of(key)));
		}

		/*
		TransactionRequest request = new TransactionRequest()
			.setInteract(new InteractRequest()
				.setRedirect(true)
				.setUserCode(true))
			.setResources(new AccessTokenRequest()
				.setResources(List.of(
					new RequestedResource().setHandle("openid"),
					new RequestedResource().setHandle("profile"),
					new RequestedResource().setHandle("email"),
					new RequestedResource().setHandle("phone")
					)))
			.setKey(new KeyRequest()
				.setHandle("client")
				);
		 */

		KeyProofParameters params = new KeyProofParameters()
			.setSigningKey(clientKey)
			.setProof(Proof.JWSD);

		RestTemplate restTemplate = requestSigners.getSignerFor(params, null);

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		PendingTransaction pending = new PendingTransaction(URI.create(asEndpoint))
			.setCallbackId(callbackId)
			.setClientNonce(nonce)
			.setHashMethod(request.getInteract().getFinish().getHashMethod())
			.setOwner(session.getId())
			.setProofParams(params)
			.add(request, response);

		if (!Strings.isNullOrEmpty(response.getInstanceId())) {
			session.setAttribute(SCANNABLE_ID, response.getInstanceId());
		}

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();
	}


	@GetMapping(path = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getPendingTransactions(HttpSession session) {
		return ResponseEntity.ok(pendingTransactionRepository.findByOwner(session.getId()));
	}

	@GetMapping(path = "/callback/{id}")
	public ResponseEntity<?> callbackEndpoint(@PathVariable("id") String callbackId, @RequestParam("hash") String interactHash, @RequestParam("interact_ref") String interact, HttpSession session) {

		log.info("In callback: " + session.getId());

		List<PendingTransaction> transactions = pendingTransactionRepository.findByCallbackIdAndOwner(callbackId, session.getId());

		if (transactions != null && transactions.size() == 1) {

			PendingTransaction pending = transactions.get(0);

			// check the incoming hash

			String expectedHash = Hash.calculateInteractHash(pending.getClientNonce(), pending.getServerNonce(), interact, pending.getGrantEndpoint(), pending.getHashMethod());

			if (!expectedHash.equals(interactHash)) {
				return ResponseEntity.badRequest().build(); // TODO: redirect this someplace useful?
			}

			TransactionContinueRequest request = new TransactionContinueRequest()
				.setInteractRef(interact)
				;

			RestTemplate restTemplate = requestSigners.getSignerFor(pending.getProofParams(), pending.getContinueToken());

			ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(pending.getContinueUri(), request, TransactionResponse.class);

			TransactionResponse response = responseEntity.getBody();

			pending.add(request, response);

			pendingTransactionRepository.save(pending);

			return ResponseEntity.status(HttpStatus.FOUND)
				.location(UriComponentsBuilder.fromUriString(clientPage).build().toUri())
				.build();

		} else {
			return ResponseEntity.notFound().build();
		}

	}

	@PostMapping("/pushback/{id}")
	public ResponseEntity<?> pushbackEndpoint(@PathVariable("id") String callbackId, @RequestBody PushbackRequest pushback, HttpSession session) {

		List<PendingTransaction> transactions = pendingTransactionRepository.findByCallbackId(callbackId);

		if (transactions != null && transactions.size() == 1) {

			PendingTransaction pending = transactions.get(0);

			log.info("In pushback: " + session.getId() + ", "  + pending.getOwner());

			// check the incoming hash

			String expectedHash = Hash.calculateInteractHash(pending.getClientNonce(), pending.getServerNonce(), pushback.getInteractRef(), pending.getGrantEndpoint(), pending.getHashMethod());

			if (!expectedHash.equals(pushback.getHash())) {
				return ResponseEntity.badRequest().build(); // TODO: redirect this someplace useful?
			}

			TransactionContinueRequest request = new TransactionContinueRequest()
				.setInteractRef(pushback.getInteractRef())
				;

			RestTemplate restTemplate = requestSigners.getSignerFor(pending.getProofParams(), pending.getContinueToken());

			ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(pending.getContinueUri(), request, TransactionResponse.class);

			TransactionResponse response = responseEntity.getBody();

			pending.add(request, response);

			pendingTransactionRepository.save(pending);

			return ResponseEntity.noContent().build();

		} else {
			return ResponseEntity.notFound().build();
		}


	}

	@PostMapping("/poll/{id}")
	public ResponseEntity<?> poll(@PathVariable("id") String id, HttpSession session) {

		Optional<PendingTransaction> maybe = pendingTransactionRepository.findFirstByIdAndOwner(id, session.getId());

		if (maybe.isPresent()) {
			PendingTransaction pending = maybe.get();

			List<Entry> entries = pending.getEntries();

			Entry lastEntry = entries.get(entries.size() - 1); // get the most recent entry

			TransactionResponse lastResponse = lastEntry.getResponse();

			if (lastResponse.getCont() == null) {
				return ResponseEntity.notFound().build();
			}

			RestTemplate restTemplate = requestSigners.getSignerFor(pending.getProofParams(), pending.getContinueToken());

			ResponseEntity<TransactionResponse> responseEntity = restTemplate.exchange(pending.getContinueUri(), HttpMethod.POST, null, TransactionResponse.class);

			TransactionResponse response = responseEntity.getBody();

			pending.add(response);

			pendingTransactionRepository.save(pending);

			return ResponseEntity
				.ok(pending);

		} else {
			return ResponseEntity.notFound().build();
		}

	}

	@DeleteMapping("/poll/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") String id, HttpSession session) {
		Optional<PendingTransaction> maybe = pendingTransactionRepository.findFirstByIdAndOwner(id, session.getId());

		if (maybe.isPresent()) {
			PendingTransaction pending = maybe.get();

			pendingTransactionRepository.delete(pending);

			return ResponseEntity
				.noContent().build();

		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/ids")
	public ResponseEntity<?> getIds(HttpSession session) {
		Map<String, Object> ids = Collections.list(session.getAttributeNames()).stream()
			.filter(e -> e.equals(AUTH_CODE_ID) || e.equals(DEVICE_ID) || e.equals(SCANNABLE_ID))
			.collect(Collectors.toMap(
				k -> k,
				k -> session.getAttribute(k)));

		return ResponseEntity.ok(ids);
	}

	@DeleteMapping("/ids")
	public ResponseEntity<?> clearIds(HttpSession session) {
		session.removeAttribute(AUTH_CODE_ID);
		session.removeAttribute(DEVICE_ID);
		session.removeAttribute(SCANNABLE_ID);

		return ResponseEntity
			.noContent().build();
	}

	@PostMapping(value = {"/use/{id}", "/use/{id}/{token}"})
	public ResponseEntity<?> useToken(@PathVariable("id") String id,
		@PathVariable(name="token", required = false) String tokenId,
		HttpSession session) {

		Optional<PendingTransaction> maybe = pendingTransactionRepository.findFirstByIdAndOwner(id, session.getId());

		if (maybe.isPresent()) {
			PendingTransaction pending = maybe.get();

			if (Strings.isNullOrEmpty(pending.getAccessToken())
				&& pending.getMultipleAccessTokens().isEmpty()) {
				return ResponseEntity.badRequest().build();
			}

			String token = null;
			KeyProofParameters params = null;
			if (!Strings.isNullOrEmpty(tokenId)) {
				token = pending.getMultipleAccessTokens().get(tokenId);
				params = pending.getMultipleAccessTokenProofParams().get(tokenId);

			} else {
				token = pending.getAccessToken();
				params = pending.getAccessTokenProofParams();
			}

			if (token == null) {
				return ResponseEntity.badRequest().build();
			}

			RestTemplate restTemplate = requestSigners.getSignerFor(params, token);

			ResponseEntity<?> entity = restTemplate.getForEntity(rsEndpoint, String.class);

			if (!Strings.isNullOrEmpty(tokenId)) {
				pending.setMultipleRsResponse(tokenId, entity.getBody().toString());
			} else {
				pending.setRsResponse(entity.getBody().toString());
			}
			pendingTransactionRepository.save(pending);

			return ResponseEntity.ok(entity);

		} else {
			return ResponseEntity.notFound().build();
		}

	}

}
