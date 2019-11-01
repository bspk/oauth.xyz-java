package io.bspk.oauth.xyz.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.Module;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;

import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.api.TransactionRequest;

/**
 * @author jricher
 *
 */
@Service
public class SigningRestTemplates {

	@Autowired
	private Module jacksonModule;

	@Autowired
	private JWK clientKey;

	private RestTemplate noSigner;
	private RestTemplate cavageSigner;
	private RestTemplate dpopSigner;
	private RestTemplate detachedSigner;
	private RestTemplate oauthPopSigner;

	@PostConstruct
	public void init() {
		// create all the templates
		noSigner = createRestTemplate(List.of(
			new RequestResponseLoggingInterceptor()
			));
		cavageSigner = createRestTemplate(List.of(
			new DigestInterceptor(),
			new CavageSigningInterceptor(),
			new RequestResponseLoggingInterceptor()
			));
		dpopSigner = createRestTemplate(List.of(
			new DpopInterceptor(),
			new RequestResponseLoggingInterceptor()
			));
		detachedSigner = createRestTemplate(List.of(
			new DetachedJwsSigningInterceptor(),
			new RequestResponseLoggingInterceptor()
			));
		oauthPopSigner = createRestTemplate(List.of(
			new OAuthPoPSigningInterceptor(),
			new RequestResponseLoggingInterceptor()
			));
	}

	public RestTemplate getSignerFor(TransactionRequest req) {
		if (req.getKeys() != null) {
			switch (req.getKeys().getProof()) {
				case DPOP:
					return dpopSigner;
				case HTTPSIG:
					return cavageSigner;
				case JWSD:
					return detachedSigner;
				case OAUTHPOP:
					return oauthPopSigner;
				case MTLS:
				default:
					return noSigner;
			}
		} else {
			return noSigner;
		}
	}

	private RestTemplate createRestTemplate(List<ClientHttpRequestInterceptor> interceptors) {
		ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());

		RestTemplate restTemplate = new RestTemplate(factory);
		restTemplate.setInterceptors(interceptors);

		MappingJackson2HttpMessageConverter messageConverter = restTemplate.getMessageConverters().stream()
			.filter(MappingJackson2HttpMessageConverter.class::isInstance)
			.map(MappingJackson2HttpMessageConverter.class::cast)
			.findFirst().orElseThrow(() -> new RuntimeException("MappingJackson2HttpMessageConverter not found"));

		messageConverter.getObjectMapper().registerModule(jacksonModule);

