package io.bspk.oauth.xyz.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.api.ClientRequest;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Client {

	private String name;
	private String uri;
	private String logoUri;

	public static Client of(ClientRequest clientRequest) {
		return new Client()
			.setName(clientRequest.getName())
			.setUri(clientRequest.getUri())
			.setLogoUri(clientRequest.getLogoUri());
	}

}
