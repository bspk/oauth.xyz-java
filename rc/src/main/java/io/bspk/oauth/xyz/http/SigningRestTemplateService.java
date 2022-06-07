package io.bspk.oauth.xyz.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.greenbytes.http.sfv.ByteSequenceItem;
import org.greenbytes.http.sfv.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;

import io.bspk.httpsig.ComponentProvider;
import io.bspk.httpsig.HttpSign;
import io.bspk.httpsig.MessageWrapper;
import io.bspk.httpsig.SignatureBaseBuilder;
import io.bspk.httpsig.SignatureParameters;
import io.bspk.httpsig.spring.RestTemplateMessageWrapper;
import io.bspk.httpsig.spring.RestTemplateRequestProvider;
import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.crypto.KeyProofParameters;
import io.bspk.oauth.xyz.data.Key.Proof;

/**
 * @author jricher
 *
 */
@Service
public class SigningRestTemplateService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory());

	public RestTemplate getSignerFor(KeyProofParameters params) {
		return getSignerFor(params, null);
	}

	public RestTemplate getSignerFor(KeyProofParameters params, String accessTokenValue) {

		if (params.getSigningKey() == null || params.getProof() == null) {
			return createRestTemplate(List.of(
				new AccessTokenInjectingInterceptor(null, accessTokenValue),
				new RequestResponseLoggingInterceptor()
			));
		}

		Proof proof = params.getProof();

		if (proof == null) {
			throw new IllegalArgumentException("Key proof must not be null.");
		}

		switch (proof) {
			case JWS:
			case JWSD:
				return createRestTemplate(List.of(
					new AccessTokenInjectingInterceptor(params, accessTokenValue),
					new JwsSigningInterceptor(params, accessTokenValue),
					new RequestResponseLoggingInterceptor()
				));
			case HTTPSIG:
				return createRestTemplate(List.of(
					new ContentDigestInterceptor(params.getDigestAlgorithm()),
					new AccessTokenInjectingInterceptor(params, accessTokenValue),
					new HttpMessageSigningInterceptor(params, accessTokenValue),
					new RequestResponseLoggingInterceptor()
				));
			case MTLS:
			default:
				return createRestTemplate(List.of(
					new RequestResponseLoggingInterceptor()
				));
		}
	}

	private RestTemplate createRestTemplate(List<ClientHttpRequestInterceptor> interceptors) {
		RestTemplate restTemplate = new RestTemplate(factory);
		restTemplate.setInterceptors(interceptors);

		// set up Jackson
		MappingJackson2HttpMessageConverter messageConverter = restTemplate.getMessageConverters().stream()
			.filter(MappingJackson2HttpMessageConverter.class::isInstance)
			.map(MappingJackson2HttpMessageConverter.class::cast)
			.findFirst().orElseThrow(() -> new RuntimeException("MappingJackson2HttpMessageConverter not found"));

		messageConverter.getObjectMapper().setSerializationInclusion(Include.NON_NULL);

		return restTemplate;
	}

	private static class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		private final ObjectMapper mapper = new ObjectMapper();

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			logRequest(request, body);
			ClientHttpResponse response = execution.execute(request, body);
			logResponse(response);
			return response;
		}

		private void logRequest(HttpRequest request, byte[] body) throws IOException {
			log.info("<<<========================request begin================================================");
			log.info("<<< URI         : {}", request.getURI());
			log.info("<<< Method      : {}", request.getMethod());
			log.info("<<< Headers     : {}", request.getHeaders());
			log.info("<<< Request body: {}", new String(body, "UTF-8"));
			if (MediaType.APPLICATION_JSON.equals(request.getHeaders().getContentType())) {
				// pretty print
				JsonNode node = mapper.readTree(body);
				String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
				log.info("<<< Pretty  :\n <<< : {}", Joiner.on("\n<<< : ").join(Splitter.on("\n").split(pretty)));
			}
			log.info("<<<=======================request end================================================");
		}

		private void logResponse(ClientHttpResponse response) throws IOException {
			String bodyAsString = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());

			log.info(">>>=========================response begin==========================================");
			log.info(">>> Status code  : {}", response.getStatusCode());
			log.info(">>> Status text  : {}", response.getStatusText());
			log.info(">>> Headers      : {}", response.getHeaders());
			log.info(">>> Response body: {}", bodyAsString);
			if (MediaType.APPLICATION_JSON.equals(response.getHeaders().getContentType())) {
				// pretty print
				JsonNode node = mapper.readTree(bodyAsString);
				String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
				log.info(">>> Pretty  :\n >>> : {}", Joiner.on("\n>>> : ").join(Splitter.on("\n").split(pretty)));
			}
			log.info(">>>====================response end=================================================");
		}
	}

	private abstract class KeyAndTokenAwareInterceptor {
		private final String accessTokenValue;
		private final KeyProofParameters params;

		public KeyAndTokenAwareInterceptor(KeyProofParameters params, String accessTokenValue) {
			// Either field may be null for different request types
			//
			//         | token        | no token         |
			//  key    | bound token  | signed request   |
			//  no key | bearer token | unsigned request |
			//
			this.accessTokenValue = accessTokenValue;
			this.params = params;
		}

		public String getAccessTokenValue() {
			return accessTokenValue;
		}
		public KeyProofParameters getParams() {
			return params;
		}

		public boolean hasAccessToken() {
			return !Strings.isNullOrEmpty(accessTokenValue);
		}

		public boolean isBearerToken() {
			if (params == null || params.getSigningKey() == null || params.getProof() == null) {
				return true;
			} else {
				return false;
			}
		}
	}

	private class AccessTokenInjectingInterceptor extends KeyAndTokenAwareInterceptor implements ClientHttpRequestInterceptor {
		public AccessTokenInjectingInterceptor(KeyProofParameters params, String accessTokenValue) {
			super(params, accessTokenValue);
			// TODO Auto-generated constructor stub
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			if (!Strings.isNullOrEmpty(getAccessTokenValue())) {
				// if there's a token, inject it into the authorization header, otherwise just bypass
				if (isBearerToken()) {
					// RFC6750 style
					request.getHeaders().add("Authorization", "Bearer " + getAccessTokenValue());
				} else {
					// GNAP style
					request.getHeaders().add("Authorization", "GNAP " + getAccessTokenValue());
				}
			}
			return execution.execute(request, body);
		}
	}

	private class ContentDigestInterceptor implements ClientHttpRequestInterceptor {
		private final Logger log = LoggerFactory.getLogger(this.getClass());
		private String digestMethod;

		public ContentDigestInterceptor(String digestMethod) {
			if (Strings.isNullOrEmpty(digestMethod)) {
				digestMethod = "sha-256";
			}
			this.digestMethod = digestMethod;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			if (body != null && body.length > 0) {

				MessageDigest sha = null;
				if ("sha-512".equals(digestMethod)) {
					sha = new SHA512.Digest();
				} else if ("sha-256".equals(digestMethod)) {
					sha = new SHA256.Digest();
				}

				if (sha == null) {
					throw new RuntimeException("unknown digest algorithm: " + digestMethod);
				}

				byte[] digest = sha.digest(body);

				ByteSequenceItem seq = ByteSequenceItem.valueOf(digest);

				log.info("^^ Content Digest: " + seq.serialize());

				Dictionary dict = Dictionary.valueOf(Map.of(
					digestMethod, seq
					));

				request.getHeaders().add("Content-Digest", dict.serialize());

			}

			return execution.execute(request, body);
		}
	}

	// this handles both the detached and attached versions
	private class JwsSigningInterceptor extends KeyAndTokenAwareInterceptor implements ClientHttpRequestInterceptor {

		public JwsSigningInterceptor(KeyProofParameters params, String accessTokenValue) {
			super(params, accessTokenValue);
		}

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			// are we doing attached or not?
			boolean attached = (getParams().getProof() == Proof.JWS &&
				(request.getMethod() == HttpMethod.PUT ||
				request.getMethod() == HttpMethod.PATCH ||
				request.getMethod() == HttpMethod.POST));

			JWK clientKey = getParams().getSigningKey();

			JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.parse(clientKey.getAlgorithm().getName()))
				.type(new JOSEObjectType("gnap-binding+jwsd"))
				.keyID(clientKey.getKeyID())
				.customParam("htm", request.getMethod().toString())
				.customParam("uri", request.getURI().toString());

			// cover the access token if it exists
			if (hasAccessToken()) {
				headerBuilder.customParam("ath", Hash.SHA256_encode_url(getAccessTokenValue().getBytes()).toString());
			}

			JWSHeader header = headerBuilder
				.build();

			Payload payload;
			if (body == null || body.length == 0) {
				// if the body is empty, use an empty payload
				payload = new Payload(new byte[0]);
			} else if (!attached) {
				// if the body's not empty, the payload is a hash of the body
				payload = new Payload(Hash.SHA256_encode_url(body));
			} else {
				// the body's not empty and it's an attached request, just use the body as the payload
				payload = new Payload(body);
			}

			//log.info(">> " + payload.toBase64URL().toString());

			try {
				JWSSigner signer = new DefaultJWSSignerFactory().createJWSSigner(clientKey);

				JWSObject jwsObject = new JWSObject(header, payload);

				jwsObject.sign(signer);

				String signature = jwsObject.serialize();

				if (attached) {

					// if we're doing attached JWS and there is a body to the request, replace the body and make the content type JOSE
					request.getHeaders().setContentType(new MediaType("application", "jose"));

					byte[] newBody = jwsObject.serialize().getBytes();

					return execution.execute(request, newBody);
				} else {
					// if we're doing detached JWS or if we're doing attached JWS and there is no body, put the results in the header

					request.getHeaders().add("Detached-JWS", signature);
				}

			} catch (JOSEException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return execution.execute(request, body);

		}
	}

	private class HttpMessageSigningInterceptor  extends KeyAndTokenAwareInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		public HttpMessageSigningInterceptor(KeyProofParameters params, String accessTokenValue) {
			super(params, accessTokenValue);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			SignatureParameters sigParams = new SignatureParameters()
				.setCreated(Instant.now())
				.setKeyid(getParams().getSigningKey().getKeyID())
				.setNonce(RandomStringUtils.randomAlphanumeric(13));

			if (getParams().getHttpSigAlgorithm() != null) {
				sigParams.setAlg(getParams().getHttpSigAlgorithm());
			}

			sigParams.addComponentIdentifier("@target-uri");
			sigParams.addComponentIdentifier("@method");

			if (request.getMethod() == HttpMethod.PUT ||
				request.getMethod() == HttpMethod.PATCH ||
				request.getMethod() == HttpMethod.POST) {
				if (body != null && body.length > 0) {
					sigParams.addComponentIdentifier("Content-Length");
					sigParams.addComponentIdentifier("Content-Type");
					sigParams.addComponentIdentifier("Content-Digest");
				}
			}

			if (hasAccessToken()) {
				sigParams.addComponentIdentifier("Authorization");
			}

			ComponentProvider ctx = new RestTemplateRequestProvider(request);

			SignatureBaseBuilder baseBuilder = new SignatureBaseBuilder(sigParams, ctx);

			byte[] baseBytes = baseBuilder.createSignatureBase();

			// holder for signed bytes

			HttpSign httpSign = new HttpSign(getParams().getHttpSigAlgorithm(), getParams().getSigningKey());

			byte[] s = httpSign.sign(baseBytes);

			if (s == null) {
				throw new RuntimeException("Could not sign message.");
			}

			MessageWrapper wrapper = new RestTemplateMessageWrapper(request);
			wrapper.addSignature(sigParams, s);


			return execution.execute(request, body);
		}

	}
}
