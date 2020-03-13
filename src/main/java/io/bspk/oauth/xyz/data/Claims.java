package io.bspk.oauth.xyz.data;

import java.time.Instant;

import org.apache.commons.lang3.BooleanUtils;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nimbusds.jwt.JWT;

import io.bspk.oauth.xyz.data.api.ClaimsRequest;
import io.bspk.oauth.xyz.json.JWTDeserializer;
import io.bspk.oauth.xyz.json.JWTSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Claims {

	private String subject;
	private String email;
	private String phone;
	@JsonSerialize(using = JWTSerializer.class)
	@JsonDeserialize(using = JWTDeserializer.class)
	private JWT oidcIdToken;
	private Instant updatedAt;


	public static Claims of(ClaimsRequest request, User user) {
		Claims c = new Claims();
		if (BooleanUtils.isTrue(request.getSubject())) {
			c.setSubject(user.getId());
		}
		if (BooleanUtils.isTrue(request.getEmail())) {
			c.setEmail(user.getEmail());
		}
		if (BooleanUtils.isTrue(request.getPhone())) {
			c.setPhone(user.getPhone());
		}
		if (BooleanUtils.isTrue(request.getOidcIdToken())) {
			c.setOidcIdToken(user.getIdToken());
		}
		c.setUpdatedAt(user.getUpdatedAt());
		return c;
	}

}
