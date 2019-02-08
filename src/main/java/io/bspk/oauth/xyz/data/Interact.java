package io.bspk.oauth.xyz.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.bspk.oauth.xyz.data.api.InteractRequest;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
public class Interact {

	public enum Type {
		REDIRECT;

		@JsonCreator
		public static Type fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}

	}

	private Type type;
	private String url;
	private String interactId;
	private String callback;
	private String state;
	private String interactHandle;

	/**
	 * @param interact
	 * @return
	 */
	public static Interact of(InteractRequest interact) {
		return new Interact()
			.setCallback(interact.getCallback())
			.setType(interact.getType())
			.setState(interact.getState());
	}

}
