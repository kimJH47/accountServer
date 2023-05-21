package core.accountserver.dto.response;

import java.util.HashMap;

public class FailedResponse extends Response {
	private HashMap<String, String> reasons = new HashMap<>();
	protected FailedResponse(String message) {
		super(message);
	}

	public void input(String fieldName, String message) {
		reasons.put(fieldName, message);
	}
}
