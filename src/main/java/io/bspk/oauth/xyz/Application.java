package io.bspk.oauth.xyz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.bspk.oauth.xyz.authserver")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

