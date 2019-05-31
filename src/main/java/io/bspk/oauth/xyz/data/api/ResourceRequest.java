package io.bspk.oauth.xyz.data.api;

import java.util.ArrayList;
import java.util.List;

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
public class ResourceRequest extends HandleReplaceable<ResourceRequest> {

	private List<String> actions = new ArrayList<>();
	private List<String> locations = new ArrayList<>();
	private List<String> data = new ArrayList<>();

}
