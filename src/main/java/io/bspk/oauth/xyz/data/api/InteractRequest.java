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
public class InteractRequest extends HandleReplaceable<InteractRequest> {

	private Callback callback;
	private Boolean redirect;
	private Boolean userCode;
	private Boolean didComm;
	private Boolean didCommQuery;

	@Data
	@Accessors(chain = true)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public static class Callback {
		private String uri;
		private String nonce;
	}

}
