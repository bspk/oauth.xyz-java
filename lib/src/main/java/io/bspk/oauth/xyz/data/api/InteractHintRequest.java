package io.bspk.oauth.xyz.data.api;

import java.util.Locale;
import java.util.Set;

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
public class InteractHintRequest {

	public Set<Locale> uiLocales;

}
