package io.bspk.oauth.xyz.data;

import org.apache.commons.lang3.BooleanUtils;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.api.ClaimsRequest;
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
		return c;
	}

}
