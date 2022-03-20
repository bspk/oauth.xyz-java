package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.json.HandleAwareFieldDeserializer;
import io.bspk.oauth.xyz.json.HandleAwareFieldSerializer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonSerialize(using =  HandleAwareFieldSerializer.class)
@JsonDeserialize(using = HandleAwareFieldDeserializer.class)
public final class HandleAwareField<T> {

	@Setter(AccessLevel.PRIVATE)
	private boolean handled;

	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private String handle;

	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private T data;

	public String asHandle() {
		if (isHandled()) {
			return handle;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public T asValue() {
		if (isHandled()) {
			throw new IllegalArgumentException();
		} else {
			return data;
		}
	}

	public TypeReference<?> getType() {
		return new TypeReference<HandleAwareField<T>>() { };
	}

	public static <S> HandleAwareField<S> of (String handle) {
		return new HandleAwareField<S>()
			.setHandle(handle)
			.setHandled(true);
	}

	public static <S> HandleAwareField<S> of (S data) {
		// avoid double-wrapping that Jackson can sometimes try to do
		if (data instanceof HandleAwareField) {
			return (HandleAwareField<S>) data;
		} else {
			return new HandleAwareField<S>()
				.setData(data)
				.setHandled(false);
		}
	}


}
