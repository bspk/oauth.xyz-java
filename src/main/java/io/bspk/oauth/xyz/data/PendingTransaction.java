package io.bspk.oauth.xyz.data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.bspk.oauth.xyz.data.api.TransactionRequest;
import io.bspk.oauth.xyz.data.api.TransactionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author jricher
 *
 */
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PendingTransaction {

	@Data
	@Accessors(chain = true)
	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
	@AllArgsConstructor
	public class Entry {
		private TransactionRequest request;
		private TransactionResponse response;
	}

	private List<Entry> entries = new ArrayList<>();

	public PendingTransaction add (TransactionRequest request, TransactionResponse response) {
		entries.add(new Entry(request, response));
		return this;
	}

}
