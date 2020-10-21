package io.bspk.oauth.xyz;

import java.text.ParseException;
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
}
