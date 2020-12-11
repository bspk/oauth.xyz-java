package io.bspk.oauth.xyz;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

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
	public MongoCustomConversions mongoCustomConversions() {
	    List<Converter<?, ?>> list = List.of(
	    	new JWKDeserializer(),
	    	new JWKSerializer(),
	    	new JWTDeserializer(),
	    	new JWTSerializer());
	    return new MongoCustomConversions(list);
	}
}
