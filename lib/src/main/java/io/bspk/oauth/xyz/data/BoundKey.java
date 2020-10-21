package io.bspk.oauth.xyz.data;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 */
@Data
@Accessors(chain = true)
public class BoundKey {

	private Key key;
	private boolean clientKey; // true if the key is bound to the client's key

	// if key is null, this key field is unbound

}
