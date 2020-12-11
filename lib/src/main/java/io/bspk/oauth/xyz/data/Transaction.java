package io.bspk.oauth.xyz.data;

import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.api.ResourceRequest;
import io.bspk.oauth.xyz.data.api.SubjectRequest;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Transaction {

	public enum Status {

		NEW,		// newly created transaction, nothing's been done to it yet
		ISSUED,		// an access token has been issued
		AUTHORIZED,	// the user has authorized but a token has not been issued yet
		WAITING,	// we are waiting for the user
		DENIED; 	// the user denied the transaction

		@JsonCreator
		public static Status fromJson(String key) {
			return key == null ? null :
				valueOf(key.toUpperCase());
		}

		@JsonValue
		public String toJson() {
			return name().toLowerCase();
		}

	}

	private @Id String id;
	private Display display;
	private User user;
	private Interact interact;
	private String interactHandle;
	private AccessToken continueAccessToken;
	private AccessToken accessToken;
	private Map<String, AccessToken> multipleAccessTokens;
	private @NonNull Status status = Status.NEW;
	private Key key;
	private Subject subject;
	private SubjectRequest subjectRequest;
	private ResourceRequest resourceRequest;
	private Set<Capability> capabilitiesRequest;
	private Set<Capability> capabilities;

}
