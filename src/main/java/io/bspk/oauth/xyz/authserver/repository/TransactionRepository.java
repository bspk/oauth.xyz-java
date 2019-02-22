package io.bspk.oauth.xyz.authserver.repository;

import org.springframework.data.repository.CrudRepository;

import io.bspk.oauth.xyz.data.Transaction;

/**
 * @author jricher
 *
 */
public interface TransactionRepository extends CrudRepository<Transaction, String> {

	// TODO: this only works for bearer tokens
	Transaction findFirstByHandlesTransactionValue(String value);

	Transaction findFirstByInteractInteractId(String id);

	Transaction findFirstByInteractUserCode(String code);

}
