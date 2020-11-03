package io.bspk.oauth.xyz.data.api;

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
public class UserCodeResponse {

	private String url;
	private String code;
	/**
	 * @param interact
	 * @return
	 */
	public static UserCodeResponse of(Interact interact) {
		if (interact == null || Strings.isNullOrEmpty(interact.getUserCode())) {
			return null;
		}

		return new UserCodeResponse()
			.setCode(interact.getUserCode())
			.setUrl(interact.getUserCodeUrl());

	}



}
