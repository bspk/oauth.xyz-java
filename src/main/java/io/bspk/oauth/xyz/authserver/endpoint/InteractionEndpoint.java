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
import org.springframework.web.util.UriComponentsBuilder;

import io.bspk.oauth.xyz.authserver.data.api.ApprovalResponse;
import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.crypto.Hash.Method;
import io.bspk.oauth.xyz.data.PendingApproval;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.Status;
import io.bspk.oauth.xyz.data.User;
import io.bspk.oauth.xyz.data.api.ApprovalRequest;
import io.bspk.oauth.xyz.data.api.UserInteractionFormSubmission;

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

	@GetMapping("{id}")
	public ResponseEntity<?> interact(@PathVariable ("id") String id, HttpSession session) {

		Transaction transaction = transactionRepository.findFirstByInteractInteractId(id);

		if (transaction != null) {

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
			Transaction transaction = pending.getTransaction();

			// burn this interaction
			transaction.getInteract().setInteractId(null);
			transaction.getInteract().setInteractionUrl(null);

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

			if (transaction.getInteract().getCallback() != null) {
				// set up an interaction handle
				String interactRef = RandomStringUtils.randomAlphanumeric(30);
				transaction.getInteract().setInteractRef(interactRef);

				String clientNonce = transaction.getInteract().getCallback().getNonce();
				String serverNonce = transaction.getInteract().getServerNonce();
				Method method = transaction.getInteract().getCallback().getHashMethod();

				String hash = Hash.CalculateInteractHash(clientNonce,
						serverNonce,
						interactRef,
						method);


				String callback = transaction.getInteract().getCallback().getUri();
				URI callbackUri = UriComponentsBuilder.fromUriString(callback)
					.queryParam("hash", hash)
					.queryParam("interact", interactRef)
					.build().toUri();

				res.setUri(callbackUri);
			} else {
				// no callback, just set it to approved
				res.setApproved(true);
			}

			transactionRepository.save(transaction);

			return ResponseEntity.ok(res);

		} else {
			return ResponseEntity.notFound().build();
		}

	}

	@GetMapping("/device")
	public ResponseEntity<?> device(HttpSession session) {

		// save the pending approval to the session
		PendingApproval pending = new PendingApproval()
			.setRequireCode(true);

		session.setAttribute("_pending_approval", pending);

		// send the user to the interaction page

		return redirectToInteractionPage();

	}

	@PostMapping(value = "/device", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> processUserCode(HttpSession session, @RequestBody UserInteractionFormSubmission submit) {

		PendingApproval pending = (PendingApproval) session.getAttribute("_pending_approval");

		if (pending == null) {
			return ResponseEntity.notFound().build();
		}

		String userCode = submit.getUserCode();

		// normalize the input code
		userCode = userCode.replace('l', '1');
		userCode = userCode.toUpperCase();
		userCode = userCode.replace('0', 'O');
		userCode = userCode.replace('I', '1');
		userCode = userCode.replaceAll("[^123456789ABCDEFGHJKLMNOPQRSTUVWXYZ]", "");

		Transaction transaction = transactionRepository.findFirstByInteractUserCode(userCode);

		if (transaction != null) {

			// process the code submission

			// TODO: add some kind of policy matching and ask the user and stuff


			transaction.getInteract().setUserCode(null); // burn the user code
			transaction.getInteract().setInteractionUrl(null);

			/*
			transaction.setStatus(Status.AUTHORIZED);

			transactionRepository.save(transaction);
			 */
			// TODO: if we need to set up the approval page
			pending.setRequireCode(false);
			pending.setTransaction(transaction);

			session.setAttribute("_pending_approval", pending);
			//session.removeAttribute("_pending_approval");

			return ResponseEntity.noContent().build();

		} else {

			return ResponseEntity.notFound().build();

		}




	}

	private ResponseEntity<?> redirectToInteractionPage() {
		URI interactionPage = UriComponentsBuilder.fromUriString(baseUrl)
			.path("/as/interact")
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
