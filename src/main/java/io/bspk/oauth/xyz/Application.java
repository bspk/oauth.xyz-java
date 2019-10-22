package io.bspk.oauth.xyz;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

import io.bspk.oauth.xyz.crypto.Hash;
import io.bspk.oauth.xyz.data.api.HandleReplaceable;
import io.bspk.oauth.xyz.json.HandleAwareDeserializer;
import io.bspk.oauth.xyz.json.HandleAwareSerializer;
import io.bspk.oauth.xyz.json.JWKDeserializer;
import io.bspk.oauth.xyz.json.JWKSerializer;

@SpringBootApplication()
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public Module jacksonModule() {
		SimpleModule module = new SimpleModule();
		module.setDeserializerModifier(new BeanDeserializerModifier() {

			@Override
			public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
				if (HandleReplaceable.class.isAssignableFrom(beanDesc.getBeanClass())) {
					return new HandleAwareDeserializer(deserializer);
				} else {
					return deserializer;
				}

			}
		});

		module.setSerializerModifier(new BeanSerializerModifier() {

			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
				if (HandleReplaceable.class.isAssignableFrom(beanDesc.getBeanClass())) {
					return new HandleAwareSerializer(serializer);
				} else {
					return serializer;
				}
			}
		});

		return module;
	}

	@Bean
	public MongoCustomConversions mongoCustomConversions() {
	    List<Converter<?, ?>> list = List.of(
	    	new JWKDeserializer(),
	    	new JWKSerializer());
	    return new MongoCustomConversions(list);
	}

	@Bean
	public RestTemplate restTemplate() {
		ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());

		RestTemplate restTemplate = new RestTemplate(factory);
		restTemplate.setInterceptors(List.of(
			new DigestInterceptor(),
			new CavageSigningInterceptor(),
			new DetachedJwsSigningInterceptor(),
			new RequestResponseLoggingInterceptor()
			));

		MappingJackson2HttpMessageConverter messageConverter = restTemplate.getMessageConverters().stream()
			.filter(MappingJackson2HttpMessageConverter.class::isInstance)
			.map(MappingJackson2HttpMessageConverter.class::cast)
			.findFirst().orElseThrow(() -> new RuntimeException("MappingJackson2HttpMessageConverter not found"));

		messageConverter.getObjectMapper().registerModule(jacksonModule());

		return restTemplate;
	}

	@Bean
	public JWK clientKey() {
		try {
			return JWK.parse(
				"{\n" +
				"  \"kty\": \"RSA\",\n" +
				"  \"d\": \"m1M7uj1uZMgQqd2qwqBk07rgFzbzdCAbsfu5kvqoALv3oRdyi_UVHXDhos3DZVQ3M6mKgb30XXESykY8tpWcQOU-qx6MwtSFbo-3SNx9fBtylyQosHECGyleVP79YTE4mC0odRoUIDS90J9AcFsdVtC6M2oJ3CCL577a-lJg6eYyQoRmbjdzqMnBFJ99TCfR6wBQQbzXi1K_sN6gcqhxMmQXHWlqfT7-AJIxX9QUF0rrXMMX9fPh-HboGKs2Dqoo3ofJ2XuePpmpVDvtGy_jenXmUdpsRleqnMrEI2qkBonJQSKL4HPNpsylbQyXt2UtYrzcopCp7jL-j56kRPpQAQ\",\n" +
				"  \"e\": \"AQAB\",\n" +
				"  \"kid\": \"xyz-client\",\n" +
				"  \"alg\": \"RS256\",\n" +
				"  \"n\": \"zwCT_3bx-glbbHrheYpYpRWiY9I-nEaMRpZnRrIjCs6b_emyTkBkDDEjSysi38OC73hj1-WgxcPdKNGZyIoH3QZen1MKyyhQpLJG1-oLNLqm7pXXtdYzSdC9O3-oiyy8ykO4YUyNZrRRfPcihdQCbO_OC8Qugmg9rgNDOSqppdaNeas1ov9PxYvxqrz1-8Ha7gkD00YECXHaB05uMaUadHq-O_WIvYXicg6I5j6S44VNU65VBwu-AlynTxQdMAWP3bYxVVy6p3-7eTJokvjYTFqgDVDZ8lUXbr5yCTnRhnhJgvf3VjD_malNe8-tOqK5OSDlHTy6gD9NqdGCm-Pm3Q\"\n" +
				"}"
				);
		} catch (ParseException e) {
			return null;
		}
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
				log.info(IntStream.range(0, body.length)
					.map(idx -> Byte.toUnsignedInt(body[idx]))
					.mapToObj(i -> Integer.toHexString(i))
					.collect(Collectors.joining(", ")));

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
			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(clientKey().getAlgorithm().getName()))
				.customParam("b64", true)
				.criticalParams(Set.of("b64"))
				.type(JOSEObjectType.JOSE)
				.keyID(clientKey().getKeyID())
				.build();

			Payload payload = new Payload(body);

			//log.info(">> " + payload.toBase64URL().toString());

			try {
				RSASSASigner signer = new RSASSASigner((RSAKey)clientKey());

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

				JWK clientKey = clientKey();
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

		        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Signature " + signatureHeaderPayload);

			} catch (NoSuchAlgorithmException | InvalidKeyException | JOSEException | SignatureException e) {
				throw new RuntimeException(e);
			}

			return execution.execute(request, body);
		}

	}
}
