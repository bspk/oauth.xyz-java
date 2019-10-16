package io.bspk.oauth.xyz.data;

import java.util.Optional;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.api.InteractRequest;
import io.bspk.oauth.xyz.data.api.InteractRequest.Callback;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Interact {

	/** request parameters */
	private Callback callback;
	private boolean canRedirect;
	private boolean canUserCode;
	private boolean canDidComm;
	private boolean canDidCommQuery;

	private String interactionUrl;
	private String interactId;
	private String serverNonce;
	private String interactHandle;
	private String userCode;
	private String userCodeUrl;

	/**
	 * @param interact
	 * @return
	 */
	public static Interact of(InteractRequest interact) {
		return new Interact()
			.setCanDidComm(Optional.ofNullable(interact.getDidComm()).orElse(Boolean.FALSE))
			.setCanDidCommQuery(Optional.ofNullable(interact.getDidCommQuery()).orElse(Boolean.FALSE))
			.setCanUserCode(Optional.ofNullable(interact.getUserCode()).orElse(Boolean.FALSE))
			.setCanRedirect(Optional.ofNullable(interact.getRedirect()).orElse(Boolean.FALSE))
			.setCallback(interact.getCallback());
	}

}
