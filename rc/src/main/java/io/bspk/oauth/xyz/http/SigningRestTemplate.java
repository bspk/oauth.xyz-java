package io.bspk.oauth.xyz.http;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
public class SigningRestTemplate {

	private RestTemplate restTemplate;


	public TransactionResponse createTransaction(String asEndpoint, TransactionRequest request) {
		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		return response;
	}

	public TransactionResponse continueTransaction(String asEndpoint, TransactionRequest request, String accessToken) {

		HttpEntity<?> entity = addAccessToken(request, accessToken);

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.exchange(asEndpoint, HttpMethod.POST, entity, TransactionResponse.class);

		TransactionResponse response = responseEntity.getBody();

		return response;
	}

	private HttpEntity<?> addAccessToken(Object body, String accessToken) {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("Authorization", "GNAP " + accessToken);

		HttpEntity<?> entity = new HttpEntity<>(body, headers);
		return entity;
	}

}
