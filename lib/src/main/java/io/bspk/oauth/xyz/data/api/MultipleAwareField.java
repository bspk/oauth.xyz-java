package io.bspk.oauth.xyz.data.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.json.MultipleAwareFieldDeserializer;
import io.bspk.oauth.xyz.json.MultipleAwareFieldSerializer;
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
@JsonSerialize(using =  MultipleAwareFieldSerializer.class)
@JsonDeserialize(using = MultipleAwareFieldDeserializer.class)
public class MultipleAwareField<T> {

	@Setter(AccessLevel.PRIVATE)
	private boolean multiple;

	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private List<T> data = new ArrayList<>();

	public T asSingle() {
		if (isMultiple()) {
			throw new IllegalArgumentException();
		} else {
			return data.get(0);
		}
	}

	public List<T> asMultiple() {
		if (isMultiple()) {
			return data;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public static <S> MultipleAwareField<S> of(S singleton) {
		MultipleAwareField<S> f = new MultipleAwareField<S>()
			.setMultiple(false);

		f.getData().add(singleton);

		return f;
	}

	public static <S> MultipleAwareField<S> of(List<S> items) {
		MultipleAwareField<S> f = new MultipleAwareField<S>()
			.setMultiple(true);

		f.getData().addAll(items);

		return f;
	}

	public static <S> MultipleAwareField<S> of(S... items) {
		return of(List.of(items));
	}
}
