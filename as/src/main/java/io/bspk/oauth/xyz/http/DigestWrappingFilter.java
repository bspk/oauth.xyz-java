package io.bspk.oauth.xyz.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

import lombok.experimental.Delegate;

/**
 * @author jricher
 *
 */
@Component
public class DigestWrappingFilter implements Filter {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String BODY_BYTES = "BODY_BYTES";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		CachingRequestWrapper requestWrapper = new CachingRequestWrapper((HttpServletRequest) request);

		calculateDigest(requestWrapper);

		chain.doFilter(requestWrapper, response);

	}

	private void calculateDigest(CachingRequestWrapper requestWrapper) {
		byte[] bytes = requestWrapper.getSavedBody();

		/*
		log.info(IntStream.range(0, bytes.length)
			.map(idx -> Byte.toUnsignedInt(bytes[idx]))
			.mapToObj(i -> Integer.toHexString(i))
			.collect(Collectors.joining(", ")));
		 */

		requestWrapper.setAttribute(BODY_BYTES, bytes);

	}

	private class CachingRequestWrapper implements HttpServletRequest {
		@Delegate(types = HttpServletRequest.class, excludes = ExcludeReaderAndInputStream.class)
		private HttpServletRequest delegate;

		private ServletInputStream savedInputStream;
		private BufferedReader savedReader;

		private byte[] savedBody;

		public CachingRequestWrapper(HttpServletRequest delegate) throws IOException {

			// cache the body as a byte array
			this.delegate = delegate;

			this.savedBody = delegate.getInputStream().readAllBytes();

			ByteArrayInputStream sourceStream = new ByteArrayInputStream(getSavedBody());

			this.savedInputStream = new DelegatingServletInputStream(sourceStream);

			this.savedReader = new BufferedReader(new InputStreamReader(sourceStream));

		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return savedInputStream;
		}

		@Override
		public BufferedReader getReader() throws IOException {
			return savedReader;
		}

		public byte[] getSavedBody() {
			return savedBody;
		}

		@Override
	    public <T extends HttpUpgradeHandler> T upgrade(
            Class<T> httpUpgradeHandlerClass) throws java.io.IOException, ServletException {
	    	return delegate.upgrade(httpUpgradeHandlerClass);
	    }

	}

	// marker interface for lombok tagging
	private interface ExcludeReaderAndInputStream {
		public ServletInputStream getInputStream();
		public BufferedReader getReader();
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
