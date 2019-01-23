package io.bspk.oauth.xyz.authserver.endpoint;

import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.data.Handle;
import io.bspk.oauth.xyz.data.Interact;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.State;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/as/transaction")
public class TransactionEndpoint {

	@Autowired
	private TransactionRepository transactionRepository;

	@Value("${oauth.xyz.root}api/as/")
	String baseUrl;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TransactionResponse> transaction(@RequestBody TransactionRequest incoming) {

		Transaction t = null;

		if (incoming.getTransactionHandle() != null) {
			// load a transaction in progress

			List<Transaction> transactions = transactionRepository.findByHandlesTransactionValue(incoming.getTransactionHandle());

			if (transactions.size() == 1) {
				t = transactions.get(0);
			} else {
				return ResponseEntity.notFound().build();
			}

		} else {
			// create a new one
			t = new Transaction();

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


		switch (t.getState()) {
			case AUTHORIZED:

				// it's been authorized so we can issue a token now

				t.setState(State.ISSUED);

				t.setAccessToken(Handle.create(Duration.ofHours(1)));

				break;
			case ISSUED:

				// we've already seen this one before

				// FIXME: this is where a refresh token would come into play, right?

				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

				//break;
			case WAITING:

				// we're still waiting for an authorization, not much to do here

				// TODO: see if the client should back off

				return ResponseEntity.status(HttpStatus.ACCEPTED).body(TransactionResponse.of(t));

				//break;
			case NEW:

				// it's a new transaction, see if we need interaction

				// FIXME: right now assume that we need a user

				if (t.getInteract() != null && t.getInteract().getType() != null) {
					switch (t.getInteract().getType()) {
						case REDIRECT:

							String interactId = RandomStringUtils.randomAlphanumeric(10);

							String interactionEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
								.path("/interact/" + interactId)
								.build().toUriString();

							t.getInteract().setUrl(interactionEndpoint);
							t.getInteract().setInteractId(interactId);

							t.setState(State.WAITING);

							break;
						default:

							// this isn't an interaction we can handle

							return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
					}
				}

				break;

			default:

				// this should never happen
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		// rotate the transaction's own handle every time it's used (this creates one on the first time through

		t.getHandles().setTransaction(Handle.create());


		transactionRepository.save(t);

		return ResponseEntity.ok(TransactionResponse.of(t));
	}

}
