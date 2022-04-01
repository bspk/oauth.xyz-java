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

	// this preserves insertion order
	private Map<String, Object> parameters = new LinkedHashMap<>();

	/**
	 * @return the alg
	 */
	public HttpSigAlgorithm getAlg() {
		return (HttpSigAlgorithm) parameters.get("alg");
	}

	/**
	 * @param alg the alg to set
	 */
	public SignatureParameters setAlg(HttpSigAlgorithm alg) {
		parameters.put("alg", alg);
		return this;
	}

	/**
	 * @return the created
	 */
	public Instant getCreated() {
		return (Instant) parameters.get("created");
	}

	/**
	 * @param created the created to set
	 */
	public SignatureParameters setCreated(Instant created) {
		parameters.put("created", created);
		return this;
	}

	/**
	 * @return the expires
	 */
	public Instant getExpires() {
		return (Instant) parameters.get("expires");
	}

	/**
	 * @param expires the expires to set
	 */
	public SignatureParameters setExpires(Instant expires) {
		parameters.put("expires", expires);
		return this;
	}

	/**
	 * @return the keyid
	 */
	public String getKeyid() {
		return (String) parameters.get("keyid");
	}

	/**
	 * @param keyid the keyid to set
	 */
	public SignatureParameters setKeyid(String keyid) {
		parameters.put("keyid", keyid);
		return this;
	}

	/**
	 * @return the nonce
	 */
	public String getNonce() {
		return (String) parameters.get("nonce");
	}

	/**
	 * @param nonce the nonce to set
	 */
	public SignatureParameters setNonce(String nonce) {
		parameters.put("nonce", nonce);
		return this;
	}

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

		// preserve order
		for (String paramName : parameters.keySet()) {
			if (paramName.equals("alg")) {
				HttpSigAlgorithm alg = getAlg();
				if (alg.getExplicitAlg() != null) {
					params.put("alg", alg.getExplicitAlg());
				}
			} else if (paramName.equals("created")) {
				params.put("created", getCreated().getEpochSecond());
			} else if (paramName.equals("expires")) {
				params.put("expires", getExpires().getEpochSecond());
			} else if (paramName.equals("keyid")) {
				params.put("keyid", getKeyid());
			} else if (paramName.equals("nonce")) {
				params.put("nonce", getNonce());
			} else {
				params.put(paramName, parameters.get(paramName));
			}
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

				for (String key : coveredComponents.getParams().keySet()) {
					if (key.equals("alg")) {
						params.setAlg(HttpSigAlgorithm.of(((StringItem)coveredComponents.getParams().get("alg")).get()));
					} else if (key.equals("created")) {
						params.setCreated(
							Instant.ofEpochSecond(((NumberItem<?>)coveredComponents.getParams().get("created")).getAsLong()));
					} else if (key.equals("expires")) {
						params.setCreated(
							Instant.ofEpochSecond(((NumberItem<?>)coveredComponents.getParams().get("expires")).getAsLong()));
					} else  if (key.equals("keyid")) {
						params.setKeyid(((StringItem)coveredComponents.getParams().get("keyid")).get());
					} else if (key.equals("nonce")) {
						params.setNonce(((StringItem)coveredComponents.getParams().get("nonce")).get());
					} else {
						params.getParameters().put(key, coveredComponents.getParams().get(key).serialize()); // store the serialized version
					}
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