		return restTemplate;
	}

	private static class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			logRequest(request, body);
			ClientHttpResponse response = execution.execute(request, body);
			logResponse(response);
			return response;
		}

		private void logRequest(HttpRequest request, byte[] body) throws IOException {
			log.info("<<<========================request begin================================================");
			log.info("URI         : {}", request.getURI());
			log.info("Method      : {}", request.getMethod());
			log.info("Headers     : {}", request.getHeaders());
			log.info("Request body: {}", new String(body, "UTF-8"));
			log.info("<<<=======================request end================================================");
		}

		private void logResponse(ClientHttpResponse response) throws IOException {
			log.info(">>>=========================response begin==========================================");
			log.info("Status code  : {}", response.getStatusCode());
			log.info("Status text  : {}", response.getStatusText());
			log.info("Headers      : {}", response.getHeaders());
			log.info("Response body: {}", StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
			log.info(">>>====================response end=================================================");
		}
	}

	private class DigestInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			if (body != null && body.length > 0) {
				// add the digest header
				/*
				log.info(IntStream.range(0, body.length)
					.map(idx -> Byte.toUnsignedInt(body[idx]))
					.mapToObj(i -> Integer.toHexString(i))
					.collect(Collectors.joining(", ")));
				*/
				String hash = Hash.SHA1_digest(body);
				request.getHeaders().add("Digest", "SHA=" + hash);
			}

			return execution.execute(request, body);
		}

	}

	private class DetachedJwsSigningInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(clientKey.getAlgorithm().getName()))
				.customParam("b64", true)
				.criticalParams(Set.of("b64"))
				.type(JOSEObjectType.JOSE)
				.keyID(clientKey.getKeyID())
				.build();

			Payload payload = new Payload(body);

			//log.info(">> " + payload.toBase64URL().toString());

			try {
				RSASSASigner signer = new RSASSASigner((RSAKey)clientKey);

				JWSObject jwsObject = new JWSObject(header, payload);

				jwsObject.sign(signer);

				String signature = jwsObject.serialize(true);

				request.getHeaders().add("Detached-JWS", signature);

			} catch (JOSEException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return execution.execute(request, body);

		}
	}

	private class CavageSigningInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			try {
				String alg = "rsa-sha256";

				// collect the base string
				Map<String, String> signatureBlock = new LinkedHashMap<>();
				String requestTarget = request.getMethodValue().toLowerCase() + " " + request.getURI().getRawPath();
				signatureBlock.put("(request-target)", requestTarget);

				List<String> headersToSign = List.of("Host", "Date", "Digest", "Content-length");

				headersToSign.forEach((h) -> {
					if (request.getHeaders().getFirst(h) != null) {
						signatureBlock.put(h.toLowerCase(), request.getHeaders().getFirst(h));
					}
				});

				String input = signatureBlock.entrySet().stream()
					.map(e -> e.getKey().strip().toLowerCase() + ": " + e.getValue().strip())
					.collect(Collectors.joining("\n"));

				RSAKey rsaKey = (RSAKey)clientKey;

				Signature signature = Signature.getInstance("SHA256withRSA");

				signature.initSign(rsaKey.toPrivateKey());
				signature.update(input.getBytes("UTF-8"));
		        byte[] s = signature.sign();

		        String encoded = Base64.getEncoder().encodeToString(s);

		        String headers = signatureBlock.keySet().stream()
		        	.map(String::toLowerCase)
		        	.collect(Collectors.joining(" "));

		        Map<String, String> signatureHeader = new LinkedHashMap<>();

		        signatureHeader.put("keyId", clientKey.getKeyID());
		        signatureHeader.put("algorithm", alg);
		        signatureHeader.put("headers", headers);
		        signatureHeader.put("signature", encoded);


		        String signatureHeaderPayload = signatureHeader.entrySet()
		        	.stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"") // TODO: the value should likely be encoded
		        	.collect(Collectors.joining(","));

		        request.getHeaders().add("Signature", signatureHeaderPayload);

			} catch (NoSuchAlgorithmException | InvalidKeyException | JOSEException | SignatureException e) {
				throw new RuntimeException(e);
			}

			return execution.execute(request, body);
		}

	}

	private class DpopInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(clientKey.getAlgorithm().getName()))
				.type(new JOSEObjectType("dpop+jwt"))
				.jwk(clientKey.toPublicJWK())
				.build();

			JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.jwtID(RandomStringUtils.randomAlphanumeric(20))
				.issueTime(Date.from(Instant.now()))
				.claim("http_method", request.getMethodValue())
				.claim("http_uri", request.getURI().toString())
				.build();

			try {
				RSASSASigner signer = new RSASSASigner((RSAKey)clientKey);

				JWSObject jwsObject = new JWSObject(header, new Payload(claims.toJSONObject()));

				jwsObject.sign(signer);

				String signature = jwsObject.serialize();

				request.getHeaders().add("DPoP", signature);

			} catch (JOSEException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return execution.execute(request, body);
		}

	}

	private class OAuthPoPSigningInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(clientKey.getAlgorithm().getName()))
				.jwk(clientKey.toPublicJWK())
				.build();

			Builder claimsSetBuilder = new JWTClaimsSet.Builder();

			String bodyHash = Hash.SHA256_encode(body);
			claimsSetBuilder.claim("b", bodyHash);

			calculateQueryHash(request, claimsSetBuilder);
			calculateHeaderHash(request, claimsSetBuilder);

			claimsSetBuilder.claim("m", request.getMethod().toString().toUpperCase());
			claimsSetBuilder.claim("u", request.getURI().getHost());
			claimsSetBuilder.claim("p", request.getURI().getPath());
			claimsSetBuilder.claim("ts", Instant.now().getEpochSecond());

			try {
				RSASSASigner signer = new RSASSASigner((RSAKey)clientKey);

				JWSObject jwsObject = new JWSObject(header, new Payload(claimsSetBuilder.build().toJSONObject()));

				jwsObject.sign(signer);

				String signature = jwsObject.serialize();

				request.getHeaders().add("PoP", signature);

			} catch (JOSEException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			return execution.execute(request, body);
		}

		private void calculateQueryHash(HttpRequest request, Builder claimsSetBuilder) {
			String query = request.getURI().getQuery();
			if (!Strings.isNullOrEmpty(query)) {
				MultiValueMap<String,String> queryParams = UriComponentsBuilder.newInstance().replaceQuery(query).build().getQueryParams();

				List<String> names = new ArrayList<>();
				List<String> hashBase = new ArrayList<>();

				queryParams.entrySet().forEach((e) -> {
					names.add(e.getKey());
					String first = e.getValue().get(0); // TODO: make sure other values don't exist
					hashBase.add(UriUtils.encodeQueryParam(e.getKey(), Charset.defaultCharset())
						+ "="
						+ UriUtils.encodeQueryParam(first, Charset.defaultCharset()));
				});

				String hash = Hash.SHA256_encode(Joiner.on("&").join(hashBase).getBytes());

				claimsSetBuilder.claim("q", List.of(
					names,
					hash));
			}
		}

		private void calculateHeaderHash(HttpRequest request, Builder claimsSetBuilder) {
			if (request.getHeaders() != null) {
				List<String> names = new ArrayList<>();
				List<String> hashBase = new ArrayList<>();

				request.getHeaders().entrySet().forEach((e) -> {
					names.add(e.getKey());
					String first = e.getValue().get(0); // TODO: make sure other values don't exist
					hashBase.add(e.getKey().toLowerCase()
						+ ": "
						+ first);
				});

				String hash = Hash.SHA256_encode(Joiner.on("\n").join(hashBase).getBytes());

				claimsSetBuilder.claim("h", List.of(
					names,
					hash));
			}
		}
	}

}
