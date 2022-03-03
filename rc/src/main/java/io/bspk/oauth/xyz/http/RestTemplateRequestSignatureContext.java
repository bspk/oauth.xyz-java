package io.bspk.oauth.xyz.http;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author jricher
 *
 */
public class RestTemplateRequestSignatureContext implements SignatureContext {

	private HttpRequest request;
	private Map<String, Integer> counters;

	public RestTemplateRequestSignatureContext(HttpRequest request) {
		this.request = request;
	}

	@Override
	public String getMethod() {
		return request.getMethod().toString();
	}

	@Override
	public String getAuthority() {
		return request.getURI().getHost();
	}

	@Override
	public String getScheme() {
		return request.getURI().getScheme();
	}

	@Override
	public String getTargetUri() {
		return request.getURI().toString();
	}

	@Override
	public String getRequestTarget() {
		String reqt = "";
		if (request.getURI().getRawPath() != null) {
			reqt += request.getURI().getRawPath();
		}
		if (request.getURI().getRawQuery() != null) {
			reqt += "?" + request.getURI().getRawQuery();
		}
		return reqt;
	}

	@Override
	public String getPath() {
		return request.getURI().getRawPath();
	}

	@Override
	public String getQuery() {
		return request.getURI().getRawQuery();
	}

	@Override
	public String getQueryParams(String name) {
		// parse as URL query parameters
		MultiValueMap<String,String> queryParams = UriComponentsBuilder.fromHttpRequest(request).build().getQueryParams();

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
		List<String> headers = request.getHeaders().get(name);
		return SignatureContext.combineFieldValues(headers);
	}

}
