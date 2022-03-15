package io.bspk.oauth.xyz.data.api;

import java.net.URI;

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

	private URI redirect;
	private URI app;
	private String finish;
	private UserCodeResponse userCode;
	private UserCodeUriResponse userCodeUri;


	public static InteractResponse of (Interact interact) {

		if (interact == null) {
			return null;
		}

		return new InteractResponse()
			.setRedirect(interact.getInteractionUrl())
			.setApp(interact.getAppUrl())
			.setFinish(interact.getServerNonce())
			.setUserCode(UserCodeResponse.of(interact))
			.setUserCodeUri(UserCodeUriResponse.of(interact));


	}

}
