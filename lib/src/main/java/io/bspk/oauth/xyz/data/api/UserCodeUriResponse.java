package io.bspk.oauth.xyz.data.api;

import java.net.URI;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.base.Strings;

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
public class UserCodeUriResponse {

	private String code;
	private URI uri;
	/**
	 * @param interact
	 * @return
	 */
	public static UserCodeUriResponse of(Interact interact) {
		if (interact == null || Strings.isNullOrEmpty(interact.getUserCode())) {
			return null;
		}

		return new UserCodeUriResponse()
			.setCode(interact.getUserCode())
			.setUri(interact.getUserCodeUrl());

	}



}
