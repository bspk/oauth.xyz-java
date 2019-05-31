package io.bspk.oauth.xyz.client.api;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.bspk.oauth.xyz.client.repository.PendingTransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.Interact.Type;
import io.bspk.oauth.xyz.data.PendingTransaction;
import io.bspk.oauth.xyz.data.PendingTransaction.Entry;
import io.bspk.oauth.xyz.data.api.ClientRequest;
import io.bspk.oauth.xyz.data.api.InteractRequest;
import io.bspk.oauth.xyz.data.api.ResourceRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
import io.bspk.oauth.xyz.data.api.UserRequest;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/client")
public class ClientAPI {

	@Value("${oauth.xyz.root}api/client/callback")
	private String callbackBaseUrl;

	@Value("${oauth.xyz.root}api/as/transaction")
	private String asEndpoint;

	@Value("${oauth.xyz.root}c")
	private String clientPage;

	@Autowired
	private PendingTransactionRepository pendingTransactionRepository;

	@PostMapping(path = "/authcode", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startAuthorizationCodeFlow(HttpSession session) {

		RestTemplate restTemplate = new RestTemplate();

		String callbackId = RandomStringUtils.randomAlphanumeric(30);

		String state = RandomStringUtils.randomAlphanumeric(20);

		TransactionRequest request = new TransactionRequest()
			.setClient(new ClientRequest()
				.setName("XYZ Redirect Client")
				.setUri("http://host.docker.internal:9834/c"))
			.setInteract(new InteractRequest()
				.setCallback(callbackBaseUrl + "/" + callbackId)
				.setState(state)
				.setType(Type.REDIRECT))
			.setResources(List.of(new ResourceRequest()))
			.setUser(new UserRequest());

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		PendingTransaction pending = new PendingTransaction()
			.add(request, response)
			.setCallbackId(callbackId)
			.setState(state)
			.setOwner(session.getId());

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();

	}

	@PostMapping(path = "/device", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startDeviceFlow(HttpSession session) {

		RestTemplate restTemplate = new RestTemplate();

		TransactionRequest request = new TransactionRequest()
			.setClient(new ClientRequest()
				.setName("XYZ Device Client")
				.setUri("http://host.docker.internal:9834/c"))
			.setInteract(new InteractRequest()
				.setType(Type.DEVICE))
			.setResources(List.of(new ResourceRequest()))
			.setUser(new UserRequest());

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		PendingTransaction pending = new PendingTransaction()
			.add(request, response)
			.setOwner(session.getId());

		pendingTransactionRepository.save(pending);

		return ResponseEntity.noContent().build();
	}


	@GetMapping(path = "/pending", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getPendingTransactions(HttpSession session) {
		return ResponseEntity.ok(pendingTransactionRepository.findByOwner(session.getId()));
	}

	@GetMapping(path = "/callback/{id}")
	public ResponseEntity<?> callbackEndpoint(@PathVariable("id") String callbackId, @RequestParam("state") String stateHash, @RequestParam("interact") String interact, HttpSession session) {

		List<PendingTransaction> transactions = pendingTransactionRepository.findByCallbackIdAndOwner(callbackId, session.getId());

		if (transactions != null && transactions.size() == 1) {

			PendingTransaction pending = transactions.get(0);

			// check the state
			String expectedStateHash = Hash.SHA3_512_encode(pending.getState());

			if (!expectedStateHash.equals(stateHash)) {
				return ResponseEntity.badRequest().build(); // TODO: redirect this someplace useful?
			}

			List<Entry> entries = pending.getEntries();

			Entry lastEntry = entries.get(entries.size() - 1); // get the most recent entry

			TransactionResponse lastResponse = lastEntry.getResponse();


			// get the handle

			TransactionRequest request = new TransactionRequest()
				.setHandle(lastResponse.getHandles().getTransaction().getValue())
				.setInteract(new InteractRequest().setHandle(Hash.SHA3_512_encode(interact)))
				;

			RestTemplate restTemplate = new RestTemplate();

			ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

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


			TransactionRequest request = new TransactionRequest()
				.setHandle(lastResponse.getHandles().getTransaction().getValue())
				;

			RestTemplate restTemplate = new RestTemplate();

			ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

			TransactionResponse response = responseEntity.getBody();

			pending.add(request, response);

			pendingTransactionRepository.save(pending);

			return ResponseEntity
				.ok(pending);

		} else {
			return ResponseEntity.notFound().build();
		}

	}

}
