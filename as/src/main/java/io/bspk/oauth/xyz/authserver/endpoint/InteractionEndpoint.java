package io.bspk.oauth.xyz.authserver.endpoint;

import java.net.URI;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.bspk.oauth.xyz.authserver.data.api.ApprovalRequest;
import io.bspk.oauth.xyz.authserver.data.api.ApprovalResponse;
import io.bspk.oauth.xyz.authserver.data.api.PendingApproval;
import io.bspk.oauth.xyz.authserver.data.api.UserInteractionFormSubmission;
import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.crypto.Hash.HashMethod;
import io.bspk.oauth.xyz.data.InteractFinish.CallbackMethod;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.Status;
import io.bspk.oauth.xyz.data.User;
import io.bspk.oauth.xyz.data.api.PushbackRequest;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/as/interact")
public class InteractionEndpoint {


	@Autowired
	private TransactionRepository transactionRepository;

	@Value("${oauth.xyz.root}")
	private String baseUrl;

	@Autowired
	private RestTemplate restTemplate; // for pushbacks

	@GetMapping("{id}")
	public ResponseEntity<?> interact(@PathVariable ("id") String id, HttpSession session) {

		Transaction transaction = transactionRepository.findFirstByInteractInteractId(id);

		if (transaction != null) {

			// burn this interaction
			transaction.getInteract().setInteractId(null);
			transaction.getInteract().setInteractionUrl(null);

			transactionRepository.save(transaction);

			PendingApproval pending = new PendingApproval()
				.setTransaction(transaction);

			session.setAttribute("_pending_approval", pending);
		}

		return redirectToInteractionPage();

	}


	@PostMapping("approve")
	public ResponseEntity<?> approve(@RequestBody ApprovalRequest approve, HttpSession session) {

		PendingApproval pending = (PendingApproval) session.getAttribute("_pending_approval");

		session.removeAttribute("_pending_approval");

		if (pending != null && pending.getTransaction() != null) {
			// note we need to grab a fresh copy because we'll udpate it
			Transaction transaction = transactionRepository.findById(pending.getTransaction().getId()).orElseThrow();

			if (approve.isApproved()) {
				transaction.setStatus(Status.AUTHORIZED);

				transaction.setUser(new User()
					.setId(session.getId())
					.setEmail("user@example.com")
					.setPhone("555-user")
					.setUpdatedAt(Instant.ofEpochMilli(session.getCreationTime()))
					);
			} else {

				transaction.setStatus(Status.DENIED);

			}


			ApprovalResponse res = new ApprovalResponse();

			if (transaction.getInteract().getCallbackMethod() != null) {
				// set up an interaction handle
				String interactRef = RandomStringUtils.randomAlphanumeric(30);
				transaction.getInteract().setInteractRef(interactRef);

				transactionRepository.save(transaction); // save the interaction reference and the state; note we have to do this before processing the callback

				String clientNonce = transaction.getInteract().getClientNonce();
				String serverNonce = transaction.getInteract().getServerNonce();
				HashMethod hashMethod = transaction.getInteract().getCallbackHashMethod();

				String hash = Hash.CalculateInteractHash(clientNonce,
						serverNonce,
						interactRef,
						hashMethod);

				if (transaction.getInteract().getCallbackMethod().equals(CallbackMethod.REDIRECT)) {
					// do a redirection

					String callback = transaction.getInteract().getCallbackUri();
					URI callbackUri = UriComponentsBuilder.fromUriString(callback)
						.queryParam("hash", hash)
						.queryParam("interact_ref", interactRef)
						.build().toUri();

					res.setUri(callbackUri);
				} else {
					// do a push to the client

					PushbackRequest pushback = new PushbackRequest()
						.setHash(hash)
						.setInteractRef(interactRef);

					ResponseEntity<Void> response = restTemplate.postForEntity(transaction.getInteract().getCallbackUri(), pushback, Void.class);

					// callback handled in background
					res.setApproved(true);
				}
			} else {
				// no callback, just set it to approved
				transactionRepository.save(transaction);

				res.setApproved(true);
			}
			return ResponseEntity.ok(res);

		} else {
			return ResponseEntity.notFound().build();
		}

	}

	@PostMapping(value = "/device", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> processUserCode(HttpSession session, @RequestBody UserInteractionFormSubmission submit) {

		String userCode = submit.getUserCode();

		// normalize the input code
		userCode = userCode.replace('l', '1'); // lowercase ell is a one
		userCode = userCode.toUpperCase(); // shift everything to uppercase
		userCode = userCode.replace('0', 'O'); // oh is zero
		userCode = userCode.replace('I', '1'); // aye is one
		userCode = userCode.replaceAll("[^123456789ABCDEFGHJKLMNOPQRSTUVWXYZ]", ""); // throw out all invalid characters

		Transaction transaction = transactionRepository.findFirstByInteractUserCode(userCode);

		if (transaction != null) {

			// process the code submission

			// TODO: add some kind of policy matching and ask the user and stuff


			transaction.getInteract().setUserCode(null); // burn the user code
			transaction.getInteract().setInteractionUrl(null);

			transactionRepository.save(transaction);

			PendingApproval pending = new PendingApproval()
				.setTransaction(transaction);

			session.setAttribute("_pending_approval", pending);

			return ResponseEntity.noContent().build();

		} else {

			return ResponseEntity.notFound().build();

		}




	}

	private ResponseEntity<?> redirectToInteractionPage() {
		URI interactionPage = UriComponentsBuilder.fromUriString(baseUrl)
			.path("/interact")
			.build().toUri();

		return ResponseEntity.status(HttpStatus.FOUND)
			.location(interactionPage)
			.build();
	}


	@GetMapping("/pending")
	public ResponseEntity<?> getPending(HttpSession session) {
		PendingApproval pending = (PendingApproval) session.getAttribute("_pending_approval");

		return ResponseEntity.of(Optional.ofNullable(pending));

	}

}
