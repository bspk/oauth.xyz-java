package io.bspk.oauth.xyz.crypto;

import org.greenbytes.http.sfv.StringItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
public class SignatureBaseBuilder {


	private static final Logger log = LoggerFactory.getLogger(SignatureBaseBuilder.class);


	private SignatureParameters sigParams;
	private SignatureContext ctx;

	public byte[] createSignatureBase() {
		StringBuilder base = new StringBuilder();

		for (StringItem componentIdentifier : sigParams.getComponentIdentifiers()) {

			String componentValue = ctx.getComponentValue(componentIdentifier);

			if (componentValue != null) {
				// write out the line to the base
				componentIdentifier.serializeTo(base)
					.append(": ")
					.append(componentValue)
					.append('\n');
			} else {
				// FIXME: be more graceful about bailing
				throw new RuntimeException("Couldn't find a value for required parameter: " + componentIdentifier.serialize());
			}
		}

		// add the signature parameters line
		sigParams.toComponentIdentifier().serializeTo(base)
			.append(": ");
		sigParams.toComponentValue().serializeTo(base);


		log.info("~~~ Signature Base  :\n ~~~ : {}", Joiner.on("\n~~~ : ").join(Splitter.on("\n").split(base.toString())));

		return base.toString().getBytes();
	}

}
