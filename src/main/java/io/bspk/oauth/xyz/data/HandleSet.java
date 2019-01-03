package io.bspk.oauth.xyz.data;

import lombok.Data;

/**
 * @author jricher
 *
 */
@Data
public class HandleSet {

	private Handle transaction;
	private Handle client;
	private Handle user;
	private Handle resource;
	private Handle interact;

}
