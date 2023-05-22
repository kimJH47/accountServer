package core.accountserver.exception.transaction;

public class CancelMustFullyException extends RuntimeException {
	public CancelMustFullyException(String message) {
		super(message);
	}
}
