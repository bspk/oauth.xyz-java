package io.bspk.oauth.xyz.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpUpgradeHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.nimbusds.jose.JOSEObject;

import lombok.experimental.Delegate;

/**
 * @author jricher
 *
 */
@Component
public class JoseUnwrappingFilter implements Filter {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String BODY_JOSE = "BODY_JOSE";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {


		HttpServletRequest req = (HttpServletRequest) request;
		if (req.getContentType() != null && req.getContentType().equals("application/jose")) {
			CachingRequestWrapper requestWrapper = new CachingRequestWrapper(req);

			processJose(requestWrapper);

			chain.doFilter(requestWrapper, response);
		} else {
			// it's not a JOSE payload, ignore it
			chain.doFilter(request, response);
		}

	}

	private void processJose(CachingRequestWrapper requestWrapper) {
		JOSEObject jose = requestWrapper.getJose();

		/*
		log.info(IntStream.range(0, bytes.length)
			.map(idx -> Byte.toUnsignedInt(bytes[idx]))
			.mapToObj(i -> Integer.toHexString(i))
			.collect(Collectors.joining(", ")));
		 */

		// save the original JOSE item as an inbound attribute
		requestWrapper.setAttribute(BODY_JOSE, jose);

	}

	private class CachingRequestWrapper implements HttpServletRequest {
		@Delegate(types = HttpServletRequest.class, excludes = ExcludeMaskedMethods.class)
		private HttpServletRequest delegate;

		private ServletInputStream savedInputStream;
		private BufferedReader savedReader;

		private JOSEObject jose;

		private long length;

		public CachingRequestWrapper(HttpServletRequest delegate) throws IOException {

			try {
				// cache the body as a byte array
				this.delegate = delegate;

				byte[] savedBody = delegate.getInputStream().readAllBytes();

				this.length = savedBody.length;

				this.jose = JOSEObject.parse(new String(savedBody));

				// make the payload of the JWT available to the rest of the system
				byte[] payload = jose.getPayload().toBytes();

				ByteArrayInputStream sourceStream = new ByteArrayInputStream(payload);

				this.savedInputStream = new DelegatingServletInputStream(sourceStream);

				this.savedReader = new BufferedReader(new InputStreamReader(sourceStream));

			} catch (ParseException e) {
				throw new IOException(e);
			}
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return savedInputStream;
		}

		@Override
		public BufferedReader getReader() throws IOException {
			return savedReader;
		}

		public JOSEObject getJose() {
			return jose;
		}

		@Override
	    public <T extends HttpUpgradeHandler> T upgrade(
            Class<T> httpUpgradeHandlerClass) throws java.io.IOException, ServletException {
	    	return delegate.upgrade(httpUpgradeHandlerClass);
	    }

		// provide the unwrapped JSON
		@Override
		public String getContentType() {
			return "application/json";
		}

		@Override
		public int getContentLength() {
			return (int) this.length;
		}

		@Override
		public long getContentLengthLong() {
			return this.length;
		}

		@Override
		public String getHeader(String name) {
			if (name.equalsIgnoreCase("content-type")) {
				return getContentType();
			} else if (name.equalsIgnoreCase("content-length")) {
				return Integer.toString(getContentLength());
			} else {
				return delegate.getHeader(name);
			}
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			if (name.equalsIgnoreCase("content-type")) {
				return Collections.enumeration(List.of(getContentType()));
			} else if (name.equalsIgnoreCase("content-length")) {
				return Collections.enumeration(List.of(Integer.toString(getContentLength())));
			} else {
				return delegate.getHeaders(name);
			}
		}

	}

	// marker interface for lombok tagging
	private interface ExcludeMaskedMethods {
		public ServletInputStream getInputStream();
		public BufferedReader getReader();
		public String getContentType();
		public int getContentLength();
		public long getContentLengthLong();
		public String getHeader(String name);
		public Enumeration<String> getHeaders(String name);
		public <T extends HttpUpgradeHandler> T upgrade(
            Class<T> httpUpgradeHandlerClass) throws java.io.IOException, ServletException;
	}


	// Copied from Spring Test Tools
	private class DelegatingServletInputStream extends ServletInputStream {

		private final InputStream sourceStream;

		private boolean finished = false;


		/**
		 * Create a DelegatingServletInputStream for the given source stream.
		 * @param sourceStream the source stream (never {@code null})
		 */
		public DelegatingServletInputStream(InputStream sourceStream) {
			Assert.notNull(sourceStream, "Source InputStream must not be null");
			this.sourceStream = sourceStream;
		}

		/**
		 * Return the underlying source stream (never {@code null}).
		 */
		public final InputStream getSourceStream() {
			return this.sourceStream;
		}


		@Override
		public int read() throws IOException {
			int data = this.sourceStream.read();
			if (data == -1) {
				this.finished = true;
			}
			return data;
		}

		@Override
		public int available() throws IOException {
			return this.sourceStream.available();
		}

		@Override
		public void close() throws IOException {
			super.close();
			this.sourceStream.close();
		}

		@Override
		public boolean isFinished() {
			return this.finished;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			throw new UnsupportedOperationException();
		}

	}
}
