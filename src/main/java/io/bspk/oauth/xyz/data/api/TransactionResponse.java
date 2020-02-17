package io.bspk.oauth.xyz.data.api;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.Capability;
import io.bspk.oauth.xyz.data.Claims;
import io.bspk.oauth.xyz.data.Handle;
import io.bspk.oauth.xyz.data.Interact;
import io.bspk.oauth.xyz.data.Transaction;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionResponse {

	private Handle handle;
	private Handle displayHandle;
	private Handle interactHandle;
	private Handle resourceHandle;
	private Handle userHandle;
	private Handle keyHandle;
	private Handle accessToken;
	private String interactionUrl;
	private String userCodeUrl;
	private String userCode;
	private Integer wait;
	private String serverNonce;
	private List<Capability> capabilities;
	private Claims claims;

	/**
	 * @param t
	 * @return
	 */
	public static TransactionResponse of(Transaction t) {

		Optional<Interact> interact = Optional.ofNullable(t.getInteract());

		return new TransactionResponse()
			.setAccessToken(t.getAccessToken())
			.setInteractionUrl(interact
				.map(Interact::getInteractionUrl)
				.orElse(null))
			.setUserCodeUrl(interact
				.map(Interact::getUserCodeUrl)
				.orElse(null))
			.setUserCode(interact
				.map(Interact::getUserCode)
				.orElse(null))
			.setServerNonce(interact
				.map(Interact::getServerNonce)
				.orElse(null))
			.setHandle(t.getHandles().getTransaction())
			.setDisplayHandle(t.getHandles().getClient())
			.setInteractHandle(t.getHandles().getInteract())
			.setResourceHandle(t.getHandles().getResource())
			.setUserHandle(t.getHandles().getUser())
			.setClaims(t.getClaims())
			.setKeyHandle(t.getHandles().getKey());

	}

}
