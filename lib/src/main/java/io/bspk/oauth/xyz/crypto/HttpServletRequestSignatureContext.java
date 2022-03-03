package io.bspk.oauth.xyz.crypto;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author jricher
 *
 */
public class HttpServletRequestSignatureContext implements SignatureContext {

	private HttpServletRequest request;
	private URI uri;
	private Map<String, Integer> counters;

	public HttpServletRequestSignatureContext(HttpServletRequest request) {
		this.request = request;
		this.uri = URI.create(request.getRequestURL().toString()
			+ (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
	}

	@Override
	public String getMethod() {
		return request.getMethod();
	}

	@Override
	public String getAuthority() {
		return uri.getHost();
	}

	@Override
	public String getScheme() {
		return uri.getScheme();
	}

	@Override
	public String getTargetUri() {
		return uri.toString();
	}

	@Override
	public String getRequestTarget() {
		String reqt = "";
		if (uri.getRawPath() != null) {
			reqt += uri.getRawPath();
		}
		if (uri.getRawQuery() != null) {
			reqt += "?" + uri.getRawQuery();
		}
		return reqt;
	}

	@Override
	public String getPath() {
		return uri.getRawPath();
	}

	@Override
	public String getQuery() {
		return uri.getRawQuery();
	}

	@Override
	public String getQueryParams(String name) {
		MultiValueMap<String,String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
		List<String> values = queryParams.get(name);

		if (values.size() == 1) {
			// no problem, return one
			return values.get(0);
		} else if (values.size() == 0) {
			throw new IllegalArgumentException("Could not find query parameter named " + name);
		} else {
			if (counters == null) {
				// we have to count the accesses, initialize all to zero
				counters = queryParams.keySet().stream()
					.collect(Collectors.toMap(Function.identity(), (k) -> 0));
			}
			Integer count = counters.get(name); // get the current count (ie: 1)
			counters.put(name, count + 1); // increment the count (ie: 2)
			return values.get(count); // return the value at the position before the update (ie: 1)
		}
	}

	@Override
	public String getStatus() {
		throw new UnsupportedOperationException("Requests cannot return a status code");
	}

	@Override
	public String getField(String name) {
		return SignatureContext.combineFieldValues(Collections.list(request.getHeaders(name)));
	}

}
