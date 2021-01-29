package io.bspk.oauth.xyz;

import java.text.ParseException;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.nimbusds.jose.jwk.JWK;

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
	public JWK clientKey2() {
		try {
			return JWK.parse(
				"{\n"
				+ "    \"kty\": \"EC\",\n"
				+ "    \"d\": \"e0ClWmBfEJVMUSCp6ewnMprJtfvDiZ8HGi3u40upAvQ\",\n"
				+ "    \"use\": \"sig\",\n"
				+ "    \"crv\": \"secp256k1\",\n"
				+ "    \"kid\": \"sig-2020-10-30T20:47:51Z\",\n"
				+ "    \"x\": \"JtH74-28f8tBlYd3SCm7RUVAkOWMj702li03oAo_GnY\",\n"
				+ "    \"y\": \"Y-R9g1bZz754iZn9etHDuCOoKz_1C4HCh2LF0lK85qk\",\n"
				+ "    \"alg\": \"ES256K\"\n"
				+ "}"
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
