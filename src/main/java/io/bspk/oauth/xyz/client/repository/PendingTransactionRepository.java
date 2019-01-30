package io.bspk.oauth.xyz.client.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import io.bspk.oauth.xyz.data.PendingTransaction;

/**
 * @author jricher
 *
 */
public interface PendingTransactionRepository extends CrudRepository<PendingTransaction, String> {

	List<PendingTransaction> findByOwner(String owner);

	List<PendingTransaction> findByCallbackIdAndOwner(String callbackId, String owner);

}
