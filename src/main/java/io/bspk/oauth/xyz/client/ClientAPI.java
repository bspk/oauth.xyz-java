package io.bspk.oauth.xyz.client;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import io.bspk.oauth.xyz.data.Interact.Type;
import io.bspk.oauth.xyz.data.api.ClientRequest;
import io.bspk.oauth.xyz.data.api.InteractRequest;
import io.bspk.oauth.xyz.data.api.ResourceRequest;
import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
import io.bspk.oauth.xyz.data.api.UserRequest;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/client")
public class ClientAPI {

	@Value("${oauth.xyz.root}c/callback")
	private String clientBaseUrl;

	@Value("${oauth.xyz.root}api/as/transaction")
	private String asEndpoint;

	@PostMapping(path = "/authcode", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startAuthorizationCodeFlow(HttpSession session) {

		RestTemplate restTemplate = new RestTemplate();

		String callbackId = RandomStringUtils.randomAlphanumeric(30);

		String state = RandomStringUtils.randomAlphanumeric(20);

		TransactionRequest request = new TransactionRequest()
			.setClient(new ClientRequest())
			.setInteract(new InteractRequest()
				.setCallback(clientBaseUrl + "/" + callbackId)
				.setState(state)
				.setType(Type.REDIRECT))
			.setResource(new ResourceRequest())
			.setUser(new UserRequest());

		ResponseEntity<TransactionResponse> responseEntity = restTemplate.postForEntity(asEndpoint, request, TransactionResponse.class);

		return responseEntity;

	}

}
