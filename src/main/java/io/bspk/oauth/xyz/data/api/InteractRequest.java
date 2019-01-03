package io.bspk.oauth.xyz.data.api;

import io.bspk.oauth.xyz.data.Interact.Type;
import lombok.Data;

/**
 * @author jricher
 *
 */
@Data
public class InteractRequest {

	private Type type;
	private String callback;
	private String state;

}
