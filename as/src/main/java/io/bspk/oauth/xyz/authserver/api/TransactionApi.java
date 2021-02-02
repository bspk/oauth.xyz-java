package io.bspk.oauth.xyz.authserver.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import io.bspk.oauth.xyz.authserver.repository.TransactionRepository;
import io.bspk.oauth.xyz.data.Transaction;

/**
 * @author jricher
 *
 */
@Controller
@CrossOrigin
@RequestMapping("/api/transaction")
public class TransactionApi {
	@Autowired
	private TransactionRepository transactionRepository;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "getall")
	public ResponseEntity<List<Transaction>> getAll() {
		List<Transaction> res = new ArrayList<>();
		transactionRepository.findAll().forEach(res::add);

		return ResponseEntity.ok(res);
	}

	@DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "{id}")
	public ResponseEntity<?> delete(@PathVariable("id") String id) {
		transactionRepository.deleteById(id);

		return ResponseEntity.noContent().build();
	}
}
