package io.bspk.oauth.xyz;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
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

import io.bspk.oauth.xyz.data.api.HandleReplaceable;
import io.bspk.oauth.xyz.json.HandleAwareDeserializer;
import io.bspk.oauth.xyz.json.HandleAwareSerializer;

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
	public RestTemplate restTemplate() {
		ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());

		RestTemplate restTemplate = new RestTemplate(factory);
		restTemplate.setInterceptors(List.of(new RequestResponseLoggingInterceptor()));

		MappingJackson2HttpMessageConverter messageConverter = restTemplate.getMessageConverters().stream()
            .filter(MappingJackson2HttpMessageConverter.class::isInstance)
            .map(MappingJackson2HttpMessageConverter.class::cast)
            .findFirst().orElseThrow( () -> new RuntimeException("MappingJackson2HttpMessageConverter not found"));

		messageConverter.getObjectMapper().registerModule(jacksonModule());

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
	            log.info("===========================request begin================================================");
	            log.info("URI         : {}", request.getURI());
	            log.info("Method      : {}", request.getMethod());
	            log.info("Headers     : {}", request.getHeaders());
	            log.info("Request body: {}", new String(body, "UTF-8"));
	            log.info("==========================request end================================================");
	    }

	    private void logResponse(ClientHttpResponse response) throws IOException {
	            log.info("============================response begin==========================================");
	            log.info("Status code  : {}", response.getStatusCode());
	            log.info("Status text  : {}", response.getStatusText());
	            log.info("Headers      : {}", response.getHeaders());
	            log.info("Response body: {}", StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
	            log.info("=======================response end=================================================");
	    }
	}

}

