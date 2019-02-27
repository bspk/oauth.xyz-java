package io.bspk.oauth.xyz.authserver.data.api;

import java.net.URI;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ApprovalResponse {

	private URI uri;
	private boolean approved;

}
