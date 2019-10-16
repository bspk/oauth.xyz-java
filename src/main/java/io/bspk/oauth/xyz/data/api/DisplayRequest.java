package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DisplayRequest extends HandleReplaceable<DisplayRequest> {

	private String name;
	private String uri;
	private String logoUri;

}
