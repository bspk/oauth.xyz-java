package io.bspk.oauth.xyz.data;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.crypto.Hash.HashMethod;
import io.bspk.httpsig.HttpSigAlgorithm;
import io.bspk.oauth.xyz.crypto.KeyProofParameters;
import io.bspk.oauth.xyz.data.api.AccessTokenRequest.TokenFlag;
import io.bspk.oauth.xyz.data.api.AccessTokenResponse;
import io.bspk.oauth.xyz.data.api.InteractResponse;
import io.bspk.oauth.xyz.data.api.TransactionContinueRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
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
	private KeyProofParameters proofParams;
	private String keyHandle;
	private URI continueUri;
	private String continueToken;
	private String accessToken;
	private KeyProofParameters accessTokenProofParams;
	private String rsResponse;

	private String standaloneUserCode;
	private String userCode;
	private URI userCodeUrl;
	private URI interactionUrl;
	private Map<String, String> multipleAccessTokens;
	private Map<String, KeyProofParameters> multipleAccessTokenProofParams;
	private Map<String, String> multipleRsResponse;
	@NonNull
	private final URI grantEndpoint;
	private Instant createdAt;
	private Subject subjectInfo;

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

				// set the key if an explicit one is given, otherwise keep what we have
				if (response.getCont().getAccessToken().getKey() != null) {
					KeyProofParameters params = new KeyProofParameters()
						.setSigningKey(response.getCont().getAccessToken().getKey().getJwk())
						.setProof(response.getCont().getAccessToken().getKey().getProof())
						// FIXME: these should be parameters, probably under "proof"
						.setDigestAlgorithm("sha-512")
						.setHttpSigAlgorithm(HttpSigAlgorithm.RSAPSS);


					setProofParams(params);
				}
			}

			setContinueUri(response.getCont().getUri());
		} else {
			// otherwise clear it out
			setContinueToken(null);
			setContinueUri(null);
			setProofParams(null);
		}

		if (response.getAccessToken() != null) {
			if (response.getAccessToken().isMultiple()) {
				List<AccessTokenResponse> tokenResponses = response.getAccessToken().asMultiple();

				setMultipleAccessTokens(tokenResponses.stream()
					.collect(Collectors.toMap(
						t -> t.getLabel(),
						t -> t.getValue())));

				setMultipleAccessTokenProofParams(tokenResponses.stream()
					.collect(Collectors.toMap(
						t -> t.getLabel(),
						t -> {
							if (t.getFlags() != null && !t.getFlags().contains(TokenFlag.BEARER)) {
								if (t.getKey() == null) {
									// the token is bound but there's no other key, use the client's key
									return getProofParams();
								} else {
									KeyProofParameters params = new KeyProofParameters()
										.setSigningKey(t.getKey().getJwk())
										.setProof(t.getKey().getProof())
										// FIXME: these should be parameters, probably under "proof"
										.setDigestAlgorithm("sha-512")
										.setHttpSigAlgorithm(HttpSigAlgorithm.RSAPSS);
									return params;
								}
							} else {
								return null;
							}
						}
						)));
			} else {
				AccessTokenResponse tokenResponse = response.getAccessToken().asSingle();
				setAccessToken(tokenResponse.getValue());
				if (tokenResponse.getFlags() != null && !tokenResponse.getFlags().contains(TokenFlag.BEARER)) {
					if (tokenResponse.getKey() != null) {
						KeyProofParameters params = new KeyProofParameters()
							.setSigningKey(tokenResponse.getKey().getJwk())
							.setProof(tokenResponse.getKey().getProof())
							// FIXME: these should be parameters, probably under "proof"
							.setDigestAlgorithm("sha-512")
							.setHttpSigAlgorithm(HttpSigAlgorithm.RSAPSS);
						setAccessTokenProofParams(params);
					} else {
						// otherwise set it to the token used on request
						setAccessTokenProofParams(getProofParams());
					}
				}
			}
		}

		if (response.getInteract() != null) {
			InteractResponse interact = response.getInteract();
			if (interact.getUserCode() != null) {
				setStandaloneUserCode(interact.getUserCode().getCode());
			}
			if (interact.getUserCodeUri() != null) {
				setUserCode(interact.getUserCodeUri().getCode());
				setUserCodeUrl(interact.getUserCodeUri().getUri());
			}
			if (interact.getRedirect() != null) {
				setInteractionUrl(interact.getRedirect());
			}

			if (interact.getFinish() != null) {
				setServerNonce(interact.getFinish());
			}
		} else {
			setStandaloneUserCode(null);
			setUserCode(null);
			setUserCodeUrl(null);
			setInteractionUrl(null);
			setServerNonce(null);
		}

		if (response.getSubject() != null) {
			setSubjectInfo(response.getSubject());
		}

		return this;
	}

	public PendingTransaction setMultipleRsResponse(String tokenId, String response) {
		Map<String, String> mrr = getMultipleRsResponse();
		if (mrr == null) {
			mrr = new HashMap<>();
		}
		mrr.put(tokenId, response);
		setMultipleRsResponse(mrr);
		return this;
	}

}
