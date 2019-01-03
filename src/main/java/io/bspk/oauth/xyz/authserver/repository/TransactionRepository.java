package io.bspk.oauth.xyz.authserver.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import io.bspk.oauth.xyz.data.Transaction;

/**
 * @author jricher
 *
 */
public interface TransactionRepository extends CrudRepository<Transaction, String> {

	List<Transaction> findByHandlesTransactionValue(String value);

}
