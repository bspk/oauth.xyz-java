package io.bspk.oauth.xyz.authserver.endpoint;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.data.Transaction;
import io.bspk.oauth.xyz.data.Transaction.State;
import io.bspk.oauth.xyz.data.User;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/as/interact")
public class InteractionEndpoint {


	@Autowired
	private TransactionRepository transactionRepository;

	@Value("${oauth.xyz.root}api/as/")
	private String baseUrl;

	@GetMapping("{id}")
	public ResponseEntity<?> interact(@PathVariable ("id") String id, HttpSession session) {

		List<Transaction> transactions = transactionRepository.findByInteractInteractId(id);

		if (transactions != null && transactions.size() == 1) {
			// found exactly one

			Transaction transaction = transactions.get(0);

			// TODO: add some kind of policy matching and ask the user and stuff

			String callback = transaction.getInteract().getCallback();

			// burn this interaction
			transaction.getInteract().setInteractId(null);

			transaction.getInteract().setUrl(null);

			transaction.setState(State.AUTHORIZED);

			transaction.setUser(new User().setId(session.getId()));

			transactionRepository.save(transaction);

			return ResponseEntity.status(HttpStatus.FOUND)
				.location(UriComponentsBuilder.fromUriString(callback).build().toUri())
				.build();

		} else {
			// there was an error in the lookup, delete all the pending ones
			transactionRepository.deleteAll(transactions);

			return ResponseEntity.notFound().build();
		}

	}


}
