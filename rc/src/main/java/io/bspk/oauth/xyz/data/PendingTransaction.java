package io.bspk.oauth.xyz.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.crypto.Hash.HashMethod;
import io.bspk.oauth.xyz.data.api.TransactionContinueRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PendingTransaction {

	@Data
	@Accessors(chain = true)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	public class Entry {
		private @Id String id = new ObjectId().toHexString();
		private TransactionRequest request;
		private TransactionContinueRequest cont;
		private TransactionResponse response;
	}

	private @Id String id;
	private List<Entry> entries = new ArrayList<>();
	private String owner;
	private String callbackId;
	private String clientNonce;
	private String serverNonce;
	private HashMethod hashMethod;
	private Key key;
	private String keyHandle;
	private String continueUri;
	private String continueToken;
	private String accessToken;
	private String userCode;
	private String userCodeUrl;
	private String interactionUrl;
	private Map<String, String> multipleAccessTokens;

	public PendingTransaction add (TransactionResponse response) {
		entries.add(new Entry().setResponse(response));

		return processResponse(response);
	}

	public PendingTransaction add (TransactionContinueRequest request, TransactionResponse response) {
		entries.add(new Entry().setCont(request).setResponse(response));

		return processResponse(response);
	}

	public PendingTransaction add (TransactionRequest request, TransactionResponse response) {
		entries.add(new Entry().setRequest(request).setResponse(response));

		return processResponse(response);
	}

	private PendingTransaction processResponse(TransactionResponse response) {
		if (response.getCont() != null) {
			// if there's a continuation section, update the values given
			if (response.getCont().getAccessToken() != null) {
				setContinueToken(response.getCont().getAccessToken().getValue());

				// set the key if an explicit one is given
				if (!response.getCont().getAccessToken().getKey().isClientKey()) {
					setKey(response.getCont().getAccessToken().getKey().getKey());
				}
			}

			setContinueUri(response.getCont().getUri());
		} else {
			// otherwise clear it out
			setContinueToken(null);
			setContinueUri(null);
			setKey(null);
		}

		if (response.getAccessToken() != null) {
			setAccessToken(response.getAccessToken().getValue());
		} else if (response.getMultipleAccessTokens() != null) {
			Map<String, String> tokens =
				response.getMultipleAccessTokens().entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getKey,
						e -> e.getValue().getValue()));
			setMultipleAccessTokens(tokens);
		}

		if (response.getUserCode() != null) {
			setUserCode(response.getUserCode());
			setUserCodeUrl(response.getUserCodeUrl());
		}

		if (response.getInteractionUrl() != null) {
			setInteractionUrl(response.getInteractionUrl());
		}

		if (response.getCallbackServerNonce() != null) {
			setServerNonce(response.getCallbackServerNonce());
		}

		return this;
	}

}
