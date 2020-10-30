package io.bspk.oauth.xyz.authserver.repository;

import org.springframework.data.repository.CrudRepository;

import io.bspk.oauth.xyz.data.Client;
import io.bspk.oauth.xyz.data.Key;

/**
 * @author jricher
 *
 */
public interface ClientRepository extends CrudRepository<Client, String> {

	Client findFirstByKey(Key key);

}
