package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.Interact;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class InteractResponse {

	private String redirect;
	private String app;
	private String callback;
	private UserCodeResponse userCode;


	public static InteractResponse of (Interact interact) {

		if (interact == null) {
			return null;
		}

		return new InteractResponse()
			.setRedirect(interact.getInteractionUrl())
			.setApp(interact.getAppUrl())
			.setCallback(interact.getServerNonce())
			.setUserCode(UserCodeResponse.of(interact));


	}

}
