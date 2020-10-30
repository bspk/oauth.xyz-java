package io.bspk.oauth.xyz.authserver.repository;

import org.springframework.data.repository.CrudRepository;

import io.bspk.oauth.xyz.data.Client;

/**
 * @author jricher
 *
 */
public interface ClientRepository extends CrudRepository<Client, String> {

	Client findFirstByKeyHash(String hash);

}
