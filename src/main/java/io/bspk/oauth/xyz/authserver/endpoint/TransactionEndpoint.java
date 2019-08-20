package io.bspk.oauth.xyz.authserver.endpoint;

import java.time.Duration;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.Client;
import io.bspk.oauth.xyz.data.Handle;
import io.bspk.oauth.xyz.data.Interact;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.Status;
import io.bspk.oauth.xyz.data.api.ClientRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/as/transaction")
public class TransactionEndpoint {

	private static final String USER_CODE_CHARS = "123456789ABCDEFGHJKLMNOPQRSTUVWXYZ";

	@Autowired
	private TransactionRepository transactionRepository;

	@Value("${oauth.xyz.root}api/as/")
	private String baseUrl;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TransactionResponse> transaction(@RequestBody TransactionRequest incoming,
		@RequestHeader(name = "Authentication", required = false) String auth) {

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

			ClientRequest clientRequest = processClientRequest(incoming.getClient());

			if (clientRequest != null) {
				t.setClient(Client.of(clientRequest));
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

				if (t.getInteract() != null && t.getInteract().getType() != null) {
					switch (t.getInteract().getType()) {
						case REDIRECT:

							String interactId = RandomStringUtils.randomAlphanumeric(10);

							String interactionEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
								.path("/interact/" + interactId) // this is unique per transaction
								.build().toUriString();

							t.getInteract().setUrl(interactionEndpoint);
							t.getInteract().setInteractId(interactId);

							t.setStatus(Status.WAITING);

							break;
						case DEVICE:

							String userCode = RandomStringUtils.random(8, USER_CODE_CHARS);

							t.getInteract().setUserCode(userCode);

							String deviceInteractionEndpoint = UriComponentsBuilder.fromHttpUrl(baseUrl)
								.path("/interact/device") // this is the same every time
								.build().toUriString();


							t.getInteract().setUserCodeUrl(deviceInteractionEndpoint);

							t.setStatus(Status.WAITING);

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

		transactionRepository.save(t);
		return ResponseEntity.ok(TransactionResponse.of(t));
	}

	/**
	 * @param client
	 * @return
	 */
	private ClientRequest processClientRequest(ClientRequest client) {

		if (client == null) {
			return null;
		} else if (!Strings.isNullOrEmpty(client.getHandle())) {
			// client passed by reference, try to look it up
			// TODO
			return null;
		} else {
			// otherwise it's an incoming client request
			return client;
		}


	}

}
