package io.bspk.oauth.xyz;

import java.text.ParseException;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.nimbusds.jose.jwk.JWK;

import io.bspk.oauth.xyz.data.BoundKey;
import io.bspk.oauth.xyz.data.api.HandleReplaceable;
import io.bspk.oauth.xyz.json.BoundKeyDeserializer;
import io.bspk.oauth.xyz.json.HandleAwareDeserializer;
import io.bspk.oauth.xyz.json.HandleAwareSerializer;
import io.bspk.oauth.xyz.json.JWKDeserializer;
import io.bspk.oauth.xyz.json.JWKSerializer;
import io.bspk.oauth.xyz.json.JWTDeserializer;
import io.bspk.oauth.xyz.json.JWTSerializer;

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
				} else if (BoundKey.class.isAssignableFrom(beanDesc.getBeanClass())) {
					return new BoundKeyDeserializer(deserializer);
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

	@Bean
	public MongoCustomConversions mongoCustomConversions() {
	    List<Converter<?, ?>> list = List.of(
	    	new JWKDeserializer(),
	    	new JWKSerializer(),
	    	new JWTDeserializer(),
	    	new JWTSerializer());
	    return new MongoCustomConversions(list);
	}

	@Bean
	public WebMvcConfigurer webMvcConfigurer() {
		WebMvcConfigurer configurer = new WebMvcConfigurer() {
			@Override
			public void addViewControllers(ViewControllerRegistry registry) {

				// map all of the front-end pages to React

				registry.addViewController("/device").setViewName("/");
				registry.addViewController("/interact").setViewName("/");
				registry.addViewController("/as").setViewName("/");
			}
		};

		return configurer;
	}

	@Bean
	public RestTemplate restTemplate(List<ClientHttpRequestInterceptor> interceptors) {
		ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory());

		RestTemplate restTemplate = new RestTemplate(factory);
		restTemplate.setInterceptors(interceptors);

		// set up Jackson
		MappingJackson2HttpMessageConverter messageConverter = restTemplate.getMessageConverters().stream()
			.filter(MappingJackson2HttpMessageConverter.class::isInstance)
			.map(MappingJackson2HttpMessageConverter.class::cast)
			.findFirst().orElseThrow(() -> new RuntimeException("MappingJackson2HttpMessageConverter not found"));

		messageConverter.getObjectMapper().registerModule(jacksonModule());
		messageConverter.getObjectMapper().setSerializationInclusion(Include.NON_NULL);

		return restTemplate;
	}

	// for logging incoming requests in full, for debugging
	/*
	@Bean
	public CommonsRequestLoggingFilter logFilter() {
		CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
		filter.setIncludeQueryString(true);
		filter.setIncludePayload(true);
		filter.setMaxPayloadLength(10000);
		filter.setIncludeHeaders(true);
		filter.setAfterMessagePrefix("REQUEST DATA : ");
		return filter;
	}
	 */
}
