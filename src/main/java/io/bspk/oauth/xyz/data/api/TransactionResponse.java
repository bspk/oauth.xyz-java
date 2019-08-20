package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.Handle;
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
	private Handle clientHandle;
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

	/**
	 * @param t
	 * @return
	 */
	public static TransactionResponse of(Transaction t) {
		return new TransactionResponse()
			.setAccessToken(t.getAccessToken())
			.setInteractionUrl(t.getInteract().getUrl())
			.setUserCodeUrl(t.getInteract().getUserCodeUrl())
			.setUserCode(t.getInteract().getUserCode())
			.setServerNonce(t.getInteract().getServerNonce())
			.setHandle(t.getHandles().getTransaction())
			.setClientHandle(t.getHandles().getClient())
			.setInteractHandle(t.getHandles().getInteract())
			.setResourceHandle(t.getHandles().getResource())
			.setUserHandle(t.getHandles().getUser())
			.setKeyHandle(t.getHandles().getKey());

	}

}
