package io.bspk.oauth.xyz.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nimbusds.jwt.JWT;

import io.bspk.oauth.xyz.json.JWTDeserializer;
import io.bspk.oauth.xyz.json.JWTSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AssertionSet {
	@JsonSerialize(using = JWTSerializer.class)
	@JsonDeserialize(using = JWTDeserializer.class)
	private JWT oidcIdToken;
}
