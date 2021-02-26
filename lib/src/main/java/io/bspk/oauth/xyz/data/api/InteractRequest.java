package io.bspk.oauth.xyz.data.api;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.Interact.InteractStart;
import io.bspk.oauth.xyz.data.InteractFinish;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class InteractRequest {

	private InteractFinish finish;
	@JsonProperty("start") // we need to set this for Jackson because we overload the setter, below
	private Set<InteractStart> start;
	private InteractHintRequest hints;

	@Tolerate
	@JsonIgnore
	public InteractRequest setStart(InteractStart... start) {
		return setStart(Set.of(start));
	}

}
