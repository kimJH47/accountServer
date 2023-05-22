package core.accountserver.dto.response;

import java.util.HashMap;

public class FailedResponse extends Response {
	private final HashMap<String, String> reasons = new HashMap<>();
	protected FailedResponse(String message) {
		super(message);
	}

	public void input(String fieldName, String message) {
		reasons.put(fieldName, message);
		System.out.println(reasons);
	}

	public HashMap<String, String> getReasons() {
		return reasons;
	}
}
