package io.bspk.oauth.xyz.http;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.greenbytes.http.sfv.ByteSequenceItem;
import org.greenbytes.http.sfv.Dictionary;
import org.greenbytes.http.sfv.InnerList;
import org.greenbytes.http.sfv.IntegerItem;
import org.greenbytes.http.sfv.Item;
import org.greenbytes.http.sfv.Parameters;
import org.greenbytes.http.sfv.StringItem;
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
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;

import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.Key;
import io.bspk.oauth.xyz.data.Key.Proof;

/**
 * @author jricher
 *
 */
@Service
public class SigningRestTemplateService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory());

	// TODO: cache and memoize this per key/proof and access token
	public RestTemplate getSignerFor(Key key, String accessTokenValue) {

		if (key == null) {
			return createRestTemplate(List.of(
				new AccessTokenInjectingInterceptor(key, accessTokenValue),
				new RequestResponseLoggingInterceptor()
			));
		}

		Proof proof = key.getProof();

		if (proof == null) {
			throw new IllegalArgumentException("Key proof must not be null.");
		}

		switch (proof) {
			case JWS:
			case JWSD:
				return createRestTemplate(List.of(
					new AccessTokenInjectingInterceptor(key, accessTokenValue),
					new JwsSigningInterceptor(key, accessTokenValue),
					new RequestResponseLoggingInterceptor()
				));
			case DPOP:
				return createRestTemplate(List.of(
					new AccessTokenInjectingInterceptor(key, accessTokenValue),
					new DpopInterceptor(key, accessTokenValue),
					new RequestResponseLoggingInterceptor()
				));
			case HTTPSIG:
				return createRestTemplate(List.of(
					new ContentDigestInterceptor(),
					new AccessTokenInjectingInterceptor(key, accessTokenValue),
					new HttpMessageSigningInterceptor(key, accessTokenValue),
					new RequestResponseLoggingInterceptor()
				));
			case OAUTHPOP:
				return createRestTemplate(List.of(
					new OAuthPoPSigningInterceptor(key, accessTokenValue),
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
		private final Key key;

		public KeyAndTokenAwareInterceptor(Key key, String accessTokenValue) {
			// Either field may be null for different request types
			//
			//         | token        | no token         |
			//  key    | bound token  | signed request   |
			//  no key | bearer token | unsigned request |
			//
			this.accessTokenValue = accessTokenValue;
			this.key = key;
		}

		public String getAccessTokenValue() {
			return accessTokenValue;
		}

		public Key getKey() {
			return key;
		}
		public boolean hasAccessToken() {
			return !Strings.isNullOrEmpty(accessTokenValue);
		}

		public boolean isBearerToken() {
			if (key == null || key.getProof() == null) {
				return true;
			} else {
				return false;
			}
		}
	}

	private class AccessTokenInjectingInterceptor extends KeyAndTokenAwareInterceptor implements ClientHttpRequestInterceptor {
		public AccessTokenInjectingInterceptor(Key key, String accessTokenValue) {
			super(key, accessTokenValue);
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

	private class ContentDigestInterceptor implements ClientHttpRequestInterceptor {
		private final Logger log = LoggerFactory.getLogger(this.getClass());

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			if (body != null && body.length > 0) {

				MessageDigest sha = new SHA512.Digest();

				byte[] digest = sha.digest(body);

				ByteSequenceItem seq = ByteSequenceItem.valueOf(digest);

				Dictionary dict = Dictionary.valueOf(Map.of(
					"sha-512", seq
					));

				request.getHeaders().add("Content-Digest", dict.serialize());

			}

			return execution.execute(request, body);
		}
	}

	// this handles both the detached and attached versions
	private class JwsSigningInterceptor extends KeyAndTokenAwareInterceptor implements ClientHttpRequestInterceptor {

		public JwsSigningInterceptor(Key key, String accessTokenValue) {
			super(key, accessTokenValue);
		}

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			// are we doing attached or not?
			boolean attached = (getKey().getProof() == Proof.JWS &&
				(request.getMethod() == HttpMethod.PUT ||
				request.getMethod() == HttpMethod.PATCH ||
				request.getMethod() == HttpMethod.POST));

			JWK clientKey = getKey().getJwk();

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
			} else {
				// if the body's not empty, the payload is a hash of the body
				payload = new Payload(Hash.SHA256_encode_url(body));
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

		public HttpMessageSigningInterceptor(Key key, String accessTokenValue) {
			super(key, accessTokenValue);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			try {

				// TODO: update with new signature mechanisms, right now we only lock on RSA-PSS
				Parameters sigParameters = Parameters.valueOf(Map.of(
					"keyid", StringItem.valueOf(getKey().getJwk().getKeyID()),
					"created", IntegerItem.valueOf(Instant.now().getEpochSecond())
					));

				// collect the base string
				Map<Item<?>, String> signatureBlock = new LinkedHashMap<>();

				URI uri = request.getURI();

				signatureBlock.put(StringItem.valueOf("@method"), request.getMethod().toString());
				signatureBlock.put(StringItem.valueOf("@target-uri"), uri.toString());
				signatureBlock.put(StringItem.valueOf("@authority"), uri.getHost());
				signatureBlock.put(StringItem.valueOf("@path"), uri.getRawPath());
				signatureBlock.put(StringItem.valueOf("@query"), uri.getRawQuery());
				signatureBlock.put(StringItem.valueOf("@scheme"), uri.getScheme());

				String reqt = "";
				if (uri.getRawPath() != null) {
					reqt += uri.getRawPath();
				}
				if (uri.getRawQuery() != null) {
					reqt += "?" + uri.getRawQuery();
				}
				signatureBlock.put(StringItem.valueOf("@request-target"), reqt);

				List<String> headersToSign = Lists.newArrayList();

				if (request.getMethod() == HttpMethod.PUT ||
					request.getMethod() == HttpMethod.PATCH ||
					request.getMethod() == HttpMethod.POST) {
					headersToSign.add("Content-Length");
					headersToSign.add("Content-Type");
					headersToSign.add("Content-Digest");
				}

				if (hasAccessToken()) {
					headersToSign.add("Authorization");
				}

				headersToSign.forEach((h) -> {
					if (request.getHeaders().getFirst(h) != null) {
						signatureBlock.put(
							StringItem.valueOf(h.toLowerCase()),
							request.getHeaders().getFirst(h).strip());
					}
				});

				// strip out all the null values from the map (we couldn't derive a value)
				signatureBlock.values().removeIf(Objects::isNull);

				// TODO: ensure basic coverage here

				// copy over the items we've added to the signature block so far
				InnerList coveredContent = InnerList.valueOf(List.copyOf(signatureBlock.keySet()))
					.withParams(sigParameters);

				signatureBlock.put(
					StringItem.valueOf("@signature-params"),
					coveredContent.serialize());

				String input = signatureBlock.entrySet().stream()
					.filter(e -> e.getValue() != null)
					.map(e -> e.getKey().serialize() + ": " + e.getValue())
					.collect(Collectors.joining("\n"));

				RSAKey rsaKey = getKey().getJwk().toRSAKey();

				Signature signer = Signature.getInstance("RSASSA-PSS");
				signer.setParameter(
					new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1));

				MessageDigest sha = new SHA512.Digest();
				byte[] hash = sha.digest(input.getBytes());

				signer.initSign(rsaKey.toPrivateKey());
				signer.update(hash);
				byte[] s = signer.sign();

				String sigId = RandomStringUtils.randomAlphabetic(5).toLowerCase();

				Dictionary sigHeader = Dictionary.valueOf(Map.of(
					sigId, ByteSequenceItem.valueOf(s)));

				Dictionary sigInputHeader = Dictionary.valueOf(Map.of(
					sigId, coveredContent));

				request.getHeaders().add("Signature", sigHeader.serialize());
				request.getHeaders().add("Signature-Input", sigInputHeader.serialize());

			} catch (NoSuchAlgorithmException | InvalidKeyException | JOSEException | SignatureException | InvalidAlgorithmParameterException e) {
				throw new RuntimeException(e);
			}

			return execution.execute(request, body);
		}

	}

	private class CavageSigningInterceptor extends KeyAndTokenAwareInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		public CavageSigningInterceptor(Key key, String accessTokenValue) {
			super(key, accessTokenValue);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			try {

				// TODO: update with new signature mechanisms, right now we only lock on RSA-256
				String alg = "rsa-sha256";

				// collect the base string
				Map<String, String> signatureBlock = new LinkedHashMap<>();
				String requestTarget = request.getMethodValue().toLowerCase() + " " + request.getURI().getRawPath();
				signatureBlock.put("(request-target)", requestTarget);

				List<String> headersToSign = Lists.newArrayList("Host", "Date");

				if (request.getMethod() == HttpMethod.PUT ||
					request.getMethod() == HttpMethod.PATCH ||
					request.getMethod() == HttpMethod.POST) {
					headersToSign.add("Content-Length");
					headersToSign.add("Digest");
				}

				if (hasAccessToken()) {
					headersToSign.add("Authorization");
				}

				headersToSign.forEach((h) -> {
					if (request.getHeaders().getFirst(h) != null) {
						signatureBlock.put(h.toLowerCase(), request.getHeaders().getFirst(h));
					}
				});

				String input = signatureBlock.entrySet().stream()
					.map(e -> e.getKey().strip().toLowerCase() + ": " + e.getValue().strip())
					.collect(Collectors.joining("\n"));

				RSAKey rsaKey = getKey().getJwk().toRSAKey();

				Signature signature = Signature.getInstance("SHA256withRSA");

				signature.initSign(rsaKey.toPrivateKey());
				signature.update(input.getBytes("UTF-8"));
				byte[] s = signature.sign();

				String encoded = Base64.getEncoder().encodeToString(s);

				String headers = signatureBlock.keySet().stream()
					.map(String::toLowerCase)
					.collect(Collectors.joining(" "));

				Map<String, String> signatureHeader = new LinkedHashMap<>();

				signatureHeader.put("keyId", getKey().getJwk().getKeyID());
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

	private class DpopInterceptor extends KeyAndTokenAwareInterceptor implements ClientHttpRequestInterceptor {

		private final Logger log = LoggerFactory.getLogger(this.getClass());

		public DpopInterceptor(Key key, String accessTokenValue) {
			super(key, accessTokenValue);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			JWK clientKey = getKey().getJwk();

			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(clientKey.getAlgorithm().getName()))
				.type(new JOSEObjectType("dpop+jwt"))
				.jwk(clientKey.toPublicJWK())
				.build();

			JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.jwtID(RandomStringUtils.randomAlphanumeric(20))
				.issueTime(Date.from(Instant.now()))
				.claim("htm", request.getMethodValue())
				.claim("htu", request.getURI().toString())
				.build();

			try {
				JWSSigner signer = new DefaultJWSSignerFactory().createJWSSigner(clientKey);

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

	private class OAuthPoPSigningInterceptor extends KeyAndTokenAwareInterceptor implements ClientHttpRequestInterceptor {

		public OAuthPoPSigningInterceptor(Key key, String accessTokenValue) {
			super(key, accessTokenValue);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			JWK clientKey = getKey().getJwk();

			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(clientKey.getAlgorithm().getName()))
				.jwk(clientKey.toPublicJWK())
				.build();

			Builder claimsSetBuilder = new JWTClaimsSet.Builder();

			String bodyHash = Hash.SHA256_encode(new String(body));
			claimsSetBuilder.claim("b", bodyHash);

			calculateQueryHash(request, claimsSetBuilder);
			calculateHeaderHash(request, claimsSetBuilder);

			claimsSetBuilder.claim("m", request.getMethod().toString().toUpperCase());
			claimsSetBuilder.claim("u", request.getURI().getHost());
			claimsSetBuilder.claim("p", request.getURI().getPath());
			claimsSetBuilder.claim("ts", Instant.now().getEpochSecond());

			if (hasAccessToken()) {
				claimsSetBuilder.claim("at", getAccessTokenValue());
			}

			try {
				JWSSigner signer = new DefaultJWSSignerFactory().createJWSSigner(clientKey);

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

				String hash = Hash.SHA256_encode(Joiner.on("&").join(hashBase));

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

				String hash = Hash.SHA256_encode(Joiner.on("\n").join(hashBase));

				claimsSetBuilder.claim("h", List.of(
					names,
					hash));
			}
		}
	}

}
