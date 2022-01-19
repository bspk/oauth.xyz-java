package io.bspk.oauth.xyz.authserver.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * Exists solely to expose discovery values to the front end.
 *
 * @author jricher
 *
 */
@Controller
@CrossOrigin
@RequestMapping("/api/whoami")
public class DiscoveryController {

	@Value("${oauth.xyz.root}")
	private String baseUrl;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getRoot() {

		Map<String, String> res = Map.of("rootUrl", baseUrl);

		return ResponseEntity.ok(res);

	}


}
