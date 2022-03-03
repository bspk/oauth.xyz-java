package io.bspk.oauth.xyz.crypto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.greenbytes.http.sfv.Dictionary;
import org.greenbytes.http.sfv.InnerList;
import org.greenbytes.http.sfv.Item;
import org.greenbytes.http.sfv.ListElement;
import org.greenbytes.http.sfv.NumberItem;
import org.greenbytes.http.sfv.Parameters;
import org.greenbytes.http.sfv.StringItem;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Carrier class for signature parameters.
 *
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
public class SignatureParameters {

	private List<StringItem> componentIdentifiers = new ArrayList<>();
	private HttpSigAlgorithm alg;
	private Instant created;
	private Instant expires;
	private String keyid;
	private String nonce;

	public StringItem toComponentIdentifier() {
		return StringItem.valueOf("@signature-params");
	}

	public InnerList toComponentValue() {

		// we do this cast to get around Java's generic type restrictions
		List<Item<? extends Object>> identifiers = componentIdentifiers.stream()
			.map(e -> (Item<? extends Object>) e)
			.collect(Collectors.toList());

		InnerList list = InnerList.valueOf(identifiers);

		Map<String, Object> params = new LinkedHashMap<>();

		if (alg != null && alg.getExplicitAlg() != null) {
			params.put("alg", alg.getExplicitAlg());
		}

		if (created != null) {
			params.put("created", created.getEpochSecond());
		}

		if (expires != null) {
			params.put("expires", expires.getEpochSecond());
		}

		if (keyid != null) {
			params.put("keyid", keyid);
		}

		if (nonce != null) {
			params.put("nonce", nonce);
		}

		list = list.withParams(Parameters.valueOf(params));

		return list;
	}

	public SignatureParameters addComponentIdentifier(String identifier) {
		if (!identifier.startsWith("@")) {
			componentIdentifiers.add(StringItem.valueOf(identifier.toLowerCase()));
		} else {
			componentIdentifiers.add(StringItem.valueOf(identifier));
		}
		return this;
	}

	public SignatureParameters addComponentIdentifier(StringItem identifier) {
		componentIdentifiers.add(identifier);
		return this;
	}

	// this ignores parameters
	public boolean containsComponentIdentifier(String identifier) {
		return componentIdentifiers.stream()
			.map(StringItem::get)
			.anyMatch(identifier::equals);
	}

	// does not ignore parameters
	public boolean containsComponentIdentifier(StringItem identifier) {
		return componentIdentifiers.contains(identifier);
	}

	/**
	 * @param signatureInput
	 * @param sigId
	 */
	public static SignatureParameters fromDictionaryEntry(Dictionary signatureInput, String sigId) {
		if (signatureInput.get().containsKey(sigId)) {
			ListElement<? extends Object> item = signatureInput.get().get(sigId);
			if (item instanceof InnerList) {
				InnerList coveredComponents = (InnerList)item;

				SignatureParameters params = new SignatureParameters()
					.setComponentIdentifiers(
						coveredComponents.get().stream()
							.map(StringItem.class::cast)
							.collect(Collectors.toList()));

				if (coveredComponents.getParams().containsKey("alg")) {
					params.setAlg(HttpSigAlgorithm.of(((StringItem)coveredComponents.getParams().get("alg")).get()));
				}

				if (coveredComponents.getParams().containsKey("created")) {
					params.setCreated(
						Instant.ofEpochSecond(((NumberItem<?>)coveredComponents.getParams().get("created")).getAsLong()));
				}

				if (coveredComponents.getParams().containsKey("expires")) {
					params.setCreated(
						Instant.ofEpochSecond(((NumberItem<?>)coveredComponents.getParams().get("expires")).getAsLong()));
				}

				if (coveredComponents.getParams().containsKey("keyid")) {
					params.setKeyid(((StringItem)coveredComponents.getParams().get("keyid")).get());
				}

				if (coveredComponents.getParams().containsKey("nonce")) {
					params.setNonce(((StringItem)coveredComponents.getParams().get("nonce")).get());
				}

				return params;

			} else {
				throw new IllegalArgumentException("Invalid syntax, identifier '" + sigId + "' must be an inner list");
			}
		} else {
			throw new IllegalArgumentException("Could not find identifier '" + sigId + "' in dictionary " + signatureInput.serialize());
		}

	}
}
