package io.bspk.oauth.xyz.rs;

import org.springframework.data.repository.CrudRepository;

import io.bspk.oauth.xyz.data.Transaction;

/**
 * @author jricher
 *
 */
public interface TokenRepository extends CrudRepository<Transaction, String> {

	Transaction findFirstByAccessTokenDataValue(String value);

}