package io.bspk.oauth.xyz.data.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
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
public class RequestedResource {

	private String type;
	private List<String> actions = new ArrayList<>();
	private List<String> locations = new ArrayList<>();
	private List<String> datatypes = new ArrayList<>();
	private List<String> privileges = new ArrayList<>();
    private Map<String, Object> other = new HashMap<>();

    @JsonAnySetter
    public void addOther(String key, Object val) {
    	other.put(key, val);
    }

    @JsonAnyGetter
    public Map<String, Object> getOther() {
        return new LinkedHashMap<>(other); //return copy
    }

}
