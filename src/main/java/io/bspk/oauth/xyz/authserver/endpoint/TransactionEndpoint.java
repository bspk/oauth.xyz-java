package io.bspk.oauth.xyz.authserver.endpoint;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.data.Handle;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.State;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/transaction")
public class TransactionEndpoint {

	@Autowired
	private TransactionRepository transactionRepository;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Transaction> transaction(@RequestBody Transaction incoming) { // TODO: create a transaction request object?

		Transaction t = null;

		if (incoming.getHandles().getTransaction() != null) {
			// load a transaction in progress

			List<Transaction> transactions = transactionRepository.findByHandlesTransactionValue(incoming.getHandles().getTransaction().getValue());

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
		}


		switch (t.getState()) {
			case AUTHORIZED:

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

				return ResponseEntity.status(HttpStatus.ACCEPTED).body(t);

				//break;
			default:

				// it's a new transaction, process it



				//break;
		}

		// rotate the transaction's own handle every time it's used (this creates one on the first time through

		t.getHandles().setTransaction(Handle.create());


		transactionRepository.save(t);

		return ResponseEntity.ok(t); // FIXME: create a transaction response object that shadows things
	}

}
