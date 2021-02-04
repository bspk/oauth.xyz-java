package io.bspk.oauth.xyz.data.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.bspk.oauth.xyz.json.HandleAwareFieldDeserializer;
import io.bspk.oauth.xyz.json.HandleAwareFieldSerializer;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ClientRequest {

	@JsonSerialize(using =  HandleAwareFieldSerializer.class)
	@JsonDeserialize(using = HandleAwareFieldDeserializer.class)
	private HandleAwareField<KeyRequest> key;
	private DisplayRequest display;

	@Tolerate
	@JsonIgnore
	public ClientRequest setKey(KeyRequest client) {
		return setKey(HandleAwareField.of(client));
	}

	@Tolerate
	@JsonIgnore
	public ClientRequest setKey(String client) {
		return setKey(HandleAwareField.of(client));
	}


}
