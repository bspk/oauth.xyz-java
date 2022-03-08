package io.bspk.oauth.xyz.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.api.DisplayRequest;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Display {

	private String name;
	private String uri;
	private String logoUri;

	public static Display of(DisplayRequest displayRequest) {
		if (displayRequest == null) {
			return null;
		}
		return new Display()
			.setName(displayRequest.getName())
			.setUri(displayRequest.getUri())
			.setLogoUri(displayRequest.getLogoUri());
	}

}
