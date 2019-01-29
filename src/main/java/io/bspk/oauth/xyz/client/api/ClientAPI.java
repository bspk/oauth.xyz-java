package io.bspk.oauth.xyz.client.api;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import io.bspk.oauth.xyz.data.Interact.Type;
import io.bspk.oauth.xyz.data.PendingTransaction;
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

	@Value("${oauth.xyz.root}c/callback")
	private String clientBaseUrl;

	@Value("${oauth.xyz.root}api/as/transaction")
	private String asEndpoint;

	@PostMapping(path = "/authcode", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startAuthorizationCodeFlow(HttpSession session) {

		RestTemplate restTemplate = new RestTemplate();

		String callbackId = RandomStringUtils.randomAlphanumeric(30);

		String state = RandomStringUtils.randomAlphanumeric(20);

		TransactionRequest request = new TransactionRequest()
			.setClient(new ClientRequest())
			.setInteract(new InteractRequest()
				.setCallback(clientBaseUrl + "/" + callbackId)
				.setState(state)
				.setType(Type.REDIRECT))
			.setResource(new ResourceRequest())
			.setUser(new UserRequest());

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		PendingTransaction pending = new PendingTransaction()
			.add(request, response);

		savePendingTransactionToSession(session, pending);

		return ResponseEntity.noContent().build();

	}

	@GetMapping(path = "/pending", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getPendingTransactions(HttpSession session) {
		return ResponseEntity.ok(getPendingTransactions(session));
	}

	private void savePendingTransactionToSession(HttpSession session, PendingTransaction pending) {
		List<PendingTransaction> allPending = getPendingTransactionsFromSession(session);
		allPending.add(pending);
		session.setAttribute("pending", allPending);
	}

	private List<PendingTransaction> getPendingTransactionsFromSession(HttpSession session) {
		@SuppressWarnings("unchecked")
		List<PendingTransaction> allPending = (List<PendingTransaction>) session.getAttribute("pending");
		if (allPending == null) {
			allPending = new ArrayList<>();
			session.setAttribute("pending", allPending);
		}
		return allPending;
	}

}
