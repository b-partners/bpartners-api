package app.bpartners.api.model.exception;

public class ForbiddenException extends ApiException {

  public ForbiddenException() {
    super(ExceptionType.CLIENT_EXCEPTION, "Access Denied");
  }

  public ForbiddenException(String message) {
    super(ExceptionType.CLIENT_EXCEPTION, message);
  }
}
