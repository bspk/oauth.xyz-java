package io.bspk.oauth.xyz.data;

import org.springframework.data.annotation.Id;

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

	private @Id String instanceId;
	private Key key;
	private Display display;


	public static Client of(ClientRequest req) {
		return new Client()
			.setKey(Key.of(req.getKey()))
			.setDisplay(Display.of(req.getDisplay()));
	}


}
