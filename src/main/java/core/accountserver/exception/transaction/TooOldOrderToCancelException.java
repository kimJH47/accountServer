package core.accountserver.exception.transaction;

public class TooOldOrderToCancelException extends RuntimeException {
	public TooOldOrderToCancelException(String message) {
		super(message);
	}
}
