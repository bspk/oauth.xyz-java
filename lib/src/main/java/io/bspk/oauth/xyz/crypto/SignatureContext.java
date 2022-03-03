package io.bspk.oauth.xyz.crypto;

import java.util.List;
import java.util.stream.Collectors;

import org.greenbytes.http.sfv.Dictionary;
import org.greenbytes.http.sfv.Item;
import org.greenbytes.http.sfv.ListElement;
import org.greenbytes.http.sfv.ParseException;
import org.greenbytes.http.sfv.Parser;
import org.greenbytes.http.sfv.StringItem;
import org.greenbytes.http.sfv.Type;

/**
 * @author jricher
 *
 */
public interface SignatureContext {

	// derived, for requests
	String getMethod();
	String getAuthority();
	String getScheme();
	String getTargetUri();
	String getRequestTarget();
	String getPath();
	String getQuery();
	String getQueryParams(String name);

	// derived, for responses
	String getStatus();

	// fields
	String getField(String name);

	static String combineFieldValues(List<String> fields) {
		if (fields == null) {
			return null;
		} else {
			return fields.stream()
				.map(String::strip)
				.collect(Collectors.joining(", "));
		}
	}

	default String getComponentValue(StringItem componentIdentifier) {
		String baseIdentifier = componentIdentifier.get();
		if (baseIdentifier.startsWith("@")) {
			// derived component
			switch (baseIdentifier) {
				case "@method":
					return getMethod();
				case "@authority":
					return getAuthority();
				case "@scheme":
					return getScheme();
				case "@target-uri":
					return getTargetUri();
				case "@request-target":
					return getRequestTarget();
				case "@path":
					return getPath();
				case "@query":
					return getQuery();
				case "@status":
					return getStatus();
				case "@query-params":
				{
					if (componentIdentifier.getParams().containsKey("name")) {
						Item<? extends Object> nameParameter = componentIdentifier.getParams().get("name");
						if (nameParameter instanceof StringItem) {
							String name = ((StringItem)nameParameter).get();
							return getQueryParams(name);
						} else {
							throw new IllegalArgumentException("Invalid Syntax: Value for 'name' parameter of " + baseIdentifier + " must be a StringItem");
						}
					} else {
						throw new IllegalArgumentException("'name' parameter of " + baseIdentifier + " is required");
					}
				}
				default:
					throw new IllegalArgumentException("Unknown derived component: " + baseIdentifier);
			}
		} else {
			if (componentIdentifier.getParams().containsKey("key")) {
				Item<? extends Object> keyParameter = componentIdentifier.getParams().get("key");
				if (keyParameter instanceof StringItem) {
					try {
						String fieldValue = getField(baseIdentifier);
						Dictionary dictionary = Parser.parseDictionary(fieldValue);
						String key = ((StringItem)keyParameter).get();
						if (dictionary.get().containsKey(key)) {

							ListElement<? extends Object> dictionaryValue = dictionary.get().get(key);

							// we always re-serialize the value
							return dictionaryValue.serialize();
						} else {
							throw new IllegalArgumentException("Value for '" + key + "' key of dictionary " + baseIdentifier + " does not exist");
						}
					} catch (ParseException e) {
						throw new IllegalArgumentException("Field " + baseIdentifier + " is not a dictionary field");
					}
				} else {
					throw new IllegalArgumentException("Invalid Syntax: Value for 'key' parameter of field " + baseIdentifier + " must be a StringItem");
				}
			} else if (componentIdentifier.getParams().containsKey("sf")) {
				switch (baseIdentifier) {
				    case "accept":
				    case "accept-encoding":
				    case "accept-language":
				    case "accept-patch":
				    case "accept-ranges":
				    case "access-control-allow-headers":
				    case "access-control-allow-methods":
				    case "access-control-expose-headers":
				    case "access-control-request-headers":
				    case "allow":
				    case "alpn":
				    case "connection":
				    case "content-encoding":
				    case "content-language":
				    case "content-length":
				    case "te":
				    case "timing-allow-origin":
				    case "trailer":
				    case "transfer-encoding":
				    case "vary":
				    case "x-xss-protection":
				    case "cache-status":
				    case "proxy-status":
				    case "variant-key":
				    case "x-list":
				    case "x-list-a":
				    case "x-list-b":
				    case "accept-ch":
				    case "example-list":
				    {
				    	// List
				    	try {
					    	String fieldValue = getField(baseIdentifier);
					    	Type<?> sf = Parser.parseList(fieldValue);
					    	return sf.serialize();
						} catch (ParseException e) {
							throw new IllegalArgumentException("Field " + baseIdentifier + " is not a structured field");
						}
				    }
				    case "alt-svc":
				    case "cache-control":
				    case "expect-ct":
				    case "keep-alive":
				    case "pragma":
				    case "prefer":
				    case "preference-applied":
				    case "surrogate-control":
				    case "variants":
				    case "signature":
				    case "signature-input":
				    case "priority":
				    case "x-dictionary":
				    case "example-dict":
				    case "cdn-cache-control":
				    {
				    	// Dictionary
				    	try {
					    	String fieldValue = getField(baseIdentifier);
					    	Type<?> sf = Parser.parseDictionary(fieldValue);
					    	return sf.serialize();
						} catch (ParseException e) {
							throw new IllegalArgumentException("Field " + baseIdentifier + " is not a structured field");
						}
				    }
				    case "access-control-max-age":
				    case "access-control-allow-credentials":
				    case "access-control-allow-origin":
				    case "access-control-request-method":
				    case "age":
				    case "alt-used":
				    case "content-type":
				    case "cross-origin-resource-policy":
				    case "expect":
				    case "host":
				    case "origin":
				    case "retry-after":
				    case "x-content-type-options":
				    case "x-frame-options":
				    case "example-integer":
				    case "example-decimal":
				    case "example-string":
				    case "example-token":
				    case "example-bytesequence":
				    case "example-boolean":
				    {
				    	// Item
				    	try {
					    	String fieldValue = getField(baseIdentifier);
					    	Type<?> sf = Parser.parseItem(fieldValue);
					    	return sf.serialize();
						} catch (ParseException e) {
							throw new IllegalArgumentException("Field " + baseIdentifier + " is not a structured field");
						}
				    }
				    default:
				    	throw new IllegalArgumentException("Field " + baseIdentifier + " is not a structured field");

				}
			} else {
				return getField(baseIdentifier);
			}
		}


	}
}
