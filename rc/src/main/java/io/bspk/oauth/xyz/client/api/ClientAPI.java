package io.bspk.oauth.xyz.client.api;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jose.jwk.JWK;

import io.bspk.oauth.xyz.client.repository.PendingTransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.Keys.Proof;
import io.bspk.oauth.xyz.data.PendingTransaction;
import io.bspk.oauth.xyz.data.PendingTransaction.Entry;
import io.bspk.oauth.xyz.data.api.DisplayRequest;
import io.bspk.oauth.xyz.data.api.InteractRequest;
import io.bspk.oauth.xyz.data.api.KeyRequest;
import io.bspk.oauth.xyz.data.api.RequestedResource;
import io.bspk.oauth.xyz.data.api.SingleTokenResourceRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
import io.bspk.oauth.xyz.data.api.UserRequest;
import io.bspk.oauth.xyz.http.SigningRestTemplate;
import io.bspk.oauth.xyz.http.SigningRestTemplateService;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/client")
public class ClientAPI {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${oauth.xyz.root}api/client/callback")
	private String callbackBaseUrl;

	@Value("${oauth.xyz.asEndpoint}")
	private String asEndpoint;// = "http://localhost:3000/transaction";
	//private String asEndpoint = "http://localhost:8080/openid-connect-server-webapp/transaction";
	//private String asEndpoint = "http://localhost:8080/as/transaction";

	@Value("${oauth.xyz.root}")
	private String clientPage;

	@Autowired
	private PendingTransactionRepository pendingTransactionRepository;

	@Autowired
	private SigningRestTemplateService requestSigners;

	@Autowired
	private JWK clientKey;

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
			.setResources(new SingleTokenResourceRequest()
				.setResources(List.of(new RequestedResource().setHandle("foo"))))
			.setClaims(new SubjectRequest()
				.setEmail(true)
				.setSubject(true))
			.setUser(new UserRequest())
			.setKeys(new KeyRequest()
				.setJwk(clientKey.toPublicJWK())
				.setProof(Proof.JWSD));
		 */


		TransactionRequest request = new TransactionRequest()
			.setDisplay(new DisplayRequest()
				.setName("XYZ Redirect Client")
				.setUri(clientPage))
			.setInteract(new InteractRequest()
				.setCallback(new InteractRequest.Callback()
					.setUri(callbackBaseUrl + "/" + callbackId)
					.setNonce(nonce))
				.setRedirect(true))
			.setResources(SingleTokenResourceRequest.ofReferences(
					"openid",
					"profile",
					"email",
					"phone"
					))
			.setKey(new KeyRequest()
				.setJwk(clientKey.toPublicJWK())
				.setProof(Proof.JWSD));
//			.setKeys(new KeyRequest()
//				.setHandle("client"));


		Proof proof = Proof.JWSD;

		SigningRestTemplate restTemplate = requestSigners.getSignerFor(proof);

		TransactionResponse response = restTemplate.createTransaction(nonce, request);

		PendingTransaction pending = new PendingTransaction()
			.add(request, response)
			.setCallbackId(callbackId)
			.setClientNonce(nonce)
			.setServerNonce(response.getCallbackServerNonce())
			.setHashMethod(request.getInteract().getCallback().getHashMethod())
			.setOwner(session.getId())
			.setProofMethod(proof)
			.setKeyHandle(response.getKeyHandle()); // we save the key handle for display, but we could re-use it in future calls if we remembered it

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();

	}

	@PostMapping(path = "/device", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startDeviceFlow(HttpSession session) {

		Proof proof = Proof.HTTPSIG;

		TransactionRequest request = new TransactionRequest()
			.setDisplay(new DisplayRequest()
				.setName("XYZ Device Client")
				.setUri(clientPage))
			.setInteract(new InteractRequest()
				.setUserCode(true))
			.setResources(new SingleTokenResourceRequest()
				.setResources(List.of(new RequestedResource().setHandle("foo"))))
			.setUser(new UserRequest())
			.setKey(new KeyRequest()
				.setJwk(clientKey.toPublicJWK())
				.setProof(proof));

		SigningRestTemplate restTemplate = requestSigners.getSignerFor(proof);

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		PendingTransaction pending = new PendingTransaction()
			.add(request, response)
			.setOwner(session.getId())
			.setProofMethod(proof);

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/scannable", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startScannableDeviceFlow(HttpSession session) {

		Proof proof = Proof.JWSD;

		/*
		TransactionRequest request = new TransactionRequest()
			.setDisplay(new DisplayRequest()
				.setName("XYZ Scannable Client")
				.setUri("http://host.docker.internal:9843/c"))
			.setInteract(new InteractRequest()
				.setUserCode(true)
				.setRedirect(true))
			.setResources(new MultiTokenResourceRequest()
				.setRequests(Map.of(
					"blanktoken", new SingleTokenResourceRequest()
						.setResources(List.of(new RequestedResource()
							.setActions(List.of("read", "write", "dolphin")))),
					"magic", new SingleTokenResourceRequest()
						.setResources(List.of(new RequestedResource()
							.setActions(List.of("foo", "bar", "baz")))))))
			.setUser(new UserRequest())
			.setKeys(new KeyRequest()
				.setJwk(clientKey.toPublicJWK())
				.setProof(proof));
		 */

		TransactionRequest request = new TransactionRequest()
			.setInteract(new InteractRequest()
				.setRedirect(true)
				.setUserCode(true))
			.setResources(new SingleTokenResourceRequest()
				.setResources(List.of(
					new RequestedResource().setHandle("openid"),
					new RequestedResource().setHandle("profile"),
					new RequestedResource().setHandle("email"),
					new RequestedResource().setHandle("phone")
					)))
			.setKey(new KeyRequest()
				.setHandle("client")
				);


		SigningRestTemplate restTemplate = requestSigners.getSignerFor(proof);

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		PendingTransaction pending = new PendingTransaction()
			.add(request, response)
			.setOwner(session.getId())
			.setProofMethod(proof);

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();
	}


	@GetMapping(path = "/pending", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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

			String expectedHash = Hash.CalculateInteractHash(pending.getClientNonce(), pending.getServerNonce(), interact, pending.getHashMethod());

			if (!expectedHash.equals(interactHash)) {
				return ResponseEntity.badRequest().build(); // TODO: redirect this someplace useful?
			}

			List<Entry> entries = pending.getEntries();

			Entry lastEntry = entries.get(entries.size() - 1); // get the most recent entry

			TransactionResponse lastResponse = lastEntry.getResponse();


			// get the handle

			TransactionRequest request = new TransactionRequest()
				.setHandle(pending.getContinueHandle())
				.setInteractRef(interact)
				;

			SigningRestTemplate restTemplate = requestSigners.getSignerFor(pending.getProofMethod());

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

			TransactionRequest request = new TransactionRequest()
				.setHandle(pending.getContinueHandle())
				;

			SigningRestTemplate restTemplate = requestSigners.getSignerFor(pending.getProofMethod());

			ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(pending.getContinueUri(), request, TransactionResponse.class);

			TransactionResponse response = responseEntity.getBody();

			pending.add(request, response);

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



}
