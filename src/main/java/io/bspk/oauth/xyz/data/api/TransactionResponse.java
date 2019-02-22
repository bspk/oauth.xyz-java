package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.Handle;
import io.bspk.oauth.xyz.data.HandleSet;
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

	private Handle accessToken;
	private HandleSet handles;
	private String interactionUrl;
	private String userCode;

	/**
	 * @param t
	 * @return
	 */
	public static TransactionResponse of(Transaction t) {
		return new TransactionResponse()
			.setAccessToken(t.getAccessToken())
			.setInteractionUrl(t.getInteract().getUrl())
			.setUserCode(t.getInteract().getUserCode())
			.setHandles(t.getHandles());
	}

}
