package core.accountserver.dto.response;


public class SuccessResponse<T> extends Response {
	T entity;

	public SuccessResponse(String message, T entity) {
		super(message);
		this.entity = entity;
	}

	public T getEntity() {
		return entity;
	}
}
